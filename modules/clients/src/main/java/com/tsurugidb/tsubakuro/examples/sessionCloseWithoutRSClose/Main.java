package com.tsurugidb.tsubakuro.examples.sessionCloseWithoutRSClose;

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
// where n is a value of about 20 to 25
public final class Main {
    private static long timeout = 5000;
    private static boolean withoutFutureResponseGet = false;

    private Main(String[] args) {
    }
    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    private static boolean forceful = false;
    private static boolean requestShutdown = false;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();
        options.addOption(Option.builder("f").argName("future").desc("without FutureResponse.get()").build());
        options.addOption(Option.builder("s").argName("shutdown").desc("do shutdown before Session close").build());
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("f")) {
                withoutFutureResponseGet = true;
                System.out.println("==== withoutFutureResponseGet mode ====");
            }
            if (cmd.hasOption("s")) {
                requestShutdown = true;
                System.out.println("==== do shutdown before Session close ====");
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
            System.exit(0);
        }

        try (var session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             SqlClient sqlClient = SqlClient.attach(session)) {

            System.out.println("---- program begin with using " + url + " to connect tsurugidb ----");
            var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS);
            System.out.println("---- ( start query ) ----");
            if (!withoutFutureResponseGet) {
                var resultSet = transaction.executeQuery("SELECT * FROM TBL01").get(timeout, TimeUnit.MILLISECONDS);
            } else {
                var future = transaction.executeQuery("SELECT * FROM TBL01");
            }
            if (requestShutdown) {
                System.out.println("---- ( request shutdown ) ----");
                session.shutdown(ShutdownType.FORCEFUL).get();
            }
            System.out.println("---- ( going to close Sessoin without ResultSet close ) ----");
        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            System.err.println(e);
        } finally {
            System.out.println("---- program end ----");
        }
    }
}
