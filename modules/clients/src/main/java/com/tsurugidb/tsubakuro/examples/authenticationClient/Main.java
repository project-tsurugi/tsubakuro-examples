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
    private static String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJyZWZyZXNoIiwiYXVkIjoiaGFyaW5va2kiLCJ0c3VydWdpL2F1dGgvbmFtZSI6InRzdXJ1Z2kiLCJpc3MiOiJoYXJpbm9raSIsImV4cCI6MTc1NDQ0ODI4MCwiaWF0IjoxNzU0MzYxODgwLCJqdGkiOiJiMGU1OTNjNS03Yjg0LTQxM2MtYjk0Yi1lYWIzMDkzYzhjZWQifQ.eXuNZj8pgc20vD9xEj0GG9fgu8jFE5weFtnkjs-T45_Hyc9RZgnPbG9iGehDOFmj6vbbeCHs8JKzeFKMikUerJll41wag7yNWheQgpsCpHVe-8Kjgsb8wug_HsL30spMGYjXErxN54nMctLaTDnyvzA12KnUTFcbOMHiNf9Bq1WbzDBaPcEe9WiEgTphwN2voQh8QgfdzQXyI0mRlDqV012xy5xRDcFrDWXsYp7QRXR1ZRWqloZOp_uTf6Q9XtpKHY8-4dHV4OEIh8HmkcrqbP-N8swCFWLfxEl67STLAhHAp_oCAcdgqVEzb7ZGk-1S0G1UZ46VHjdIVnPfSMbaWw";

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
                             new UsernamePasswordCredential("tsurugi", wrongPassword ? "drowssap" : "password"))
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
            e.printStackTrace();
        }
    }
}
