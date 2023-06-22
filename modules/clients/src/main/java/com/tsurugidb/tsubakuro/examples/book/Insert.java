package com.tsurugidb.tsubakuro.examples.book;

import java.io.IOException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;

public class Insert {
    private static final String sql = "INSERT INTO foo (bar1, bar2, bar3) VALUES (:int_for_bar1, :double_for_bar2, :char_for_bar3)";

    public static void doInsert(String url) throws IOException, ServerException, InterruptedException {
        try (Session session = SessionBuilder.connect(url).create();
             SqlClient sqlClient = SqlClient.attach(session);
             var preparedStatement = sqlClient.prepare(sql,
                                                       Placeholders.of("int_for_bar1", long.class),
                                                       Placeholders.of("double_for_bar2", double.class),
                                                       Placeholders.of("char_for_bar3", String.class)).get();
             Transaction transaction = sqlClient.createTransaction().await();) {

            // first data
            transaction.executeStatement(preparedStatement,
                                         Parameters.of("int_for_bar1", (long) 1234),
                                         Parameters.of("double_for_bar2", (double) 56.789),
                                         Parameters.of("char_for_bar3", "first data")).get();
            // second data
            transaction.executeStatement(preparedStatement,
                                         Parameters.of("int_for_bar1", (long) 5678),
                                         Parameters.of("double_for_bar2", (double) 123.45),
                                         Parameters.of("char_for_bar3", "second data")).get();
            transaction.commit().get();
        }
    }

    private Insert() {
    }
}
