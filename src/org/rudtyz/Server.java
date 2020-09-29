package org.rudtyz;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server implements NetPoll.OnPoll{
    private ServerSocketChannel serverChannel = null;
    private NetPoll poll = new NetPoll();

    void listenAndServe(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        poll.register(serverChannel, SelectionKey.OP_ACCEPT, null);
        poll.setOnPollEventListener(this);
        while (true) {
            poll.netPoll();
        }
    }


    public void write(SocketChannel client, ReceiveBuffer b) {
        poll.register(client, SelectionKey.OP_READ | SelectionKey.OP_WRITE, b);
    }


    public void onMessage(SocketChannel client, byte[] b, int len) {
        System.out.println(new String(b, 0, len));
        write(client, new ReceiveBuffer(b, len));
    }

    @Override
    public void onRead(SocketChannel client, Object att) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        try {
            int receiveSize = client.read(buf);

            if (receiveSize <= 0) {
//                key.cancel();
                try {
                    client.close();
                } catch (IOException ignore) {
                }
                return;
            }

            poll.register(client, SelectionKey.OP_READ, null);

            onMessage(client, buf.array(), receiveSize);
        } catch (IOException ignore) {
        }
    }

    @Override
    public void onWrite(SocketChannel client, Object att) {
        var buf = (ReceiveBuffer) att;
        var sendBuffer = buf.toByteBuffer();
        try {
            client.write(sendBuffer);
        } catch (IOException e) {
            if (!client.isConnected()) {
                return;
            }
            System.out.println("write fail");
        }

        poll.register(client, SelectionKey.OP_READ, null);
    }

    @Override
    public void onConnect(SocketChannel client, Object att) {

    }

    @Override
    public void onAccept(SocketChannel client, Object att) {
        try {
            client.configureBlocking(false);
            poll.register(client, SelectionKey.OP_READ, null);
        } catch (IOException e) {
            System.out.println("client connect fail");
            try {
                client.close();
            } catch (IOException ignore) {
            }
        }
    }
}
