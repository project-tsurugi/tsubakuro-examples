package com.tsurugidb.tsubakuro.examples.fault;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.ResultSet;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");
    private static long timeout = 500;  // milliseconds

    private static boolean query = false;

    public static void main(String[] args) {
        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(timeout, TimeUnit.MILLISECONDS);
             SqlClient sqlClient = SqlClient.attach(session);
             var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {

            var resultSet = transaction.executeQuery("select * from some_table").await();
            var metadata = resultSet.getMetadata().getColumns();
            System.out.println("begin fetching records");
            while (resultSet.nextRow()) {
                int columnIndex = 0;
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
            System.out.println("reached end of records");
            resultSet.close();
        } catch (Exception e) {
            System.out.println("catch exception");
            e.printStackTrace();
            System.err.println(e);
        }
    }
}
