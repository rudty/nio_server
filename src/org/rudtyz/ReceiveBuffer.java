package org.rudtyz;


import java.nio.ByteBuffer;

public class ReceiveBuffer {

    private byte[] buf;
    private int offset;
    private int length;

    public ReceiveBuffer(byte[] buf) {
        this(buf, 0, buf.length);
    }
    public ReceiveBuffer(byte[] buf, int length) {
        this(buf, 0, length);
    }

    public ReceiveBuffer(byte[] buf, int offset, int length) {
        this.buf = buf;
        this.offset = offset;
        this.length = length;
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(buf, offset, length);
    }

}
