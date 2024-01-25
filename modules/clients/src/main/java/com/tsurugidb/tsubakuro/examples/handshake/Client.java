package com.tsurugidb.tsubakuro.examples.handshake;

import java.io.IOException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;

public final class Client {
    // this example requires tsurugi.ini that has prepare_test_tables=true entry in sql section.
    public static void doClient(String url) throws IOException, ServerException, InterruptedException {
        try (Session session = SessionBuilder.connect(url).withLabel("exampleLabel").withApplicationName("exampleApplication").create();
             SqlClient sqlClient = SqlClient.attach(session);
             Transaction transaction = sqlClient.createTransaction().get()) {

            var resultSet = transaction.executeQuery("SELECT * FROM T0").get();
            var metadata = resultSet.getMetadata().getColumns();

            int count = 0;
            while (resultSet.nextRow()) {
                int columnIndex = 0;
                while (resultSet.nextColumn()) {
                    if (!resultSet.isNull()) {
                        switch (metadata.get(columnIndex).getAtomType()) {
                        case INT4:
                            resultSet.fetchInt4Value();
                            break;
                        case INT8:
                            resultSet.fetchInt8Value();
                            break;
                        case FLOAT4:
                            resultSet.fetchFloat4Value();
                            break;
                        case FLOAT8:
                            resultSet.fetchFloat8Value();
                            break;
                        case CHARACTER:
                            resultSet.fetchCharacterValue();
                            break;
                        default:
                            throw new IOException("the column type is invalid");
                        }
                    }
                    columnIndex++;
                }
                count++;
            }

            transaction.commit().get();
        }
        
    }

    private Client() {
    }
}
