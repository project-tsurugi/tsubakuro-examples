package com.tsurugidb.tsubakuro.examples.serviceClient;

import java.io.IOException;

import com.tsurugidb.tsubakuro.client.ServiceClientCollector;

public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");

    private Main(String[] args) {
    }

    public static void main(String[] args) {
        try {
            var classes = ServiceClientCollector.collect(false);
            for (var elem: classes) {
                System.out.println(elem.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
