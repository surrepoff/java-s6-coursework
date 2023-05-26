package org.surrepoff.stat;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        ServerMonitor serverMonitor = new ServerMonitor();
        try {
            serverMonitor.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}