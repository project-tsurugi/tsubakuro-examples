package com.tsurugidb.tsubakuro.examples.sqlClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.util.FutureResponse;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");
    private static long timeout = 500;  // milliseconds

    private static String sql ="";
    private static boolean usePreparedStatement = false;
    private static boolean query = true;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("s").argName("sql").hasArg().desc("Sql text.").build());
        options.addOption(Option.builder("q").argName("query").desc("Query mode.").build());
        options.addOption(Option.builder("p").argName("prepared statement").desc("Use prepared statement.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("s")) {
                sql = cmd.getOptionValue("s");
            }
            if (cmd.hasOption("p")) {
                usePreparedStatement = true;
                System.err.println("prepared statement");
            }
            if (cmd.hasOption("q")) {
                query = true;
                System.err.println("query mode");
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }

        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(timeout, TimeUnit.MILLISECONDS);
             SqlClient sqlClient = SqlClient.attach(session);
             var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {

            PreparedStatement preparedStatement = null;

            if (usePreparedStatement) {
                preparedStatement = sqlClient.prepare(sql).await();
            }

            if (query) {
                FutureResponse<ResultSet> rs;
                if (usePreparedStatement) {
                    rs = transaction.executeQuery(preparedStatement);
                } else {
                    rs = transaction.executeQuery(sql);
                }
                rs.await();
                rs.close();
            } else {
                if (usePreparedStatement) {
                    transaction.executeStatement(preparedStatement).await();
                } else {
                    transaction.executeStatement(sql).await();
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
