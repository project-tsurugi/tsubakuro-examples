package com.tsurugidb.tsubakuro.examples;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

//import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
//import com.tsurugidb.tsubakuro.common.Session;
//import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tateyama or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    private static boolean selectOnly = false;
    private static boolean textInsert = false;
    private static int loopCount = 1;
    private static int selectCount = 1;
    private static int threadCount = 1;
    private static int sleepSeconds = 0;
    private static int sessionCount = 1;
    private static boolean suppressDisplay = false;
    private static long timeout = 5000;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("s").argName("select").desc("Select only mode.").build());
        options.addOption(Option.builder("t").argName("text_insert").desc("Do Insert by text SQL.").build());
        options.addOption(Option.builder("c").argName("concurrency").hasArg().desc("Specify the number of threads conducting the select operation.").build());
        options.addOption(Option.builder("n").argName("number").hasArg().desc("Specify the execution count of the select operation.").build());
        options.addOption(Option.builder("l").argName("loops").hasArg().desc("Specify the number of loop count of the thread invocation.").build());
        options.addOption(Option.builder("p").argName("pause").hasArg().desc("Sleep specified time before session close.").build());
        options.addOption(Option.builder("d").argName("display").desc("No result printout.").build());
        options.addOption(Option.builder("m").argName("session number").hasArg().desc("The number of session.").build());
        options.addOption(Option.builder("o").argName("timeout").hasArg().desc("timeout value.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("s")) {
                selectOnly = true;
                System.err.println("select only");
            }
            if (cmd.hasOption("t")) {
                textInsert = true;
                System.err.println("text insert");
            }
            if (cmd.hasOption("n")) {
                selectCount = Integer.parseInt(cmd.getOptionValue("n"));
                System.err.println("select count = " + selectCount);
            }
            if (cmd.hasOption("c")) {
                threadCount = Integer.parseInt(cmd.getOptionValue("c"));
                System.err.println("thread count = " + threadCount);
            }
            if (cmd.hasOption("l")) {
                loopCount = Integer.parseInt(cmd.getOptionValue("l"));
                System.err.println("loop count = " + loopCount);
            }
            if (cmd.hasOption("p")) {
                sleepSeconds = Integer.parseInt(cmd.getOptionValue("p"));
                System.err.println("sleep before session close for " + sleepSeconds + " seconds");
            }
            if (cmd.hasOption("d")) {
                suppressDisplay = true;
                System.err.println("No result display");
            }
            if (cmd.hasOption("m")) {
                sessionCount = Integer.parseInt(cmd.getOptionValue("m"));
                System.err.println("Session count = " + sessionCount);
            }
            if (cmd.hasOption("o")) {
                timeout = Long.parseLong(cmd.getOptionValue("o")) * 1000;
                System.err.println("tmeout = " + timeout);
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }

        for (int i = 0; i < sessionCount; i++ ) {
            if (sessionCount > 1) {
                System.out.println("======== session no. " + (i + 1) + " ========");
            }
            try {
                if (!selectOnly) {
                    var insert = new Insert(url);
                    if (!textInsert) {
                        insert.prepareAndInsert();
                    } else {
                        insert.insertByText();
                    }
                }
                var select = new Select(url, loopCount, selectCount, threadCount, suppressDisplay, sleepSeconds, timeout);
                select.prepareAndSelect();
            } catch (IOException | ServerException | InterruptedException e) {
                System.out.println(e);
            }
        }
    }
}
