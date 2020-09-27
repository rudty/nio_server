package org.rudtyz;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Main {

    public static void doAccept(SelectionKey key) throws Exception {
        var serverSocketChannel = (ServerSocketChannel) key.channel();
        var client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(Poll.selector, SelectionKey.OP_READ);
    }

    public static void doWrite(SelectionKey key) throws Exception {
        var client = (SocketChannel) key.channel();

        var buf = (ReceiveBuffer) key.attachment();
        var sendBuffer = buf.toByteBuffer();
        client.write(sendBuffer);

        client.register(Poll.selector, SelectionKey.OP_READ);
    }

    public static void doRead(SelectionKey key) throws Exception {
        SocketChannel client = (SocketChannel) key.channel();

        ByteBuffer buf = ByteBuffer.allocate(1024);
        int receiveSize = client.read(buf);

        if (receiveSize <= 0) {
            key.cancel();
            client.close();
            return;
        }


        onMessage(client, buf.array(), receiveSize);
    }


    public static void write(SocketChannel client, ReceiveBuffer b) throws ClosedChannelException {
        client.register(
                Poll.selector,
                SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                b);
    }


    public static void onMessage(SocketChannel client, byte[] b, int len) throws Exception{
        System.out.println(new String(b, 0, len));
        write(client, new ReceiveBuffer(b, len));
    }

    public static void main(String[] args) throws Exception {
        var serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.register(Poll.selector, SelectionKey.OP_ACCEPT);

        while (true) {
            if (Poll.selector.select(10L) > 0) {
                var keys = Poll.selector.selectedKeys();

                for (var key : keys) {
                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        doAccept(key);
                    } else if (key.isWritable()) {
                        doWrite(key);
                    } else if (key.isReadable()) {
                        doRead(key);
                    }
                }
                keys.clear();
            }
        }
    }
}
