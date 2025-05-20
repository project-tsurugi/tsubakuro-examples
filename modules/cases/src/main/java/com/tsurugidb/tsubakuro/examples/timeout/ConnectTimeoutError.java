package com.tsurugidb.tsubakuro.examples.timeout;

import java.util.concurrent.TimeUnit;

import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;

public class ConnectTimeoutError {
    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    public static void main(String[] args) throws Exception {
        try (var session = SessionBuilder.connect(url).createAsync().get(Long.MAX_VALUE, TimeUnit.MILLISECONDS); //
                var sqlClient = SqlClient.attach(session)) {
            try (var tx = sqlClient.createTransaction().await()) {
                tx.executeQuery("select * from TBL01").await();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            System.out.println("isAlive=" + session.isAlive());
        }
        System.out.println("----");
        try (var session = SessionBuilder.connect(url).createAsync().get(Long.MAX_VALUE, TimeUnit.MILLISECONDS); //
                var sqlClient = SqlClient.attach(session)) {
            try (var tx = sqlClient.createTransaction().await()) {
                tx.executeQuery("select * from TBL01").await();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            System.out.println("isAlive=" + session.isAlive());
        }
    }
}
