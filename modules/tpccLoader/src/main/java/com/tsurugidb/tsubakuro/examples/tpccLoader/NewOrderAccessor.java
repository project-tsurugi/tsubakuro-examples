package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class NewOrderAccessor implements TableAccessor {
    private final String tableName = "NEW_ORDER";
    private final String createTable = "CREATE TABLE NEW_ORDER (no_o_id INT NOT NULL, no_d_id INT NOT NULL, no_w_id INT NOT NULL, PRIMARY KEY(no_w_id, no_d_id, no_o_id))";
    private final String insert = "INSERT INTO NEW_ORDER (no_o_id, no_d_id, no_w_id)VALUES (:no_o_id, :no_d_id, :no_w_id)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("no_o_id", long.class),
        Placeholders.of("no_d_id", long.class),
        Placeholders.of("no_w_id", long.class) };

    NewOrderAccessor() {
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
                                             Parameters.of("no_o_id", Long.parseLong(columns[0])),
                                             Parameters.of("no_d_id", Long.parseLong(columns[1])),
                                             Parameters.of("no_w_id", Long.parseLong(columns[2]))).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
