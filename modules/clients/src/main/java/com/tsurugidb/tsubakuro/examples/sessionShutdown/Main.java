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

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();
        options.addOption(Option.builder("f").argName("forceful").desc("forceful shutdown").build());
        options.addOption(Option.builder("n").argName("noShutdown").desc("do not request shutdown").build());
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
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
            System.exit(0);
        }

        try (var session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             ) {

            if (requestShutdown) {
                Thread.sleep(2000);
                session.shutdown(forceful ? ShutdownType.FORCEFUL : ShutdownType.GRACEFUL).get();
            }

            while (session.isAlive()) {
                Thread.sleep(2000);
                System.out.println("session is still alive");
            }

        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            System.out.println(e);
        }
        System.out.println("session shutdown has been completed");
    }
}
