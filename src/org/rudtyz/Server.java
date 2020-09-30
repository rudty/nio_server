package org.rudtyz;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server implements NetPoll.OnPollEventListener{
    private ServerSocketChannel serverChannel = null;
    private NetPoll poll = new NetPoll();

    void listenAndServe(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        poll.registerAccept(serverChannel, null);
        poll.setOnPollEventListener(this);
        while (true) {
            poll.netPoll();
        }
    }


    public void write(SocketChannel client, ReceiveBuffer b) throws IOException {
        poll.registerWrite(client, b);
    }


    public void onMessage(SocketChannel client, byte[] b, int len) throws IOException {
        System.out.println(new String(b, 0, len));
        write(client, new ReceiveBuffer(b, len));
    }

    @Override
    public void onRead(SocketChannel client, Object att) throws IOException{
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int receiveSize = client.read(buf);

        if (receiveSize <= 0) {
//                key.cancel();
            try {
                client.close();
            } catch (IOException ignore) {
            }
            return;
        }

        onMessage(client, buf.array(), receiveSize);

    }

    @Override
    public void onWrite(SocketChannel client, Object att) throws IOException {
        var buf = (ReceiveBuffer) att;
        var sendBuffer = buf.toByteBuffer();
        client.write(sendBuffer);
    }

    @Override
    public void onAccept(SocketChannel client, Object att) throws IOException {

    }

    @Override
    public void onIdle() {
        System.out.println("IDLE");
    }
}
