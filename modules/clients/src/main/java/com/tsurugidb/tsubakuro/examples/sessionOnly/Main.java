package com.tsurugidb.tsubakuro.examples.sessionOnly;

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
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
// import com.tsurugidb.tsubakuro.sql.SqlClient;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    private static final int INNER_COUNT = 1000;
    private static int loopCount = 1;
    private static long timeout = 5000;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();
        options.addOption(Option.builder("l").argName("loops").hasArg().desc("Specify the number of loop count of the thread invocation.").build());
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("l")) {
                loopCount = Integer.parseInt(cmd.getOptionValue("l"));
                System.err.println("loop count = " + loopCount);
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
            System.exit(0);
        }

        var start = System.currentTimeMillis();
        for (int i = 0; i < loopCount; i++) {
            for (int j = 0; j < INNER_COUNT; j++) {
                try (var session = SessionBuilder.connect(url)
                     .withCredential(new UsernamePasswordCredential("user", "pass"))
                     .create(10, TimeUnit.SECONDS);
                     ) {
                } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
                    System.out.println(e);
                }
            }
        }
        var elapsed = System.currentTimeMillis() - start;
        System.out.println(MessageFormat.format("session only benchmark result: loopcount = {0,number,integer}, elapsed = {1,number,integer} milliseconds.", loopCount * INNER_COUNT, elapsed));
    }
}
