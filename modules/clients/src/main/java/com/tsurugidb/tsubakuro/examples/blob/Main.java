package com.tsurugidb.tsubakuro.examples.blob;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
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

    private static boolean query = false;
    private static boolean createFile = true;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("q").argName("query").desc("Query mode.").build());
        options.addOption(Option.builder("n").argName("noCreateFile").desc("Does not create file.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("q")) {
                query = true;
            }
            if (cmd.hasOption("n")) {
                createFile = false;
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }

        System.err.println(query ? "query mode" : "statement mode");
        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(timeout, TimeUnit.MILLISECONDS);
             SqlClient sqlClient = SqlClient.attach(session);
             var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {

            PreparedStatement preparedStatement = null;
            if (query) {
                preparedStatement = sqlClient.prepare("SELECT FROM testtable WHERE key = 1").await();

                var resultSet = transaction.executeQuery(preparedStatement).await();
                var metadata = resultSet.getMetadata().getColumns();
                while (resultSet.nextRow()) {
                    int columnIndex = 0;
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
                            case BLOB:
                                var is = transaction.openInputStream(resultSet.fetchBlob()).await();
                                System.out.print("column name = " + metadata.get(columnIndex).getName() + ": content = '");
                                while (true) {
                                    var i = is.read();
                                    if (i < 0) {
                                        break;
                                    }
                                    char c = (char) i;
                                    if (c != '\n') {
                                        System.out.print(c);
                                    }
                                }
                                System.out.println("'");
                                break;
                            case CLOB:
                                var lr = new LineNumberReader(transaction.openReader(resultSet.fetchClob()).await());
                                System.out.print("column name = " + metadata.get(columnIndex).getName() + ": content = '");
                                String s;
                                while ((s = lr.readLine()) != null) {
                                    System.out.print(s);
                                }
                                System.out.println("'");
                                break;
                            default:
                                throw new IOException("the column type is invalid");
                            }
                        } else {
                            System.out.println("the column is NULL");
                        }
                        columnIndex++;
                    }
                }
                resultSet.close();
            } else {
                String[] fileNameArray = {"/tmp/testChannelBlob1Up.data", "/tmp/testChannelBlob2Up.data", "/tmp/testChannelClobUp.data"};
                byte[] data = new byte[] { 0x01, 0x02, 0x03 };
                for (var n: fileNameArray) {
                    var path = Paths.get(n);
                    if (Files.notExists(path) && createFile) {
                        Files.write(Paths.get(n), data);
                    }
                }
                preparedStatement = sqlClient.prepare("INSERT INTO testTable (key, blob_column_1, blob_column_2, clob_column) VALUES (1, :blob1, :blob2, :clob)",
                                                      Placeholders.of("blob1", SqlCommon.Blob.class),
                                                      Placeholders.of("blob2", SqlCommon.Blob.class),
                                                      Placeholders.of("clob", SqlCommon.Clob.class)).await();

                transaction.executeStatement(preparedStatement,
                                             Parameters.blobOf("blob1", Paths.get(fileNameArray[0])),
                                             Parameters.blobOf("blob2", Paths.get(fileNameArray[1])),
                                             Parameters.clobOf("clob", Paths.get(fileNameArray[2]))).await();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
    }
}
