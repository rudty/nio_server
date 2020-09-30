package org.rudtyz;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Collections;
import java.util.Set;

public class NetPoll {
    public interface OnPollEventListener {
        /**
         * 클라이언트에서 메세지를 보낼 시 호출될 함수
         *
         * @param channel 클라이언트
         * @param att     register 에서 인자로 넣은 것
         */
        void onRead(SocketChannel channel, Object att) throws IOException;

        /**
         * Write 호출 시 수행할 함수
         *
         * @param channel 클라이언트
         * @param att     register 에서 인자로 넣은 것
         */
        void onWrite(SocketChannel channel, Object att) throws IOException;

        /**
         * 클라이언트와 연결되었을때 호출될 함수
         *
         * @param channel 클라이언트
         * @param att     register 에서 인자로 넣은 것
         */
        void onAccept(SocketChannel channel, Object att) throws IOException;

        /**
         * Poll 을 아무것도 하지 않았을때 실행될 함수
         */
        void onIdle();
    }
    private final Selector serverSocketSelector;
    private final Selector clientSocketSelector;
    private OnPollEventListener pollEventListener = null;

    private static long POLL_TIMEOUT = 1L;

    public NetPoll() {
        try {
            serverSocketSelector = Selector.open();
            clientSocketSelector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void register(SocketChannel channel, int ops, Object att) throws ClosedChannelException {
        channel.register(clientSocketSelector, ops, att);
    }

    public void registerRead(SocketChannel channel, Object att) throws ClosedChannelException {
        if (channel.isConnected()) {
            register(channel, SelectionKey.OP_READ, att);
        }
    }

    public void registerWrite(SocketChannel channel, Object att) throws ClosedChannelException {
        if (channel.isConnected()) {
            register(channel, SelectionKey.OP_WRITE, att);
        }
    }

    public void registerReadAndWrite(SocketChannel channel, Object att) throws ClosedChannelException {
        if (channel.isConnected()) {
            register(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE, att);
        }
    }

    public void registerAccept(ServerSocketChannel channel, Object att) throws ClosedChannelException {
        try {
            channel.register(serverSocketSelector, SelectionKey.OP_ACCEPT, att);
        } catch (ClosedChannelException ignore) {
        }
    }

    public void setOnPollEventListener(OnPollEventListener l) {
        this.pollEventListener = l;
    }

    private Set<SelectionKey> pollKeys(Selector selector, long timeout) {
        try {
            if (selector.select(timeout) > 0) {
                return selector.selectedKeys();
            }
        } catch (IOException ignore) {

        }
        return Collections.emptySet();
    }

    private void closeKey(SelectionKey key) {
        var chan = key.channel();
        try {
            chan.close();
        } catch (Exception ignore) {

        }
        try {
            key.cancel();
        } catch (Exception ignore) {

        }
    }

    private int pollServer() {
        var keys = pollKeys(serverSocketSelector, POLL_TIMEOUT);
        var l = pollEventListener;
        int acceptChannels = 0;
        if (l != null) {
            for (var key : keys) {
                try {
                    var server = (ServerSocketChannel) key.channel();
                    var client = server.accept();
                    if (client != null) {
                        client.configureBlocking(false);
                        l.onAccept(client, key.attachment());
                        acceptChannels += 1;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        keys.clear();
        return acceptChannels;
    }

    private int pollClient() {
        var keys = pollKeys(clientSocketSelector, POLL_TIMEOUT);
        var l = pollEventListener;
        int readWriteChannels = 0;
        if (l != null) {
            for (var key : keys) {
                var att = key.attachment();
                var client = (SocketChannel) key.channel();
                if (!client.isConnected()) {
                    closeKey(key);
                    continue;
                }
                try {
                    if (key.isWritable()) {
                        try {
                            l.onWrite(client, att);
                        } catch (ClosedChannelException e) {
                            throw e;
                        } catch (Throwable ignore) {

                        }
                        readWriteChannels += 1;
                    }

                    if (key.isReadable()) {
                        try {
                            l.onRead(client, att);
                        } catch (ClosedChannelException e) {
                            throw e;
                        } catch (Throwable ignore) {

                        }
                        readWriteChannels += 1;
                    }
                } catch (Throwable t) {
                    closeKey(key);
                }
            }
        }
        keys.clear();

        return readWriteChannels;
    }

    public void netPoll() {
        int w = 0;
        w += pollServer();
        w += pollClient();

        if (w == 0) {
            var l = pollEventListener;
            if (l != null) {
                l.onIdle();
            }
        }
    }
}
