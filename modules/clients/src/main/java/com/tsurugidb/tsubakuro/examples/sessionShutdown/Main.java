package com.tsurugidb.tsubakuro.examples.sessionShutdown;

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
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    private static boolean forceful = false;
    private static boolean requestShutdown = true;
    private static boolean execSql = false;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();
        options.addOption(Option.builder("f").argName("forceful").desc("forceful shutdown").build());
        options.addOption(Option.builder("n").argName("noShutdown").desc("do not request shutdown").build());
        options.addOption(Option.builder("s").argName("execSQL").desc("execute sql").build());
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("f")) {
                forceful = true;
                System.err.println("forceful shutdown");
            }
            if (cmd.hasOption("n")) {
                requestShutdown = false;
                System.err.println("no shutdown request");
            }
            if (cmd.hasOption("s")) {
                execSql = true;
                System.err.println("execute sql");
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
            System.exit(0);
        }

        try (var session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             SqlClient sqlClient = SqlClient.attach(session);
             ) {

            if (execSql) {
                var transaction = sqlClient.createTransaction().get();
                var future = transaction.executeQuery("SELECT * FROM TBL02,TBL02,TBL02,TBL02,TBL02,TBL01 WHERE TBL01.pk=-1");
                Thread.sleep(2000);
                shutdown(session);
                try {
                    transaction.commit().get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Thread.sleep(2000);
                shutdown(session);
            }

        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
        System.out.println("session shutdown has been completed");
    }

    public static void shutdown(Session session) throws IOException, ServerException, InterruptedException, TimeoutException {
        if (requestShutdown) {
            long start = System.currentTimeMillis();
            try {
                System.out.println("---- request shutdown ----");
                session.shutdown(forceful ? ShutdownType.FORCEFUL : ShutdownType.GRACEFUL).get();
            } finally {
                System.out.println("---- finish shutdown, which takes " + (System.currentTimeMillis() - start) + " milli sec ----");
            }
            Thread.sleep(100);
        }
        boolean detectAlive = false;
        while (session.isAlive()) {
            System.out.println("session is still alive");
            Thread.sleep(100);
            detectAlive = true;
        }
        if (detectAlive) {
            System.out.println("session status has changed from alive to shutdown");
        }
    }
}
