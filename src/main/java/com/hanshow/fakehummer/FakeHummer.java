package com.hanshow.fakehummer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeHummer {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger("MAIN");

        try {
            HummerContext.getInstance();
            HttpServer httpServer = new HttpServer();
            httpServer.start();
        } catch (Throwable t) {
            logger.error("Failed to start FakeHummer", t);
            System.exit(1);
        }
    }
}
