package org.rudtyz;

import java.io.IOException;
import java.nio.channels.SocketChannel;


public class NetPollTask {

    @FunctionalInterface
    public interface OnPollTask {
        void onPollTask(SocketChannel channel, Object att) throws IOException;
    }

    private OnPollTask task;
    private Object att;

    public NetPollTask(OnPollTask task, Object att) {
        this.task = task;
        this.att = att;
    }

    public void run(SocketChannel channel) throws IOException {
        if (task != null) {
            task.onPollTask(channel, att);
        }
    }
}
