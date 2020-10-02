package org.rudtyz;

public class Main {

    public static void main(String[] args) throws Exception {
        new EchoServer().listenAndServe(8080);
    }
}
