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
import com.tsurugidb.tsubakuro.exception.CoreServiceException;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
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
    //    private static long timeout = 500;  // milliseconds
    private static long timeout = 30000;  // milliseconds

    private static boolean useFile = false;
    private static boolean useToken = false;
    private static int sleepTime = 0;
    private static boolean wrongCredential = false;
    private static boolean noAuth = false;
    private static boolean nullPassword = false;
    private static boolean updateAuthentication = false;
    private static boolean updateAuthenticationDiffUser = false;
    private static boolean longUsernameAnsPassword = false;
    private static String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJyZWZyZXNoIiwiYXVkIjoiaGFyaW5va2kiLCJ0c3VydWdpL2F1dGgvbmFtZSI6InRzdXJ1Z2kiLCJpc3MiOiJoYXJpbm9raSIsImV4cCI6MTc1NTY3OTg3NiwiaWF0IjoxNzU1Njc2Mjc2LCJqdGkiOiI5ODg0ZGY5YS05N2YxLTQwYmEtYjY1MC02MDM3NjUzOTRmOTkifQ.LmoF-V3oA2_c1ne38zgdrHz8uYElh6sbfZNRu8yyHwM67B4T4aMQ6SiaGgLrGDGivfqYQmiub4UnRg1MeNazeZOwp1PQAnLh68OaL8csbuoeKWjrWimRglHmGv7L-D4eY9md1Fv_YjEiTWXl5UiIwQ4NkvZ22rvDiu6vNIGJ8xdDz2WQLIVSCdb1eOYxcrX9QclKw6K3JS5fAWxhhX-b2aUiwH3jqdHDT4IlpXxFLUJGUQ1Zw6L92gcFY65zOcD_moQywFC3GAQEstBBjVWQLyn8eEqbF5W73908VVfXnAlXz6d57dR0Ic67_T22ZVEHUyXLZSqQygWbR2v7hZng2Q";
    private static String wrongToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJyZWZyZXNoIiwiYXVkIjoiYXV0aGVudGljYXRpb24tbWFuYWdlciIsInRzdXJ1Z2kvYXV0aC9uYW1lIjoiaG9yaWthd2EiLCJpc3MiOiJhdXRoZW50aWNhdGlvbi1tYW5hZ2VyIiwiZXhwIjoxNzQ5NzMyNDU5LCJpYXQiOjE3NDk2NDYwNTksImp0aSI6ImFmM2RlMjk1LTE2NzktNDZkYy04YmM4LTU2MWZmNzhkZTFkYyJ9.kflvbX9DEx_Rw29VWkMuyG8Vyuc5Do69jQ-BAnYKhkOFW94Jjpw7y0mbDKe5RfQc2VZ2jG5qikESTPt-U0EEupH9ns29j845iJPmIsTP_sPqN65keZEu3bu0XHGlQMZZ1cZ1wbekT8qVJQoteBx18a26YeetEpZni1i8ng9bMMF0eAMosWKMoHpa-29BW1avshjYPi8tcCXNql9-Vdyc5HGbTBq6TnFZNxnFiObNo1LpYLdArv-GsLK6Yswx4uosFVpn6TIQDaVbuBnC3_t53QPsCnweRc-4BwtFfi4DGQFXtiNC1kSan9scmwGRYkohOniFBPVlQSKmNBKHIbFbkQ";
    private static String longUsername ="12345678901234567890123456789012345678901234567890123456789012";
    private static String longPassword ="12345678901234567890123456789012345678901234567890123456789012";

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("f").argName("use file").desc("use file.").build());
        options.addOption(Option.builder("t").argName("use token").desc("use token.").build());
        options.addOption(Option.builder("w").argName("wrong password").desc("wrong password.").build());
        options.addOption(Option.builder("s").argName("sleep").hasArg().desc("sleep a while.").build());
        options.addOption(Option.builder("n").argName("no auth").desc("no auth.").build());
        options.addOption(Option.builder("p").argName("password is null").desc("password is null.").build());
        options.addOption(Option.builder("u").argName("update credential").desc("update credential.").build());
        options.addOption(Option.builder("d").argName("update credential diff user").desc("update credential diff user.").build());
        options.addOption(Option.builder("l").argName("long username and password").desc("long username and password.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("f")) {
                useFile = true;
                System.err.println("case of use file");
            }
            if (cmd.hasOption("t")) {
                useToken = true;
                System.err.println("case of use token");
            }
            if (cmd.hasOption("w")) {
                wrongCredential = true;
                System.err.println("case of wrong credential");
            }
            if (cmd.hasOption("s")) {
                sleepTime = Integer.parseInt(cmd.getOptionValue("s"));
                System.err.println("sleep for " + sleepTime + " seconds");
            }
            if (cmd.hasOption("n")) {
                noAuth = true;
                System.err.println("case of no auth");
            }
            if (cmd.hasOption("p")) {
                nullPassword = true;
                System.err.println("case of null password");
            }
            if (cmd.hasOption("u")) {
                updateAuthentication = true;
                System.err.println("do update credential");
            }
            if (cmd.hasOption("d")) {
                updateAuthenticationDiffUser = true;
                System.err.println("do update credential with different user");
            }
            if (cmd.hasOption("l")) {
                longUsernameAnsPassword = true;
                System.err.println("long username and password");
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }

        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(longUsernameAnsPassword ? new UsernamePasswordCredential(longUsername, longPassword) :
                             noAuth ? NullCredential.INSTANCE :
                             useFile ? FileCredential.load(FileCredential.DEFAULT_CREDENTIAL_PATH.get()) :
                             useToken ?
                             new RememberMeCredential(wrongCredential ? wrongToken : token) :
                             new UsernamePasswordCredential("tsurugi", nullPassword ? null : wrongCredential ? "drowssap" : "password"))
             .create(timeout, TimeUnit.MILLISECONDS);
             SqlClient authenticationClient = SqlClient.attach(session); ) {

            var uopt = session.getUserName().await();
            if (uopt.isPresent()) {
                System.out.println("login with user '" + uopt.get() + "'");
                try {
                    System.out.println("AuthenticationExpirationTime '" + session.getAuthenticationExpirationTime().await().toString() + "'");
                } catch (ServerException e) {
                    System.out.println("getAuthenticationExpirationTime() result in " + e);
                }
            } else {
                System.out.println("login user name is empty, probably connecting to servers with authentication disabled");
            }

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime * 1000);
                    System.out.println("AuthenticationExpirationTime '" + session.getAuthenticationExpirationTime().await().toString() + "'");
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }

            if (updateAuthentication) {
                session.updateAuthentication(new UsernamePasswordCredential("tsurugi", "password")).await();
                var uopt2 = session.getUserName().await();
                if (uopt2.isPresent()) {
                    System.out.println("user name after updateAuthentication is '" + uopt2.get() + "'");
                }
            }
            if (updateAuthenticationDiffUser) {
                session.updateAuthentication(new UsernamePasswordCredential("dummy", "password")).await();
                var uopt3 = session.getUserName().await();
                if (uopt3.isPresent()) {
                    System.out.println("user name after updateAuthentication is '" + uopt3.get() + "'");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
