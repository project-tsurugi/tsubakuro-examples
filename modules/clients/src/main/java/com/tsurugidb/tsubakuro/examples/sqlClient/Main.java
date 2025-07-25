package com.tsurugidb.tsubakuro.examples.sqlClient;

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
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.util.FutureResponse;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");
    private static long timeout = 500;  // milliseconds

    private static String sql ="";
    private static boolean usePreparedStatement = false;
    private static boolean query = true;
    private static boolean display = false;
    private static boolean interrupting = false;
    private static boolean useToken = false;
    private static boolean waitForWhile = false;
    private static String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJyZWZyZXNoIiwiYXVkIjoiYXV0aGVudGljYXRpb24tbWFuYWdlciIsInRzdXJ1Z2kvYXV0aC9uYW1lIjoidXNlciIsImlzcyI6ImF1dGhlbnRpY2F0aW9uLW1hbmFnZXIiLCJleHAiOjE3NTA4MzE4MTksImlhdCI6MTc1MDc0NTQxOSwianRpIjoiZGQ4MjU4OGMtMWFiNC00MTNjLThhNGYtZDkxYmYyOGU5OTRlIn0.ifRlaNF742JDkWOx1DlijeE-CazKCHXikxxR2BUv6V1YobquEvGo6Wei0cbTCo45NABKXDd8Zrf0S7u8ARuMla4aQZksXgq0a96YfYDRd-c7VYPR8JxBf7mBF3Jtu3NBP6bhzDU-b0kytp9kURAnlJ2GFK8Z-3EwiDaHjWTnA3Y9W6PLPLfaO3f1t2XgNyU3MK0zXz_6Qyy_Yo9fyAhiPCxQPtyXRRwJqw43A77n3HLDb0geI4-5f10MJ7mlZUDvTW6CXTtZyizyyREAqbcpV_OghKKJF2-tSWMud9aBIejxA3f5RSJhziX7vwXc72WQqiZYJWbzpNL2RQOhHhRbYg";

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("s").argName("sql").hasArg().desc("Sql text.").build());
        options.addOption(Option.builder("q").argName("query").desc("Query mode.").build());
        options.addOption(Option.builder("p").argName("prepared statement").desc("Use prepared statement.").build());
        options.addOption(Option.builder("d").argName("display result").desc("Display query result.").build());
        options.addOption(Option.builder("i").argName("interrupting the reading").desc("interrupting the reading.").build());
        options.addOption(Option.builder("t").argName("use token").desc("use token.").build());
        options.addOption(Option.builder("w").argName("wait").desc("wait.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("s")) {
                sql = cmd.getOptionValue("s");
            }
            if (cmd.hasOption("p")) {
                usePreparedStatement = true;
                System.err.println("prepared statement");
            }
            if (cmd.hasOption("q")) {
                query = true;
                System.err.println("query mode");
                if (cmd.hasOption("d")) {
                    display = true;
                    System.err.println("display query result");
                }
                if (cmd.hasOption("i")) {
                    interrupting = true;
                    System.err.println("interrupt the ResultSet reading");
                }
            }
            if (cmd.hasOption("t")) {
                useToken = true;
                System.err.println("use token");
            }
            if (cmd.hasOption("w")) {
                waitForWhile = true;
                System.err.println("wait for a while");
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }

        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(useToken ?
                             new RememberMeCredential(token) :
                             new UsernamePasswordCredential("user", "pass"))
             .create(timeout, TimeUnit.MILLISECONDS);
             SqlClient sqlClient = SqlClient.attach(session);
             var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {

            PreparedStatement preparedStatement = null;

            if (usePreparedStatement) {
                preparedStatement = sqlClient.prepare(sql).await();
            }

	    if (waitForWhile) {
		System.out.println("pause for a while");
		try {
		    Thread.sleep(150000);
		} catch(InterruptedException e){
		    e.printStackTrace();
		} 
	    }		
	    if (query) {
                FutureResponse<ResultSet> frs;
                if (usePreparedStatement) {
                    frs = transaction.executeQuery(preparedStatement);
                } else {
                    frs = transaction.executeQuery(sql);
                }
                var rs = frs.await();
                if (display) {
                    printResultset(rs);
                }
                System.out.println("close the ResultSet begin");
                rs.close();
                System.out.println("close the ResultSet end");
            } else {
                if (usePreparedStatement) {
                    transaction.executeStatement(preparedStatement).await();
                } else {
                    transaction.executeStatement(sql).await();
                }
            }
        } catch (Exception e) {
            System.err.println("catch exception at 115");
            System.err.println(e);
        }
    }

    static void printResultset(ResultSet resultSet) throws InterruptedException, IOException, ServerException {
        int count = 1;

        while (resultSet.nextRow()) {
            System.out.println("---- ( count: " + count + " )----");
            count++;
            int columnIndex = 0;
            var metadata = resultSet.getMetadata().getColumns();
            while (resultSet.nextColumn()) {
                if (!resultSet.isNull()) {
                    switch (metadata.get(columnIndex).getAtomType()) {
                    case INT4:
                        System.out.println(resultSet.fetchInt4Value());
                        break;
                    case INT8:
                        System.out.println(resultSet.fetchInt8Value());
                        break;
                    case FLOAT4:
                        System.out.println(resultSet.fetchFloat4Value());
                        break;
                    case FLOAT8:
                        System.out.println(resultSet.fetchFloat8Value());
                        break;
                    case CHARACTER:
                        System.out.println(resultSet.fetchCharacterValue());
                        break;
                    default:
                        throw new IOException("the column type is invalid");
                    }
                } else {
                    System.out.println("the column is NULL");
                }
                columnIndex++;
            }
            if (interrupting) {
                break;
            }
        }
    }
}
