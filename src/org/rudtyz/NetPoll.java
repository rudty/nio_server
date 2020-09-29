package org.rudtyz;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Collections;
import java.util.Set;

public class NetPoll {
    public interface OnPoll{
        /**
         * client poll 성공했을때 호출될 인터페이스
         * @param ops SelectionKey 의 OP_READ, OP_WRITE, OP_CONNECT, OP_ACCEPT
         * @param channel 클라이언트
         * @param att register 에서 인자로 넣은 것
         */
        void onPoll(int ops, SocketChannel channel, Object att);
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

                if (key.isAcceptable()) {
                    try {
                        var server = (ServerSocketChannel) key.channel();
                        var client = server.accept();
                        if (client != null) {
                            l.onPoll(SelectionKey.OP_ACCEPT, client, att);
                        }
                        continue;
                    } catch (IOException ignore) {

                    }
                }

                var client = (SocketChannel) key.channel();
                int ops = 0;

                if (client == null) {
                    continue;
                }
                if (!client.isConnected()) {
                    key.cancel();
                }
                if (key.isWritable()) {
                    ops = SelectionKey.OP_WRITE;
                }
                if (key.isReadable()) {
                    ops = SelectionKey.OP_READ;
                }
                if (key.isConnectable()) {
                    ops = SelectionKey.OP_CONNECT;
                }
                l.onPoll(ops, client, att);
            }
        }
        keys.clear();
    }
}
