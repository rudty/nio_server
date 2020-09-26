package org.rudtyz;

import java.io.IOException;
import java.nio.channels.Selector;

public class Poll {
    public static Selector selector;

    static {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
