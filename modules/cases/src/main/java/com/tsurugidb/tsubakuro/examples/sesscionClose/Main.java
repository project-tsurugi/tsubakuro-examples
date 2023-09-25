package com.tsurugidb.tsubakuro.examples.sesscionClose;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;


public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");

    public static void main(String[] args) {
        for (int i = 0; i < 50; i++) {
            try {
                Session session = SessionBuilder.connect(url).create();
                SqlClient sqlClient = SqlClient.attach(session);
                var futureTransaction = sqlClient.createTransaction();
                
                session.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        System.out.println("================");
        for (int i = 0; i < 50; i++) {
            try {
                Session session = SessionBuilder.connect(url).create();
                SqlClient sqlClient = SqlClient.attach(session);
                var futureTransaction = sqlClient.createTransaction().get();

                session.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private Main(String[] args) {
    }
}
