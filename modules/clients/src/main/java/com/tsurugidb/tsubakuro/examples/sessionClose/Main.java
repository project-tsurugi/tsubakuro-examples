package com.tsurugidb.tsubakuro.examples.sessionClose;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.text.MessageFormat;
    
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.common.ShutdownType;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;

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
    private static boolean threadSleep = false;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();
        options.addOption(Option.builder("g").argName("doGet").desc("do get() operation").build());
        options.addOption(Option.builder("s").argName("sleep").desc("sleep for a while").build());
        options.addOption(Option.builder("h").argName("huge").desc("huge result set").build());
        CommandLineParser parser = new DefaultParser();

        String sql = "SELECT * FROM TBL02,TBL02,TBL02,TBL02,TBL02,TBL01 WHERE TBL01.pk=-1";
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("g")) {
                doGet = true;
                System.err.println("do get() operation");
            }
            if (cmd.hasOption("s")) {
                threadSleep = true;
                System.err.println("sleep for a while");
            }
            if (cmd.hasOption("h")) {
                sql = "SELECT * FROM TBL02,TBL02,TBL01";
                System.err.println("sql = " + sql);
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
            var futureResultSet = transaction.executeQuery(sql);
            if (doGet) {
                futureResultSet.get();
            }
            if (threadSleep) {
                Thread.sleep(10000);
            }

            System.out.println("going to do session.close");
            session.close();

            if (!doGet) {
                futureResultSet.get();
            }
        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            System.err.println(e);
        } finally {
            System.out.println("---- program execution finished in " + (System.currentTimeMillis() - start) + " milli seconds ----");
        }
    }
}
