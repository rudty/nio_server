package org.rudtyz;

import java.io.IOException;
import java.nio.channels.*;
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
    private final Selector readWriteSelector;
    private OnPollEventListener pollEventListener = null;

    private static long POLL_TIMEOUT = 1L;

    public NetPoll() {
        try {
            serverSocketSelector = Selector.open();
            readWriteSelector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                        client.register(readWriteSelector, SelectionKey.OP_READ, new NetPollTask(this::callRead, null));
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

    private void callRead(SocketChannel client, Object att) throws IOException {
        var l = pollEventListener;
        if (l != null) {
            l.onRead(client, att);
        }
    }

    private void callWrite(SocketChannel client, Object att) throws IOException {
        var l = pollEventListener;
        if (l != null) {
            l.onWrite(client, att);
        }
    }

    /**
     * 다음 poll 시 write
     * @param channel 클라
     * @param att 저장할 값 아무거나 (callback 에서 받음)
     * @throws ClosedChannelException 클라에서 연결을 종료하였음
     */
    public void registerWrite(SocketChannel channel, Object att) throws ClosedChannelException {
        if (channel.isConnected()) {
            channel.register(readWriteSelector,
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                    new NetPollTask(this::callWrite, att));
        }
    }

    private void closeClient(SelectionKey key, SocketChannel channel) {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Exception ignore) {

        }
        try {
            if (key != null) {
                key.cancel();
            }
        } catch (Exception ignore) {

        }
    }

    /**
     * 클라이언트의 연결을 닫습니다
     * @param channel 클라이언트
     */
    public void closeClient(SocketChannel channel) {
        var key = channel.keyFor(readWriteSelector);
        closeClient(key, channel);
    }

    /**
     * read, write 시의 poll 동작
     * @return 몇개의 read, write 작업을 했는지
     */
    private int pollClient() {
        var keys = pollKeys(readWriteSelector, POLL_TIMEOUT);
        var l = pollEventListener;
        int readWriteChannels = 0;
        if (l != null) {
            for (var key : keys) {
                var att = key.attachment();
                var client = (SocketChannel) key.channel();
                if (!client.isConnected()) {
                    closeClient(key, client);
                    continue;
                }

                try {

                     // 반드시 read 를 추가해서 두번 write 하지 않게 변경
                    client.register(readWriteSelector,
                            SelectionKey.OP_READ,
                            new NetPollTask(this::callRead, null));

                    var task = (NetPollTask)att;
                    task.run(client);
                    readWriteChannels += 1;
                } catch (IOException e) {
                    closeClient(key, client);
                } catch (Throwable t) {
                    t.printStackTrace();
                    closeClient(key, client);
                }
            }
        }
        keys.clear();

        return readWriteChannels;
    }

    /**
     * 밖에서 계속 이 함수를 호출해서 poll 작업을 수행
     * 호출시 selector 를 검사해서 accept, read, write 작업이 있다면
     * 해당 작업의 수행을, 없다면 idle 함수를 실행
     */
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
