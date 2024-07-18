package com.tsurugidb.tsubakuro.examples.keepAlive;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
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

    private static boolean keepAlive = true;
    private static long interval = 1000;
    private static long loopCount = 200;
    private static long timeout = 0;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("n").argName("noKeepAlive").desc("No keep alive.").build());
        options.addOption(Option.builder("l").argName("loopCount").hasArg().desc("Loop count.").build());
        options.addOption(Option.builder("t").argName("timeout").hasArg().desc("Timeout.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("n")) {
                keepAlive = false;
                System.err.println("no keep alive");
            }
            if (cmd.hasOption("l")) {
                loopCount = Integer.parseInt(cmd.getOptionValue("l"));
                System.err.println("loop count = " + loopCount);
            }
            if (cmd.hasOption("t")) {
                timeout = Integer.parseInt(cmd.getOptionValue("t"));
                System.err.println("timeout = " + timeout);
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }

        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .withKeepAlive(keepAlive)
             .create(10, TimeUnit.SECONDS)) {
            if (timeout > 0) {
                session.updateExpirationTime(timeout, TimeUnit.SECONDS);
            }
            int n = 0;
            while (true) {
                if (!session.isAlive()) {
                    System.out.println("detect tsurugidb session is not alive.");
                    break;
                }
                if (++n > loopCount) {
                    System.out.println("reach loop count (" + loopCount + ")");
                    break;
                }
                System.out.println("tsurugidb session is alive, N = " + n);
                Thread.sleep(interval);
            }
        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            System.out.println(e);
        }
        System.out.println("exiting.");
    }
}
