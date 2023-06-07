package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class ItemAccessor implements TableAccessor {
    private final String tableName = "ITEM";
    private final String createTable = "CREATE TABLE ITEM (i_id INT NOT NULL, i_name VARCHAR(24) NOT NULL, i_price DOUBLE NOT NULL, i_data VARCHAR(50) NOT NULL, PRIMARY KEY(i_id))";
    //    private final String insert = "INSERT INTO ITEM (i_id, i_name, i_price, i_data) VALUES (:i_id, :i_name, :i_price, :i_data)";
    private final String insert = "INSERT INTO ITEM (i_id, i_name, i_price, i_data, i_im_id) VALUES (:i_id, :i_name, :i_price, :i_data, 0)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("i_id", long.class),
        Placeholders.of("i_name", String.class),
        Placeholders.of("i_price", double.class),
        Placeholders.of("i_data",  String.class) };

    ItemAccessor() {
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
                                             Parameters.of("i_id", Long.parseLong(columns[0])),
                                             Parameters.of("i_name", columns[1]),
                                             Parameters.of("i_price", Double.parseDouble(columns[2])),
                                             Parameters.of("i_data", columns[3])).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
