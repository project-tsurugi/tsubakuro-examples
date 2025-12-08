package com.tsurugidb.tsubakuro.examples.getSystemInfo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.system.proto.SystemResponse;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.exception.CoreServiceException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.system.SystemClient;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");
    private static long timeout = 500;  // milliseconds

    public static void main(String[] args) {
        try (
             Session session = SessionBuilder.connect(url)
             .create(timeout, TimeUnit.MILLISECONDS);
             SystemClient systemClient = SystemClient.attach(session); ) {

            var info = systemClient.getSystemInfo().await();
            System.out.println("name = " + info.getName());
            System.out.println("version = " + info.getVersion());
            System.out.println("date = " + info.getDate());
            System.out.println("url = " + info.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
