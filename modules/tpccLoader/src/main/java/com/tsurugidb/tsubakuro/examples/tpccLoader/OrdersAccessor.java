package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class OrdersAccessor implements TableAccessor {
    private final String tableName = "ORDERS";
    private final String createTable = "CREATE TABLE ORDERS (o_id INT NOT NULL, o_d_id INT NOT NULL, o_w_id INT NOT NULL, o_c_id INT NOT NULL, o_entry_d CHAR(24) NOT NULL, o_carrier_id INT, o_ol_cnt INT NOT NULL, o_all_local INT NOT NULL, PRIMARY KEY(o_w_id, o_d_id, o_id))";
    private final String insert = "INSERT INTO ORDERS (o_id, o_c_id, o_d_id, o_w_id, o_entry_d, o_carrier_id, o_ol_cnt, o_all_local)VALUES (:o_id, :o_c_id, :o_d_id, :o_w_id, :o_entry_d, :o_carrier_id, :o_ol_cnt, :o_all_local)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("o_id", long.class),
        Placeholders.of("o_c_id", long.class),
        Placeholders.of("o_d_id", long.class),
        Placeholders.of("o_w_id", long.class),
        Placeholders.of("o_entry_d", String.class),
        Placeholders.of("o_carrier_id", long.class),
        Placeholders.of("o_ol_cnt", long.class),
        Placeholders.of("o_all_local", long.class) };

    OrdersAccessor() {
    }

    public String tableName() {
        return tableName;
    }

    public void createTable(Transaction transaction) throws IOException {
        try {
            transaction.executeStatement(createTable).get();
        } catch (ServerException | InterruptedException e) {
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
                                             Parameters.of("o_id", Long.parseLong(columns[0])),
                                             Parameters.of("o_c_id", Long.parseLong(columns[1])),
                                             Parameters.of("o_d_id", Long.parseLong(columns[2])),
                                             Parameters.of("o_w_id", Long.parseLong(columns[3])),
                                             Parameters.of("o_entry_d", columns[4]),
                                             (!columns[5].equals("")) ?
                                             Parameters.of("o_carrier_id", Long.parseLong(columns[5])) :
                                             Parameters.ofNull("o_carrier_id"),
                                             Parameters.of("o_ol_cnt", Long.parseLong(columns[6])),
                                             Parameters.of("o_all_local", Long.parseLong(columns[7]))).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
