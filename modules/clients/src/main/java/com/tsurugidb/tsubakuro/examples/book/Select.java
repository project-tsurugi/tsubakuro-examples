package com.tsurugidb.tsubakuro.examples.book;

import java.io.IOException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.ResultSet;

public final class Select {
    public static void doSelect(String url) throws IOException, ServerException, InterruptedException {
        try (
             Session session = SessionBuilder.connect(url).create();
             SqlClient sqlClient = SqlClient.attach(session);
             Transaction transaction = sqlClient.createTransaction().get();) {

            try (ResultSet resultSet = transaction.executeQuery("SELECT * FROM foo").get();) {
                doSomeWorkUsingResultset(resultSet);
            }
            transaction.commit().get();
        }
    }

    private static void doSomeWorkUsingResultset(ResultSet resultSet) throws InterruptedException, IOException, ServerException {
        int count = 0;
        var metadata = resultSet.getMetadata().getColumns();

        while (resultSet.nextRow()) {
            System.out.printf("---- ( record No. %d )----\n", count);
            int columnIndex = 0;
            while (resultSet.nextColumn()) {
                if (!resultSet.isNull()) {
                    switch (metadata.get(columnIndex).getAtomType()) {
                    case INT4:
                        System.out.printf("column No. %d (INT4): %d\n", columnIndex, resultSet.fetchInt4Value());
                        break;
                    case INT8:
                        System.out.printf("column No. %d (INT8): %d\n", columnIndex, resultSet.fetchInt8Value());
                        break;
                    case FLOAT4:
                        System.out.printf("column No. %d (FLOAT4): %f\n", columnIndex, resultSet.fetchFloat4Value());
                        break;
                    case FLOAT8:
                        System.out.printf("column No. %d (FLOAT8): %f\n", columnIndex, resultSet.fetchFloat8Value());
                        break;
                    case CHARACTER:
                        System.out.printf("column No. %d (CHARACTER): %s\n", columnIndex, resultSet.fetchCharacterValue());
                        break;
                    default:
                        throw new IOException("the column type is invalid");
                    }
                } else {
                    System.out.println("the column is NULL");
                }
                columnIndex++;
            }
            System.out.println();
            count++;
        }
    }

    private Select() {
    }
}
