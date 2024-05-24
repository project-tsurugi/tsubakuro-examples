package com.tsurugidb.tsubakuro.examples.longQuery;

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

    private static boolean forceful = false;
    private static boolean requestShutdown = true;

    public static void main(String[] args) {
        try (var session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             SqlClient sqlClient = SqlClient.attach(session);
             var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {

            System.out.println("---- use " + url + " to connect tsurugidb----");
            try {
                System.out.println("---- ( start query 1 ) ----");
                long start = System.currentTimeMillis();
                Select.query(transaction, "SELECT * FROM TBL02,TBL02,TBL02,TBL02,TBL02,TBL01 WHERE TBL01.pk=-1");
                System.out.println("---- ( end query 1 in " + (System.currentTimeMillis() - start) + " milli sec. ) ----");
            } finally {
                System.out.println("---- ( start query 2 ) ----");
                long start = System.currentTimeMillis();
                Select.query(transaction, "SELECT * FROM TBL02,TBL02,TBL02,TBL02,TBL01 WHERE TBL01.pk=-1");
                System.out.println("---- ( end query 2 in " + (System.currentTimeMillis() - start) + " milli sec. ) ----");
            }
            transaction.commit().await();
        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            System.out.println("---- ( catch exception )----");
            System.out.println(e);
        }
    }
}
