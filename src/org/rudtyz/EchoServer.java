package org.rudtyz;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class EchoServer extends ServerBase {

    @Override
    public void onMessage(SocketChannel client, byte[] b, int len) throws IOException {
        super.onMessage(client, b, len);
        String msg = new String(b, 0, len, Charset.defaultCharset());
        System.out.println(msg);
        write(client, b, len);
    }

    @Override
    public void onIdle() {
        super.onIdle();
        System.out.println("IDLE");
    }
}
