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

public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");

    private Main(String[] args) {
    }

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("d").argName("describe").desc("do describe table.").build());
        options.addOption(Option.builder("p").argName("prepare").desc("do prepare full.").build());
        options.addOption(Option.builder("q").argName("prepare").desc("do prepare no interval.").build());
        options.addOption(Option.builder("r").argName("prepare").desc("do prepare no zone.").build());
        options.addOption(Option.builder("s").argName("prepare").desc("do prepare no date time.").build());
        options.addOption(Option.builder("t").argName("prepare").desc("do prepare no timestamp.").build());
        options.addOption(Option.builder("u").argName("prepare").desc("do prepare only int and char.").build());

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

            if (cmd.hasOption("p")) {
                String sql = "INSERT INTO new_type_test (colpk, colA_numeric, colB_numeric_p6, colC_numeric_p6_s4, col1_timestamp, col2_timestamp_0, col3_timestamp_5, col4_date, col5_time_3, col6_interval_2, col7_interval_S_4, col8_timestamptz_5, col9_timetz_6, col0_varchar_11) VALUES (:colpk, :colA_numeric, :colB_numeric_p6, :colC_numeric_p6_s4, :col1_timestamp, :col2_timestamp_0, :col3_timestamp_5, :col4_date, :col5_time_3, :col6_interval_2, :col7_interval_S_4, :col8_timestamptz_5, :col9_timetz_6, :col0_varchar_11)";
                try (var preparedStatement = sqlClient.prepare(sql,
                                                               Placeholders.of("colpk", int.class),
                                                               Placeholders.of("colA_numeric", BigDecimal.class),
                                                               Placeholders.of("colB_numeric_p6", BigDecimal.class),
                                                               Placeholders.of("colC_numeric_p6_s4", BigDecimal.class),
                                                               Placeholders.of("col1_timestamp", LocalDateTime.class),
                                                               Placeholders.of("col2_timestamp_0", LocalDateTime.class),
                                                               Placeholders.of("col3_timestamp_5", LocalDateTime.class),
                                                               Placeholders.of("col4_date", LocalDate.class),
                                                               Placeholders.of("col5_time_3", LocalTime.class),
                                                               Placeholders.of("col6_interval_2", DateTimeInterval.class),
                                                               Placeholders.of("col7_interval_S_4", DateTimeInterval.class),
                                                               Placeholders.of("col8_timestamptz_5", OffsetDateTime.class),
                                                               Placeholders.of("col9_timetz_6", OffsetTime.class),
                                                               Placeholders.of("col0_varchar_11", String.class)).get();
                     Transaction transaction = sqlClient.createTransaction().await()) {
                }
                System.out.println("full success");
            }
            if (cmd.hasOption("q")) {
                String sql = "INSERT INTO new_type_test (colpk, colA_numeric, colB_numeric_p6, colC_numeric_p6_s4, col1_timestamp, col2_timestamp_0, col3_timestamp_5, col4_date, col5_time_3, col8_timestamptz_5, col9_timetz_6, col0_varchar_11) VALUES (:colpk, :colA_numeric, :colB_numeric_p6, :colC_numeric_p6_s4, :col1_timestamp, :col2_timestamp_0, :col3_timestamp_5, :col4_date, :col5_time_3, :col8_timestamptz_5, :col9_timetz_6, :col0_varchar_11)";
                try (var preparedStatement = sqlClient.prepare(sql,
                                                               Placeholders.of("colpk", int.class),
                                                               Placeholders.of("colA_numeric", BigDecimal.class),
                                                               Placeholders.of("colB_numeric_p6", BigDecimal.class),
                                                               Placeholders.of("colC_numeric_p6_s4", BigDecimal.class),
                                                               Placeholders.of("col1_timestamp", LocalDateTime.class),
                                                               Placeholders.of("col2_timestamp_0", LocalDateTime.class),
                                                               Placeholders.of("col3_timestamp_5", LocalDateTime.class),
                                                               Placeholders.of("col4_date", LocalDate.class),
                                                               Placeholders.of("col5_time_3", LocalTime.class),
                                                               Placeholders.of("col8_timestamptz_5", OffsetDateTime.class),
                                                               Placeholders.of("col9_timetz_6", OffsetTime.class),
                                                               Placeholders.of("col0_varchar_11", String.class)).get();
                     Transaction transaction = sqlClient.createTransaction().await()) {
                }
                System.out.println("no interval success");
            }
            if (cmd.hasOption("r")) {
                String sql = "INSERT INTO new_type_test (colpk, colA_numeric, colB_numeric_p6, colC_numeric_p6_s4, col1_timestamp, col2_timestamp_0, col3_timestamp_5, col4_date, col5_time_3, col0_varchar_11) VALUES (:colpk, :colA_numeric, :colB_numeric_p6, :colC_numeric_p6_s4, :col1_timestamp, :col2_timestamp_0, :col3_timestamp_5, :col4_date, :col5_time_3, :col0_varchar_11)";
                try (var preparedStatement = sqlClient.prepare(sql,
                                                               Placeholders.of("colpk", int.class),
                                                               Placeholders.of("colA_numeric", BigDecimal.class),
                                                               Placeholders.of("colB_numeric_p6", BigDecimal.class),
                                                               Placeholders.of("colC_numeric_p6_s4", BigDecimal.class),
                                                               Placeholders.of("col1_timestamp", LocalDateTime.class),
                                                               Placeholders.of("col2_timestamp_0", LocalDateTime.class),
                                                               Placeholders.of("col3_timestamp_5", LocalDateTime.class),
                                                               Placeholders.of("col4_date", LocalDate.class),
                                                               Placeholders.of("col5_time_3", LocalTime.class),
                                                               Placeholders.of("col0_varchar_11", String.class)).get();
                     Transaction transaction = sqlClient.createTransaction().await()) {
                }
                System.out.println("no zone success");
            }
            if (cmd.hasOption("s")) {
                String sql = "INSERT INTO new_type_test (colpk, colA_numeric, colB_numeric_p6, colC_numeric_p6_s4, col1_timestamp, col2_timestamp_0, col3_timestamp_5, col4_date, col5_time_3, col0_varchar_11) VALUES (:colpk, :colA_numeric, :colB_numeric_p6, :colC_numeric_p6_s4, :col1_timestamp, :col2_timestamp_0, :col3_timestamp_5, :col4_date, :col5_time_3, :col0_varchar_11)";
                try (var preparedStatement = sqlClient.prepare(sql,
                                                               Placeholders.of("colpk", int.class),
                                                               Placeholders.of("colA_numeric", BigDecimal.class),
                                                               Placeholders.of("colB_numeric_p6", BigDecimal.class),
                                                               Placeholders.of("colC_numeric_p6_s4", BigDecimal.class),
                                                               Placeholders.of("col1_timestamp", LocalDateTime.class),
                                                               Placeholders.of("col2_timestamp_0", LocalDateTime.class),
                                                               Placeholders.of("col3_timestamp_5", LocalDateTime.class),
                                                               Placeholders.of("col4_date", LocalDate.class),
                                                               Placeholders.of("col5_time_3", LocalTime.class),
                                                               Placeholders.of("col0_varchar_11", String.class)).get();
                     Transaction transaction = sqlClient.createTransaction().await()) {
                }
                System.out.println("no date time success");
            }
            if (cmd.hasOption("t")) {
                String sql = "INSERT INTO new_type_test (colpk, colA_numeric, colB_numeric_p6, colC_numeric_p6_s4, col0_varchar_11) VALUES (:colpk, :colA_numeric, :colB_numeric_p6, :colC_numeric_p6_s4, :col0_varchar_11)";
                try (var preparedStatement = sqlClient.prepare(sql,
                                                               Placeholders.of("colpk", int.class),
                                                               Placeholders.of("colA_numeric", BigDecimal.class),
                                                               Placeholders.of("colB_numeric_p6", BigDecimal.class),
                                                               Placeholders.of("colC_numeric_p6_s4", BigDecimal.class),
                                                               Placeholders.of("col0_varchar_11", String.class)).get();
                     Transaction transaction = sqlClient.createTransaction().await()) {
                }
                System.out.println("no timestamp success");
            }
            if (cmd.hasOption("u")) {
                String sql = "INSERT INTO new_type_test (colpk, col0_varchar_11) VALUES (:colpk, :col0_varchar_11)";
                try (var preparedStatement = sqlClient.prepare(sql,
                                                               Placeholders.of("colpk", int.class),
                                                               Placeholders.of("col0_varchar_11", String.class)).get();
                     Transaction transaction = sqlClient.createTransaction().await()) {
                }
                System.out.println("only int and char success");
            }
            
        } catch (IOException | InterruptedException | TimeoutException | ServerException | ParseException e) {
            System.out.println(e);
        }
    }
}

// CREATE TABLE new_type_test (
//     colA_numeric        NUMERIC,
//     colB_numeric_p6     NUMERIC (6),
//     colC_numeric_p6_s4  NUMERIC (6, 4),
//     col1_timestamp      TIMESTAMP,
//     col2_timestamp_0    TIMESTAMP (0),
//     col3_timestamp_5    TIMESTAMP (5),
//     col4_date           DATE,
//     col5_time_3         TIME (3),
//     col6_interval_2     INTERVAL (2),
//     col7_interval_S_4   INTERVAL SECOND (4),
//     col8_timestamptz_5  TIMESTAMP (5) with time zone,
//     col9_timetz_6       TIME (6) with time zone,
//     col0_varchar_11     VARCHAR (11)
// ) tablespace tsurugi;
