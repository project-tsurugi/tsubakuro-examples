package com.tsurugidb.tsubakuro.examples.authenticationClient;

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
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
// import com.tsurugidb.tsubakuro.util.FutureResponse;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");
    private static long timeout = 500;  // milliseconds

    private static boolean useToken = false;
    private static int sleepTime = 0;
    private static boolean wrongPassword = false;
    private static String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJyZWZyZXNoIiwiYXVkIjoiYXV0aGVudGljYXRpb24tbWFuYWdlciIsInRzdXJ1Z2kvYXV0aC9uYW1lIjoidXNlciIsImlzcyI6ImF1dGhlbnRpY2F0aW9uLW1hbmFnZXIiLCJleHAiOjE3NTA4MzE4MTksImlhdCI6MTc1MDc0NTQxOSwianRpIjoiZGQ4MjU4OGMtMWFiNC00MTNjLThhNGYtZDkxYmYyOGU5OTRlIn0.ifRlaNF742JDkWOx1DlijeE-CazKCHXikxxR2BUv6V1YobquEvGo6Wei0cbTCo45NABKXDd8Zrf0S7u8ARuMla4aQZksXgq0a96YfYDRd-c7VYPR8JxBf7mBF3Jtu3NBP6bhzDU-b0kytp9kURAnlJ2GFK8Z-3EwiDaHjWTnA3Y9W6PLPLfaO3f1t2XgNyU3MK0zXz_6Qyy_Yo9fyAhiPCxQPtyXRRwJqw43A77n3HLDb0geI4-5f10MJ7mlZUDvTW6CXTtZyizyyREAqbcpV_OghKKJF2-tSWMud9aBIejxA3f5RSJhziX7vwXc72WQqiZYJWbzpNL2RQOhHhRbYg";

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("t").argName("use token").desc("use token.").build());
        options.addOption(Option.builder("w").argName("wrong password").desc("wrong password.").build());
        options.addOption(Option.builder("s").argName("sleep").hasArg().desc("sleep a while.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("t")) {
                useToken = true;
                System.err.println("use token");
            }
            if (cmd.hasOption("w")) {
                wrongPassword = true;
                System.err.println("wrong password");
            }
            if (cmd.hasOption("s")) {
                sleepTime = Integer.parseInt(cmd.getOptionValue("s"));
                System.err.println("sleep for " + sleepTime + " seconds");
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }

        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(useToken ?
                             new RememberMeCredential(token) :
                             new UsernamePasswordCredential("user", wrongPassword ? "ssap" : "pass"))
             .create(timeout, TimeUnit.MILLISECONDS);
             SqlClient authenticationClient = SqlClient.attach(session); ) {

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime * 1000);
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("catch exception at 115");
            System.err.println(e);
        }
    }
}
