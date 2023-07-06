package com.tsurugidb.tsubakuro.examples.multipleSelect;

import java.io.IOException;
import java.util.ArrayList;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.ResultSet;

public final class Select {
    private static ArrayList<ResultSet> resultSets = new ArrayList<>();
    public static void doSelect(String url) throws IOException, ServerException, InterruptedException {
        System.out.println("select entered");
        try (
             Session session = SessionBuilder.connect(url).create();
             SqlClient sqlClient = SqlClient.attach(session);
             Transaction transaction = sqlClient.createTransaction().get();) {

            for (int i = 0; i < 200; i++) {
                resultSets.add(transaction.executeQuery("SELECT * FROM foo").get());
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
        System.out.println("select completed");
    }

    private Select() {
    }
}
