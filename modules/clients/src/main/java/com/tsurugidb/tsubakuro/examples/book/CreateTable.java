package com.tsurugidb.tsubakuro.examples.book;

import java.io.IOException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;

public final class CreateTable {
    public static void doCreateTable(String url) throws IOException, ServerException, InterruptedException {
        try (Session session = SessionBuilder.connect(url).create();
             SqlClient sqlClient = SqlClient.attach(session);
             Transaction transaction = sqlClient.createTransaction().get()) {

            transaction.executeStatement("CREATE TABLE foo (bar1 INT, bar2 DOUBLE, bar3 VARCHAR(20), PRIMARY KEY(bar1))").get();
            transaction.commit().get();
        }
    }

    private CreateTable() {
    }
}
