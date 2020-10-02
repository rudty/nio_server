package org.rudtyz;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerBase implements NetPoll.OnPollEventListener{
    private ServerSocketChannel serverChannel = null;
    private NetPoll poll = new NetPoll();

    public final void listenAndServe(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        poll.registerAccept(serverChannel, null);
        poll.setOnPollEventListener(this);
        while (true) {
            poll.netPoll();
        }
    }

    public final void write(SocketChannel client, ReceiveBuffer b) throws IOException {
        poll.registerWrite(client, b);
    }

    public final void write(SocketChannel client, byte[] b) throws IOException {
        write(client, b, 0, b.length);
    }

    public final void write(SocketChannel client, byte[] b, int len) throws IOException {
        write(client, b, 0, len);
    }

    public final void write(SocketChannel client, byte[] b, int offset, int len) throws IOException {
        write(client, new ReceiveBuffer(b, offset, len));
    }

    public void onMessage(SocketChannel client, byte[] b, int len) throws IOException {
    }

    @Override
    public final void onRead(SocketChannel client, Object att) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int receiveSize = client.read(buf);

        if (receiveSize <= 0) {
            poll.closeClient(client);
        }

        onMessage(client, buf.array(), receiveSize);

    }

    @Override
    public final void onWrite(SocketChannel client, Object att) throws IOException {
        var buf = (ReceiveBuffer) att;
        var sendBuffer = buf.toByteBuffer();
        client.write(sendBuffer);
    }

    @Override
    public final void onAccept(SocketChannel client, Object att) throws IOException {

    }

    @Override
    public void onIdle() {
    }
}
