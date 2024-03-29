package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class HistoryAccessor implements TableAccessor {
    private final String tableName = "HISTORY";
    private final String createTable = "CREATE TABLE HISTORY (h_c_id BIGINT, h_c_d_id BIGINT, h_c_w_id BIGINT, h_d_id BIGINT, h_w_id BIGINT, h_date CHAR(25), h_amount DOUBLE, h_data VARCHAR(24))";
    private final String insert = "INSERT INTO HISTORY (h_c_id, h_c_d_id, h_c_w_id, h_d_id, h_w_id, h_date, h_amount, h_data) VALUES (:c_id, :c_d_id, :c_w_id, :d_id, :w_id, :timestamp, :h_amount, :h_data)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("c_id", long.class),
        Placeholders.of("c_d_id", long.class),
        Placeholders.of("c_w_id", long.class),
        Placeholders.of("d_id", long.class),
        Placeholders.of("w_id", long.class),
        Placeholders.of("timestamp", String.class),
        Placeholders.of("h_amount", double.class),
        Placeholders.of("h_data",  String.class) };

    HistoryAccessor() {
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
                                             Parameters.of("c_id", Long.parseLong(columns[0])),
                                             Parameters.of("c_d_id", Long.parseLong(columns[1])),
                                             Parameters.of("c_w_id", Long.parseLong(columns[2])),
                                             Parameters.of("d_id", Long.parseLong(columns[3])),
                                             Parameters.of("w_id", Long.parseLong(columns[4])),
                                             Parameters.of("timestamp", columns[5]),
                                             Parameters.of("h_amount", Double.parseDouble(columns[6])),
                                             Parameters.of("h_data", columns[7])).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
