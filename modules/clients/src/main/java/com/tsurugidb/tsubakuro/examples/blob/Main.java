package com.tsurugidb.tsubakuro.examples.blob;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.RandomStringUtils;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.BlobTransferMedium;
import com.tsurugidb.tsubakuro.common.BlobTransferType;
import com.tsurugidb.tsubakuro.common.LargeObjectClient;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.BlobReference;
import com.tsurugidb.tsubakuro.sql.ClobReference;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.impl.BlobReferenceForSql;
import com.tsurugidb.tsubakuro.sql.impl.ClobReferenceForSql;
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
    //    private static boolean createFile = true;
    private static boolean privileged = false;
    private static boolean createTable = false;
    private static boolean useLargeObjectClient = false;
    private static boolean useStream = false;
    private static boolean doesNotUse = false;
    private static boolean largeData = false;
    private static int loop = 1;
    private static char faultType = 'n';

    private final static String TABLE = "testTable";
    private final static String KEY = "1";

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("q").argName("query").desc("Query mode.").build());
        //        options.addOption(Option.builder("n").argName("noCreateFile").desc("Does not create file.").build());
        options.addOption(Option.builder("p").argName("privileged").desc("Use privileged blob transfer.").build());
        options.addOption(Option.builder("i").argName("initialize (createTable)").desc("Create table first.").build());
        options.addOption(Option.builder("c").argName("use LargeObjectClient").desc("Use LargeObjectClient.").build());
        options.addOption(Option.builder("s").argName("use LargeObjectClient stream").desc("Use LargeObjectClient stream.").build());
        options.addOption(Option.builder("d").argName("does not use BLOB").desc("does not use BLOB.").build());
        options.addOption(Option.builder("l").argName("large data").desc("use large test data.").build());
        options.addOption(Option.builder("f").argName("fault injection").hasArg().desc("fault injection.").build());
        options.addOption(Option.builder("m").argName("multiple query").hasArg().desc("multiple query.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("q")) {
                query = true;
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
            if (cmd.hasOption("d")) {
                doesNotUse = true;
            }
            if (cmd.hasOption("l")) {
                largeData = true;
            }
            if (cmd.hasOption("m")) {
                loop = Integer.parseInt(cmd.getOptionValue("m"));
            }
            if (cmd.hasOption("f")) {
                faultType = cmd.getOptionValue("f").charAt(0);
            }
            //            if (cmd.hasOption("n")) {
            //                createFile = false;
            //            }
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
             .withBlobTransfer(doesNotUse ? BlobTransferType.DOES_NOT_USE : (privileged ? BlobTransferType.PRIVILEGED : BlobTransferType.RELAY))
             .create(timeout, TimeUnit.MILLISECONDS);
             SqlClient sqlClient = SqlClient.attach(session);
             var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {

            var blobTransferMedium = session.getBlobTransferMedium();

            PreparedStatement preparedStatement = null;
            if (query) {
                for (int lc = 0; lc < loop; lc++) {
                System.out.println(blobTransferMedium.getBlobTransferType());
                System.out.println(blobTransferMedium.getParameters());

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
                                BlobReference blobColumn = resultSet.fetchBlob();
                                switch (faultType) {
                                case 'o':
                                    blobColumn = new BlobReferenceForSql(SqlCommon.LargeObjectProvider.forNumber((int) blobColumn.getProvider()), blobColumn.getObjectId() + 1111, blobColumn.getReferenceTag());
                                    break;
                                case 't':
                                    blobColumn = new BlobReferenceForSql(SqlCommon.LargeObjectProvider.forNumber((int) blobColumn.getProvider()), blobColumn.getObjectId(), blobColumn.getReferenceTag() + 1111);
                                    break;
                                }
                                var is = transaction.openInputStream(blobColumn).await();
                                System.out.print("column name = " + metadata.get(columnIndex).getName() + ": content = '");
                                long blobSize = 0;
                                while (true) {
                                    var i = is.read();
                                    if (i < 0) {
                                        if (largeData) {
                                            System.out.print("size = " + blobSize + " byte");
                                        }
                                        break;
                                    }
                                    if (!largeData) {
                                        char c = (char) i;
                                        if (c != '\n') {
                                            System.out.print(c);
                                        }
                                    }
                                    blobSize++;
                                }
                                System.out.println("'");
                                break;
                            case CLOB:
                                ClobReference clobColumn = resultSet.fetchClob();
                                switch (faultType) {
                                case 'o':
                                    clobColumn = new ClobReferenceForSql(SqlCommon.LargeObjectProvider.forNumber((int) clobColumn.getProvider()), clobColumn.getObjectId() + 1111, clobColumn.getReferenceTag());
                                    break;
                                case 't':
                                    clobColumn = new ClobReferenceForSql(SqlCommon.LargeObjectProvider.forNumber((int) clobColumn.getProvider()), clobColumn.getObjectId(), clobColumn.getReferenceTag() + 1111);
                                    break;
                                }
                                var lr = new LineNumberReader(transaction.openReader(clobColumn).await());
                                long clobSize = 0;
                                System.out.print("column name = " + metadata.get(columnIndex).getName() + ": content = '");
                                String s;
                                while ((s = lr.readLine()) != null) {
                                    if (!largeData) {
                                        System.out.print(s);
                                    }
                                    clobSize += (s.length() + 1);
                                }
                                if (largeData) {
                                    System.out.print("size = " + clobSize + " byte");
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
                }

            } else {

                String[] fileNameArray = {"/tmp/testChannelBlob1Up.data", "/tmp/testChannelBlob2Up.data", "/tmp/testChannelClobUp.data"};
                byte[] data = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                                           'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
                for (var n: fileNameArray) {
                    var path = Paths.get(n);
                    
                    if (Files.exists(path)) {
                        Files.delete(path);
                    }

                    if (largeData) {
                        if (Files.notExists(path)) {
                            createHugeFile(path, 1024);
                        } else {
                            throw new AssertionError();
                        }
                    } else {
                        if (Files.notExists(path)) {
                            Files.write(Paths.get(n), data);
                        } else {
                            throw new AssertionError();
                        }
                    }
                }
                preparedStatement = sqlClient.prepare("INSERT INTO " + TABLE + " (key, blob_column_1, blob_column_2, clob_column) VALUES (" + KEY + ", :blob1, :blob2, :clob)",
                                                      Placeholders.of("blob1", SqlCommon.Blob.class),
                                                      Placeholders.of("blob2", SqlCommon.Blob.class),
                                                      Placeholders.of("clob", SqlCommon.Clob.class)).await();

                SqlRequest.Parameter p1;
                SqlRequest.Parameter p2;
                SqlRequest.Parameter p3;
                    
                if (useLargeObjectClient) {
                    System.err.println("use LargeObjectClient");
                    LargeObjectClient largeObjectClient = session.getLargeObjectClient();
                    System.out.println(largeObjectClient);

                    if (useStream) {
                        System.err.println("use Stream for LargeObjectClient");

                        p1 = Parameters.blobOf("blob1", largeObjectClient.upload(new FileInputStream(fileNameArray[0])).await());
                        p2 = Parameters.blobOf("blob2", largeObjectClient.upload(new FileInputStream(fileNameArray[1])).await());
                        p3 = Parameters.clobOf("clob", largeObjectClient.upload(new InputStreamReader(new FileInputStream(fileNameArray[2]))).await());
                    } else {
                        p1 = Parameters.blobOf("blob1", largeObjectClient.upload(Paths.get(fileNameArray[0])).await());
                        p2 = Parameters.blobOf("blob2", largeObjectClient.upload(Paths.get(fileNameArray[1])).await());
                        p3 = Parameters.clobOf("clob", largeObjectClient.upload(Paths.get(fileNameArray[2])).await());
                    }
                    switch (faultType) {
                    case 'o':
                        var ref1 = p1.getLargeObjectInfoBlob().getBlobRelayReference();
                        p1 = SqlRequest.Parameter.newBuilder()
                            .setName(p1.getName())
                            .setLargeObjectInfoBlob(SqlRequest.ClientOnlyLargeObjectInfo.newBuilder()
                                                    .setBlobRelayReference(SqlRequest.BlobRelayReference.newBuilder()
                                                                           .setStorageId(ref1.getStorageId())
                                                                           .setObjectId(ref1.getObjectId() + 1111)
                                                                           .setTag(ref1.getTag())))
                            .build();
                        break;
                    case 't':
                        var ref2 = p2.getLargeObjectInfoBlob().getBlobRelayReference();
                        p2 = SqlRequest.Parameter.newBuilder()
                            .setName(p2.getName())
                            .setLargeObjectInfoBlob(SqlRequest.ClientOnlyLargeObjectInfo.newBuilder()
                                                    .setBlobRelayReference(SqlRequest.BlobRelayReference.newBuilder()
                                                                           .setStorageId(ref2.getStorageId())
                                                                           .setObjectId(ref2.getObjectId())
                                                                           .setTag(ref2.getTag() + 1111)))
                            .build();
                        break;
                    }
                } else {
                    p1 = Parameters.blobOf("blob1", Paths.get(fileNameArray[0]));
                    p2 = Parameters.blobOf("blob2", Paths.get(fileNameArray[1]));
                    p3 = Parameters.clobOf("clob", Paths.get(fileNameArray[2]));
                }
                transaction.executeStatement(preparedStatement, p1, p2, p3).await();

                System.out.println("insertion [b|c]lob to key " + KEY + " completed");
            }
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
    }

    static void createHugeFile(final Path path, int size) throws IOException {
        Iterable<String> lines = () -> Stream
            .generate(() -> RandomStringUtils.randomAlphanumeric(size))
            .limit(1024).iterator();
        Files.write(path, lines);
    }
}
