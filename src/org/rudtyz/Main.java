package org.rudtyz;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
        SocketChannel client = (SocketChannel) key.channel();

        ReceiveBuffer buf = (ReceiveBuffer) key.attachment();
        var sendBuffer = ByteBuffer.wrap(buf.getBuffer(), 0, buf.getReceiveLength());
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

        var arr = buf.array();
        System.out.println(new String(arr));

        client.register(
                Poll.selector,
                SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                new ReceiveBuffer(receiveSize, arr));
    }

    public static void main(String[] args) throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.register(Poll.selector, SelectionKey.OP_ACCEPT);

        while (true) {
            if (Poll.selector.select(1L) > 0) {
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

//                    key.cancel();
                }
                keys.clear();
            }
            Thread.sleep(100);
        }
    }
}
