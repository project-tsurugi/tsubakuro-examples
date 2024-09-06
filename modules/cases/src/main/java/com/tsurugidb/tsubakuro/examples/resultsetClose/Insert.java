package com.tsurugidb.tsubakuro.examples.resultsetClose;

import java.io.IOException;
import java.util.Random;

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
        Random rand = new Random();
        int num = rand.nextInt(10) + 100;
    
        try (Session session = SessionBuilder.connect(url).create();
             SqlClient sqlClient = SqlClient.attach(session);
             PreparedStatement preparedStatement = sqlClient.prepare("INSERT INTO foo (bar1, bar2, bar3) VALUES (:int_for_bar1, :double_for_bar2, :char_for_bar3)",
                                                       Placeholders.of("int_for_bar1", long.class),
                                                       Placeholders.of("double_for_bar2", double.class),
                                                       Placeholders.of("char_for_bar3", String.class)).get();
             Transaction transaction = sqlClient.createTransaction().get();) {

            oneInsert(transaction, preparedStatement, testResponse, 1234, 56.789, "text for first data");
            oneInsert(transaction, preparedStatement, testResponse, 5678, 123.45, "text for second data");
            for (long i = 10000; i < 100000; i++) {
                oneInsert(transaction, preparedStatement, testResponse, i, (double) rand.nextInt(10000) / (double) 1000., "text for other data");
            }
            
            transaction.commit().get();
        }
    }

    private static void oneInsert(Transaction transaction, PreparedStatement preparedStatement, boolean testResponse, long lv, double dv, String sv) throws IOException, ServerException, InterruptedException {
        // insert first data into foo table
        var f = transaction.executeStatement(preparedStatement,
                                             Parameters.of("int_for_bar1", lv),
                                             Parameters.of("double_for_bar2", dv),
                                             Parameters.of("char_for_bar3", sv));
        if (testResponse) {
            f.close();  // test of FutureResponse close without performing get()
        } else {
            f.get();  // normal operation;
        }
    }
    
    private Insert() {
    }
}
