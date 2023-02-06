package com.tsurugidb.tsubakuro.examples.datetime;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// for command options
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

// for types
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import com.tsurugidb.tsubakuro.sql.io.DateTimeInterval;

import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.ResultSetMetadata;

public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");

    private Main(String[] args) {
    }

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("z").argName("prepare").desc("do simple test.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             SqlClient sqlClient = SqlClient.attach(session);) {

            cmd = parser.parse(options, args);
            if (cmd.hasOption("d")) {
                var meta = sqlClient.getTableMetadata("new_type_test").await();
                System.out.println(meta);
            }

            if (cmd.hasOption("z")) {
                String insert = "INSERT INTO decimal_test (pk, dec_value, date_value, ts_value) VALUES (:pk, :dec_value, :date_value, :ts_value)";
                String select = "SELECT * FROM decimal_test";
                try (var preparedStatementInsert = sqlClient.prepare(insert,
                                                                     Placeholders.of("pk", int.class),
                                                                     Placeholders.of("dec_value", BigDecimal.class),
                                                                     Placeholders.of("date_value", LocalDate.class),
                                                                     Placeholders.of("ts_value", LocalDateTime.class)).get();
                     var preparedStatementSelect = sqlClient.prepare(select).get();
                     Transaction transaction = sqlClient.createTransaction().await()) {

                    transaction.executeStatement(preparedStatementInsert,
                                                 Parameters.of("pk", 10),
                                                 //                                                 Parameters.of("dec_value", BigDecimal.valueOf(1.2)),
                                                 Parameters.of("dec_value", BigDecimal.ONE),
                                                 Parameters.of("date_value", LocalDate.of(2023, 1, 10)),
                                                 Parameters.of("ts_value", LocalDateTime.of(LocalDate.ofEpochDay(10_000), LocalTime.ofNanoOfDay(123_456_789)))).get();

                    try (var resultSet = transaction.executeQuery(preparedStatementSelect).get(); ) {
                        printResultset(resultSet);
                    }
                    
                    transaction.commit();
                }
            }

        } catch (IOException | InterruptedException | TimeoutException | ServerException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void printResultset(ResultSet resultSet) throws InterruptedException, IOException, ServerException {
        int count = 1;
        
        while (resultSet.nextRow()) {
            System.out.println("---- ( " + count + " )----");
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

                    case DECIMAL:
                        System.out.println(resultSet.fetchDecimalValue());
                        break;
                    case DATE:
                        System.out.println(resultSet.fetchDateValue());
                        break;
                    case TIME_POINT:
                        System.out.println(resultSet.fetchTimePointValue());
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
    }
}
