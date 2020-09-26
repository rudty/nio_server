package org.rudtyz;

import java.nio.ByteBuffer;

public class ReceiveBuffer {

    private int receiveLength;
    private byte[] buf;


    public ReceiveBuffer(int receiveLength, byte[] buf) {
        this.receiveLength = receiveLength;
        this.buf = buf;
    }

    public int getReceiveLength() {
        return receiveLength;
    }

    public byte[] getBuffer() {
        return buf;
    }

}
