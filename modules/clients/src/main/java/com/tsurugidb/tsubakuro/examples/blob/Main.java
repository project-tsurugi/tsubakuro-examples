package com.tsurugidb.tsubakuro.examples.blob;

import java.io.FileInputStream;
import java.io.InputStreamReader;
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
import com.tsurugidb.tsubakuro.common.BlobTransferMedium;
import com.tsurugidb.tsubakuro.common.BlobTransferType;
import com.tsurugidb.tsubakuro.common.LargeObjectClient;
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
    private static boolean privileged = false;
    private static boolean createTable = false;
    private static boolean useLargeObjectClient = false;
    private static boolean useStream = false;

    private final static String TABLE = "testTable";
    private final static String KEY = "1";

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("q").argName("query").desc("Query mode.").build());
        options.addOption(Option.builder("n").argName("noCreateFile").desc("Does not create file.").build());
        options.addOption(Option.builder("p").argName("privileged").desc("Use privileged blob transfer.").build());
        options.addOption(Option.builder("i").argName("initialize (createTable)").desc("Create table first.").build());
        options.addOption(Option.builder("c").argName("use LargeObjectClient").desc("Use LargeObjectClient.").build());
        options.addOption(Option.builder("s").argName("use LargeObjectClient stream").desc("Use LargeObjectClient stream.").build());

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
            if (cmd.hasOption("p")) {
                privileged = true;
            }
            if (cmd.hasOption("i")) {
                createTable = true;
            }
            if (cmd.hasOption("c")) {
                useLargeObjectClient = true;
            }
            if (cmd.hasOption("s")) {
                useStream = true;
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
            return;
        }

        // create table
        if (createTable) {
            try (
                 Session session = SessionBuilder.connect(url)
                 .withCredential(new UsernamePasswordCredential("user", "pass"))
                 .withBlobTransfer(BlobTransferType.DOES_NOT_USE)
                 .create(timeout, TimeUnit.MILLISECONDS);
                 SqlClient sqlClient = SqlClient.attach(session);
                 var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {
                transaction.executeStatement("create table " + TABLE + " ( key int primary key, blob_column_1 blob, blob_column_2 blob, clob_column clob )");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e);
                return;
            }
        }

        // execute DML
        System.err.println(query ? "== select ==" : "== insert ==");
        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .withBlobTransfer(privileged ? BlobTransferType.PRIVILEGED : BlobTransferType.RELAY)
             .create(timeout, TimeUnit.MILLISECONDS);
             SqlClient sqlClient = SqlClient.attach(session);
             var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {

            var blobTransferMedium = session.getBlobTransferMedium();
            System.out.println(blobTransferMedium.getBlobTransferType());
            System.out.println(blobTransferMedium.getParameters());

            PreparedStatement preparedStatement = null;
            if (query) {
                preparedStatement = sqlClient.prepare("SELECT * FROM " + TABLE + " WHERE key = " + KEY).await();

                var resultSet = transaction.executeQuery(preparedStatement).await();
                var metadata = resultSet.getMetadata().getColumns();
                while (resultSet.nextRow()) {
                    int columnIndex = 0;
                    while (resultSet.nextColumn()) {
                        if (!resultSet.isNull()) {
                            switch (metadata.get(columnIndex).getAtomType()) {
                            case INT4:
                                System.out.println("column name = " + metadata.get(columnIndex).getName() + ": content = " + resultSet.fetchInt4Value());
                                break;
                            case INT8:
                                System.out.println("column name = " + metadata.get(columnIndex).getName() + ": content = " + resultSet.fetchInt8Value());
                                break;
                            case FLOAT4:
                                System.out.println("column name = " + metadata.get(columnIndex).getName() + ": content = " + resultSet.fetchFloat4Value());
                                break;
                            case FLOAT8:
                                System.out.println("column name = " + metadata.get(columnIndex).getName() + ": content = " + resultSet.fetchFloat8Value());
                                break;
                            case CHARACTER:
                                System.out.println("column name = " + metadata.get(columnIndex).getName() + ": content = '" + resultSet.fetchCharacterValue() + "'");
                                break;
                            case BLOB:
                                var blobColumn = resultSet.fetchBlob();
                                var is = transaction.openInputStream(blobColumn).await();
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
                                var clobColumn = resultSet.fetchClob();
                                var lr = new LineNumberReader(transaction.openReader(clobColumn).await());
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
                    System.out.println("====");
                }
                resultSet.close();
            } else {
                String[] fileNameArray = {"/tmp/testChannelBlob1Up.data", "/tmp/testChannelBlob2Up.data", "/tmp/testChannelClobUp.data"};
                byte[] data = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                                           'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
                for (var n: fileNameArray) {
                    var path = Paths.get(n);
                    if (Files.notExists(path) && createFile) {
                        Files.write(Paths.get(n), data);
                    }
                }
                preparedStatement = sqlClient.prepare("INSERT INTO " + TABLE + " (key, blob_column_1, blob_column_2, clob_column) VALUES (" + KEY + ", :blob1, :blob2, :clob)",
                                                      Placeholders.of("blob1", SqlCommon.Blob.class),
                                                      Placeholders.of("blob2", SqlCommon.Blob.class),
                                                      Placeholders.of("clob", SqlCommon.Clob.class)).await();

                if (useLargeObjectClient) {
                    System.err.println("use LargeObjectClient");
                    LargeObjectClient largeObjectClient = session.getLargeObjectClient();

                    if (useStream) {
                        System.err.println("use Stream for LargeObjectClient");

                        var is0 = new FileInputStream(fileNameArray[0]);
                        var is1 = new FileInputStream(fileNameArray[1]);
                        var r2 = new InputStreamReader(new FileInputStream(fileNameArray[2]));
                        transaction.executeStatement(preparedStatement,
                                                     Parameters.blobOf("blob1", largeObjectClient.upload(is0).await()),
                                                     Parameters.blobOf("blob2", largeObjectClient.upload(is1).await()),
                                                     Parameters.clobOf("clob", largeObjectClient.upload(r2).await())).await();
                    } else {
                        transaction.executeStatement(preparedStatement,
                                                     Parameters.blobOf("blob1", largeObjectClient.upload(Paths.get(fileNameArray[0])).await()),
                                                     Parameters.blobOf("blob2", largeObjectClient.upload(Paths.get(fileNameArray[1])).await()),
                                                     Parameters.clobOf("clob", largeObjectClient.upload(Paths.get(fileNameArray[2])).await())).await();
                    }
                } else {
                    transaction.executeStatement(preparedStatement,
                                                 Parameters.blobOf("blob1", Paths.get(fileNameArray[0])),
                                                 Parameters.blobOf("blob2", Paths.get(fileNameArray[1])),
                                                 Parameters.clobOf("clob", Paths.get(fileNameArray[2]))).await();
                }

                System.out.println("insertion [b|c]lob to key " + KEY + " completed");
            }
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
    }
}
