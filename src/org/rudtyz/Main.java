package org.rudtyz;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Main {

    public static void doAccept(SelectionKey key) {
        var serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel client = null;
        try {
            client = serverSocketChannel.accept();
            client.configureBlocking(false);
            client.register(Poll.selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            System.out.println("client connect fail");
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public static void registerRead(SocketChannel client) {
        if (client.isConnected()) {
            try {
                client.register(Poll.selector, SelectionKey.OP_READ);
            } catch (ClosedChannelException ignore) {
            }
        }
    }

    public static void doWrite(SelectionKey key) {
        var client = (SocketChannel) key.channel();

        var buf = (ReceiveBuffer) key.attachment();
        var sendBuffer = buf.toByteBuffer();
        try {
            client.write(sendBuffer);
        } catch (IOException e) {
            if (!client.isConnected()) {
                return;
            }
            System.out.println("write fail");
        }

        registerRead(client);
    }

    public static void doRead(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();

        ByteBuffer buf = ByteBuffer.allocate(1024);
        try {
            int receiveSize = client.read(buf);

            if (receiveSize <= 0) {
                key.cancel();
                try {
                    client.close();
                } catch (IOException ignore) {
                }
                return;
            }

            registerRead(client);

            onMessage(client, buf.array(), receiveSize);
        } catch (IOException ignore) {
        }
    }


    public static void write(SocketChannel client, ReceiveBuffer b) {
        if (client.isConnected()) {
            try {
                client.register(
                        Poll.selector,
                        SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                        b);
            } catch (ClosedChannelException ignore) {
            }
        }
    }


    public static void onMessage(SocketChannel client, byte[] b, int len) {
        System.out.println(new String(b, 0, len));
        write(client, new ReceiveBuffer(b, len));
    }

    public static void netPoll() {
        var keys = Poll.selector.selectedKeys();

        for (var key : keys) {
            if (!key.isValid()) {
                continue;
            }

            if (key.isAcceptable()) {
                doAccept(key);
            }
            if (key.isWritable()) {
                doWrite(key);
            }
            if (key.isReadable()) {
                doRead(key);
            }
        }
        keys.clear();
    }

    public static void main(String[] args) throws Exception {
        var serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.register(Poll.selector, SelectionKey.OP_ACCEPT);

        while (true) {
            if (Poll.selector.select(10L) > 0) {

            }
        }
    }
}
