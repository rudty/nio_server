package org.rudtyz;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Collections;
import java.util.Set;

public class NetPoll {
    public interface OnPoll{
        /**
         * 클라이언트에서 메세지를 보낼 시 호출될 함수
         * @param channel 클라이언트
         * @param att register 에서 인자로 넣은 것
         */
        void onRead(SocketChannel channel, Object att);

        /**
         * Write 호출 시 수행할 함수
         * @param channel 클라이언트
         * @param att register 에서 인자로 넣은 것
         */
        void onWrite(SocketChannel channel, Object att);

        /**
         * 클라이언트에서 연결 시도 시 호출될 함수
         * @param channel 클라이언트
         * @param att register 에서 인자로 넣은 것
         */
        void onConnect(SocketChannel channel, Object att);

        /**
         * 클라이언트와 연결되었을때 호출될 함수
         * @param channel 클라이언트
         * @param att register 에서 인자로 넣은 것
         */
        void onAccept(SocketChannel channel, Object att);
    }

    private final Selector selector;
    private OnPoll pollEventListener = null;

    public NetPoll() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SelectionKey register(SocketChannel channel, int ops, Object att) {
        if (channel.isConnected()) {
            try {
                return channel.register(selector, ops, att);
            } catch (ClosedChannelException ignore) {
            }
        }
        return null;
    }

    public SelectionKey register(ServerSocketChannel channel, int ops, Object att) {
        try {
            return channel.register(selector, ops, att);
        } catch (ClosedChannelException ignore) {
            return null;
        }
    }

    public void setOnPollEventListener(OnPoll l) {
        this.pollEventListener = l;
    }

    private Set<SelectionKey> pollKeys(long timeout) {
        try {
            if (selector.select(timeout) > 0) {
                return selector.selectedKeys();
            }
        } catch (IOException ignore) {

        }
        return Collections.emptySet();
    }

    public void netPoll() {
        var keys = pollKeys(1L);
        var l = pollEventListener;
        if (l != null) {
            for (var key : keys) {
                if (!key.isValid()) {
                    continue;
                }

                var att = key.attachment();
                SocketChannel client = null;
                if (key.isAcceptable()) {
                    try {
                        var server = (ServerSocketChannel) key.channel();
                        client = server.accept();
                        if (client != null) {
                            l.onAccept(client, att);
                        }
                        continue;
                    } catch (IOException ignore) {

                    }
                }

                client = (SocketChannel) key.channel();

                if (client == null) {
                    continue;
                }
                if (!client.isConnected()) {
                    key.cancel();
                } else {
                    if (key.isWritable()) {
                        l.onWrite(client, att);
                    }
                    if (key.isReadable()) {
                        l.onRead(client, att);
                    }
                    if (key.isConnectable()) {
                        l.onConnect(client, att);
                    }
                }
            }
        }
        keys.clear();
    }
}
