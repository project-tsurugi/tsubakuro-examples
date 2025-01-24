package com.tsurugidb.tsubakuro.examples.tables;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    public static void main(String[] args) {
        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             SqlClient sqlClient = SqlClient.attach(session)) {

            var searchPath = sqlClient.getSearchPath().get();
            System.out.println("---- schema names ----");
            for (var s : searchPath.getSchemaNames()) {
                System.out.println("  " + s);
            }
            var tableList = sqlClient.listTables().get();
            for (var t : tableList.getTableNames()) {
                var metadata = sqlClient.getTableMetadata(t).get();
                System.out.println("---- table name = " + t + " ----");
                var dn = metadata.getDatabaseName();
                var sn = metadata.getSchemaName();
                var tn = metadata.getTableName();
                System.out.println("DatabaseName = " + (dn.isPresent() ? dn.get() : "DatabaseName not exist"));
                System.out.println("SchemaName = " + (sn.isPresent() ? sn.get() : "SchemaName not exist"));
                System.out.println("TableName = " + tn);
                for (var c : metadata.getColumns()) {
                    System.out.println("column = " + c);
                    System.out.println();
                }                    
            }

        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
