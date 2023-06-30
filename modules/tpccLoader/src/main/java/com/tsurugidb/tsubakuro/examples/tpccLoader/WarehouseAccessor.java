package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class WarehouseAccessor implements TableAccessor {
    private final String tableName = "WAREHOUSE";
    private final String createTable = "CREATE TABLE WAREHOUSE (w_id BIGINT NOT NULL, w_name VARCHAR(10) NOT NULL, w_street_1 VARCHAR(20) NOT NULL, w_street_2 VARCHAR(20) NOT NULL, w_city VARCHAR(20) NOT NULL, w_state CHAR(2) NOT NULL, w_zip CHAR(9) NOT NULL, w_tax DOUBLE NOT NULL, w_ytd DOUBLE NOT NULL, PRIMARY KEY(w_id))";
    private final String insert = "INSERT INTO WAREHOUSE (w_id, w_name, w_street_1, w_street_2, w_city, w_state, w_zip, w_tax, w_ytd) VALUES (:w_id, :w_name, :w_street_1, :w_street_2, :w_city, :w_state, :w_zip, :w_tax, :w_ytd)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("w_id", long.class),
        Placeholders.of("w_name", String.class),
        Placeholders.of("w_street_1", String.class),
        Placeholders.of("w_street_2", String.class),
        Placeholders.of("w_city", String.class),
        Placeholders.of("w_state", String.class),
        Placeholders.of("w_zip", String.class),
        Placeholders.of("w_tax", double.class),
        Placeholders.of("w_ytd", double.class) };

    WarehouseAccessor() {
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
                                             Parameters.of("w_id", Long.parseLong(columns[0])),
                                             Parameters.of("w_name", columns[1]),
                                             Parameters.of("w_street_1", columns[2]),
                                             Parameters.of("w_street_2", columns[3]),
                                             Parameters.of("w_city", columns[4]),
                                             Parameters.of("w_state", columns[5]),
                                             Parameters.of("w_zip", columns[6]),
                                             Parameters.of("w_tax", Double.parseDouble(columns[7])),
                                             Parameters.of("w_ytd", Double.parseDouble(columns[8]))).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
