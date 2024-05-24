package com.tsurugidb.tsubakuro.examples.longQuery;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.ResultSet;

public class Select {
    private static long timeout = 5000;

    static void query(Transaction transaction, String query, boolean suppressDisplay) {
        try (var resultSet = transaction.executeQuery(query).get(timeout, TimeUnit.MILLISECONDS)) {
            if (!suppressDisplay) {
                printResultset(resultSet);
            }
        } catch (ServerException | InterruptedException | IOException | TimeoutException e) {
            // Intentionally grasps exceptions to allow the caller to continue processing
            System.err.println(e);
        }
    }
    static void query(Transaction transaction, String query) {
        query(transaction, query, false);
    }

    static void printResultset(ResultSet resultSet) throws InterruptedException, IOException, ServerException {
        int count = 1;

        while (resultSet.nextRow()) {
            System.out.println("---- ( count: " + count + " ) ----");
            count++;
            int columnIndex = 0;
            var metadata = resultSet.getMetadata().getColumns();
            while (resultSet.nextColumn()) {
                if (!resultSet.isNull()) {
                    switch (metadata.get(columnIndex).getAtomType()) {
                    case INT4:
                        System.out.println(resultSet.fetchInt4Value());
                        break;
                    case INT8:
                        System.out.println(resultSet.fetchInt8Value());
                        break;
                    case FLOAT4:
                        System.out.println(resultSet.fetchFloat4Value());
                        break;
                    case FLOAT8:
                        System.out.println(resultSet.fetchFloat8Value());
                        break;
                    case CHARACTER:
                        System.out.println(resultSet.fetchCharacterValue());
                        break;
                    default:
                        throw new IOException("the column type is invalid");
                    }
                } else {
                    System.out.println("the column is NULL");
                }
                columnIndex++;
            }
        }
    }
}
