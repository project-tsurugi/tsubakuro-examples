package com.tsurugidb.tsubakuro.examples.sessionClose;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.tsurugidb.tsubakuro.util.FutureResponse;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.common.ShutdownType;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.ResultSet;

// Prior to executing this program, the following create table and insert to table must be executed.
//    DROP TABLE IF EXISTS TBL01;
//    CREATE TABLE TBL01(pk INT PRIMARY KEY);
//    INSERT INTO TBL01 VALUES(1);
//    INSERT INTO TBL01 VALUES(2);
//    ......
//    INSERT INTO TBL01 VALUES(n);
//    DROP TABLE IF EXISTS TBL02;
//    CREATE TABLE TBL02(pk INT PRIMARY KEY);
//    INSERT INTO TBL02 VALUES(1);
//    INSERT INTO TBL02 VALUES(2);
//    ......
//    INSERT INTO TBL02 VALUES(n);
// where n is a value of about 20 to 25
public final class Main {
    private static long timeout = 5000;
    private Main(String[] args) {
    }
    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    private static boolean doGet = false;
    private static boolean execSql = false;
    private static boolean transactionClose = false;
    private static int sleepTime = 2000;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();
        options.addOption(Option.builder("g").argName("doGet").desc("do get() operation").build());
        options.addOption(Option.builder("s").argName("execSql").desc("execute SQL").build());
        options.addOption(Option.builder("h").argName("huge").desc("huge result set").build());
        options.addOption(Option.builder("t").argName("transactionClose").desc("clost transaction").build());
        CommandLineParser parser = new DefaultParser();

        String sql = "SELECT * FROM TBL02,TBL02,TBL02,TBL02,TBL02,TBL01 WHERE TBL01.pk=-1";
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("g")) {
                doGet = true;
                System.err.println("do get() operation");
            }
            if (cmd.hasOption("s")) {
                execSql = true;
                System.err.println("execute SQL");
            }
            if (cmd.hasOption("h")) {
                sql = "SELECT * FROM TBL02,TBL02,TBL01";
                System.err.println("sql = " + sql);
            }
            if (cmd.hasOption("t")) {
                transactionClose = true;
                System.err.println("do transaction.close() before session.close()");
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
            System.exit(0);
        }

        var start = System.currentTimeMillis();
        try {
            var session = SessionBuilder.connect(url)
                .withCredential(new UsernamePasswordCredential("user", "pass"))
                .create(10, TimeUnit.SECONDS);
            SqlClient sqlClient = SqlClient.attach(session);
            var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS);
            FutureResponse<ResultSet> futureResultSet = null;
            if (execSql) {
                futureResultSet = transaction.executeQuery(sql);
                if (doGet) {
                    futureResultSet.get();
                }
            }
            Thread.sleep(sleepTime);

            if (transactionClose) {
                System.out.println(LocalDateTime.now() + " going to transaction.close()");
                transaction.close();
                System.out.println(LocalDateTime.now() + " return from transaction.close()");
                Thread.sleep(sleepTime);
            }

            System.out.println(LocalDateTime.now() + " going to session.close()");
            session.close();
            System.out.println(LocalDateTime.now() + " return from session.close()");

            if (!doGet && futureResultSet != null) {
                futureResultSet.get();
            }
        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            System.err.println(e);
        } finally {
            System.out.println("---- program execution finished in " + (System.currentTimeMillis() - start - sleepTime) + " milli seconds ----");
        }
    }
}
