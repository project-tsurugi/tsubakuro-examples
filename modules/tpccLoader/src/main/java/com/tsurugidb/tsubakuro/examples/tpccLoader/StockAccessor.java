package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class StockAccessor implements TableAccessor {
    private final String tableName = "STOCK";
    private final String createTable = "CREATE TABLE STOCK (s_i_id BIGINT NOT NULL, s_w_id BIGINT NOT NULL, s_quantity BIGINT NOT NULL, s_dist_01 CHAR(24) NOT NULL, s_dist_02 CHAR(24) NOT NULL, s_dist_03 CHAR(24) NOT NULL, s_dist_04 CHAR(24) NOT NULL, s_dist_05 CHAR(24) NOT NULL, s_dist_06 CHAR(24) NOT NULL, s_dist_07 CHAR(24) NOT NULL, s_dist_08 CHAR(24) NOT NULL, s_dist_09 CHAR(24) NOT NULL, s_dist_10 CHAR(24) NOT NULL, s_ytd BIGINT NOT NULL, s_order_cnt BIGINT NOT NULL, s_remote_cnt BIGINT NOT NULL, s_data VARCHAR(50) NOT NULL, PRIMARY KEY(s_w_id, s_i_id))";
    private final String insert = "INSERT INTO STOCK (s_i_id, s_w_id, s_quantity, s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05, s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, s_data, s_ytd, s_order_cnt, s_remote_cnt) VALUES (:s_i_id, :s_w_id, :s_quantity, :s_dist_01, :s_dist_02, :s_dist_03, :s_dist_04, :s_dist_05, :s_dist_06, :s_dist_07, :s_dist_08, :s_dist_09, :s_dist_10, :s_data, 0, 0, 0)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("s_i_id", long.class),
        Placeholders.of("s_w_id", long.class),
        Placeholders.of("s_quantity", long.class),
        Placeholders.of("s_dist_01", String.class),
        Placeholders.of("s_dist_02", String.class),
        Placeholders.of("s_dist_03", String.class),
        Placeholders.of("s_dist_04", String.class),
        Placeholders.of("s_dist_05", String.class),
        Placeholders.of("s_dist_06", String.class),
        Placeholders.of("s_dist_07", String.class),
        Placeholders.of("s_dist_08", String.class),
        Placeholders.of("s_dist_09", String.class),
        Placeholders.of("s_dist_10", String.class),
        Placeholders.of("s_data", String.class) };

    StockAccessor() {
    }

    public String tableName() {
        return tableName;
    }

    public void createTable(Transaction transaction) throws IOException {
        try {
            transaction.executeStatement(createTable).get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }

    public void insert(SqlClient client, CsvReader csvReader) throws IOException {
        try (var preparedStatement = client.prepare(insert, placeholders).get();
             var transaction = client.createTransaction().get();) {
            do {
                String[] columns = csvReader.getContents();
                if (columns == null) {
                    break;
                }
                transaction.executeStatement(preparedStatement,
                                             Parameters.of("s_i_id", Long.parseLong(columns[0])),
                                             Parameters.of("s_w_id", Long.parseLong(columns[1])),
                                             Parameters.of("s_quantity", Long.parseLong(columns[2])),
                                             Parameters.of("s_dist_01", columns[3]),
                                             Parameters.of("s_dist_02", columns[4]),
                                             Parameters.of("s_dist_03", columns[5]),
                                             Parameters.of("s_dist_04", columns[6]),
                                             Parameters.of("s_dist_05", columns[7]),
                                             Parameters.of("s_dist_06", columns[8]),
                                             Parameters.of("s_dist_07", columns[9]),
                                             Parameters.of("s_dist_08", columns[10]),
                                             Parameters.of("s_dist_09", columns[11]),
                                             Parameters.of("s_dist_10", columns[12]),
                                             Parameters.of("s_data", columns[13])).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
