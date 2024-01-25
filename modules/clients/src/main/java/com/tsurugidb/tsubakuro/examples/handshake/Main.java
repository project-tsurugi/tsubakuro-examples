package com.tsurugidb.tsubakuro.examples.handshake;

import java.io.IOException;

import com.tsurugidb.tsubakuro.exception.ServerException;

public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");

    public static void main(String[] args) {
        try {
            Client.doClient(url);
        } catch (IOException | ServerException | InterruptedException e) {
            System.out.println(e);
            e.printStackTrace();
            return;
        }
    }

    private Main(String[] args) {
    }
}
