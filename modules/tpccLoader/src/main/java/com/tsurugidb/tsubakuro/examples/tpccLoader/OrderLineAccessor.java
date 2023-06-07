package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class OrderLineAccessor implements TableAccessor {
    private final String tableName = "ORDER_LINE";
    private final String createTable = "CREATE TABLE ORDER_LINE (ol_o_id INT NOT NULL, ol_d_id INT NOT NULL, ol_w_id INT NOT NULL, ol_number INT NOT NULL, ol_i_id INT NOT NULL, ol_supply_w_id INT NOT NULL, ol_delivery_d CHAR(24), ol_quantity INT NOT NULL, ol_amount DOUBLE NOT NULL, ol_dist_info CHAR(24) NOT NULL, PRIMARY KEY(ol_w_id, ol_d_id, ol_o_id, ol_number))";
    private final String insert = "INSERT INTO ORDER_LINE (ol_o_id, ol_d_id, ol_w_id, ol_number, ol_i_id, ol_supply_w_id, ol_quantity, ol_amount, ol_dist_info, ol_delivery_d) VALUES (:ol_o_id, :ol_d_id, :ol_w_id, :ol_number, :ol_i_id, :ol_supply_w_id, :ol_quantity, :ol_amount, :ol_dist_info, :ol_delivery_d)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("ol_o_id", long.class),
        Placeholders.of("ol_d_id", long.class),
        Placeholders.of("ol_w_id", long.class),
        Placeholders.of("ol_number", long.class),
        Placeholders.of("ol_i_id", long.class),
        Placeholders.of("ol_supply_w_id", long.class),
        Placeholders.of("ol_quantity", long.class),
        Placeholders.of("ol_amount", double.class),
        Placeholders.of("ol_dist_info", String.class),
        Placeholders.of("ol_delivery_d", String.class) };

    OrderLineAccessor() {
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
                                             Parameters.of("ol_o_id", Long.parseLong(columns[0])),
                                             Parameters.of("ol_d_id", Long.parseLong(columns[1])),
                                             Parameters.of("ol_w_id", Long.parseLong(columns[2])),
                                             Parameters.of("ol_number", Long.parseLong(columns[3])),
                                             Parameters.of("ol_i_id", Long.parseLong(columns[4])),
                                             Parameters.of("ol_supply_w_id", Long.parseLong(columns[5])),
                                             Parameters.of("ol_quantity", Long.parseLong(columns[6])),
                                             Parameters.of("ol_amount", Double.parseDouble(columns[7])),
                                             Parameters.of("ol_dist_info", columns[8]),
                                             Parameters.of("ol_delivery_d", "")).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
