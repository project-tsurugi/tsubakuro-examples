package com.tsurugidb.tsubakuro.examples.resultsetClose;

import java.io.IOException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;

public class Insert {
    public static void doInsert(String url, boolean testResponse) throws IOException, ServerException, InterruptedException {
        try (Session session = SessionBuilder.connect(url).create();
             SqlClient sqlClient = SqlClient.attach(session);
             PreparedStatement preparedStatement = sqlClient.prepare("INSERT INTO foo (bar1, bar2, bar3) VALUES (:int_for_bar1, :double_for_bar2, :char_for_bar3)",
                                                       Placeholders.of("int_for_bar1", long.class),
                                                       Placeholders.of("double_for_bar2", double.class),
                                                       Placeholders.of("char_for_bar3", String.class)).get();
             Transaction transaction = sqlClient.createTransaction().get();) {

            // insert first data into foo table
            var f1 = transaction.executeStatement(preparedStatement,
                                         Parameters.of("int_for_bar1", (long) 1234),
                                         Parameters.of("double_for_bar2", (double) 56.789),
                                         Parameters.of("char_for_bar3", "text for first data"));
            if (testResponse) {
                f1.close();  // test of FutureResponse close without performing get()
            } else {
                f1.get();  // normal operation;
            }
            // insert second data into foo table
            var f2 = transaction.executeStatement(preparedStatement,
                                         Parameters.of("int_for_bar1", (long) 5678),
                                         Parameters.of("double_for_bar2", (double) 123.45),
                                         Parameters.of("char_for_bar3", "text for second data"));
            if (testResponse) {
                f2.close();  // test of FutureResponse close without performing get()
            } else {
                f2.get();  // normal operation;
            }
            transaction.commit().get();
        }
    }

    private Insert() {
    }
}
