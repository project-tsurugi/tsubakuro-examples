package com.tsurugidb.tsubakuro.examples.isAlive;

import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.common.ShutdownType;

public class SessionIsAliveAndClose {
    private static String url = System.getProperty("tsurugi.dbname");

    public static void main(String[] args) throws Exception {

        var session = SessionBuilder.connect(url).create();
        System.out.println(String.format("session.isAlive=%s", session.isAlive()));

        session.shutdown(ShutdownType.FORCEFUL).await();
        System.out.println("session.shutdown done.");

        session.close();
        System.out.println("session.closed done.");

    }
}
