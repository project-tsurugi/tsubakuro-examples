package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class DistrictAccessor implements TableAccessor {
    private final String tableName = "DISTRICT";
    private final String createTable = "CREATE TABLE DISTRICT (d_id INT NOT NULL, d_w_id INT NOT NULL, d_name VARCHAR(10) NOT NULL, d_street_1 VARCHAR(20) NOT NULL, d_street_2 VARCHAR(20) NOT NULL, d_city VARCHAR(20) NOT NULL, d_state CHAR(2) NOT NULL, d_zip  CHAR(9) NOT NULL, d_tax DOUBLE NOT NULL, d_ytd DOUBLE NOT NULL, d_next_o_id INT NOT NULL, PRIMARY KEY(d_w_id, d_id))";
    private final String insert = "INSERT INTO DISTRICT (d_id, d_w_id, d_name, d_street_1, d_street_2, d_city, d_state, d_zip, d_tax, d_ytd, d_next_o_id) VALUES (:d_id, :d_w_id, :d_name, d_street_1, :d_street_2, :d_city, :d_state, :d_zip, d_tax, :d_ytd, :d_next_o_id)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("d_id", long.class),
        Placeholders.of("d_w_id", long.class),
        Placeholders.of("d_name", String.class),
        Placeholders.of("d_street_1", String.class),
        Placeholders.of("d_street_2", String.class),
        Placeholders.of("d_city", String.class),
        Placeholders.of("d_state", String.class),
        Placeholders.of("d_zip", String.class),
        Placeholders.of("d_tax", double.class),
        Placeholders.of("d_ytd", double.class),
        Placeholders.of("d_next_o_id", long.class) };

    DistrictAccessor() {
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
                                             Parameters.of("d_id", Long.parseLong(columns[0])),
                                             Parameters.of("d_w_id", Long.parseLong(columns[1])),
                                             Parameters.of("d_name", columns[2]),
                                             Parameters.of("d_street_1", columns[3]),
                                             Parameters.of("d_street_2", columns[4]),
                                             Parameters.of("d_city", columns[5]),
                                             Parameters.of("d_state", columns[6]),
                                             Parameters.of("d_zip", columns[7]),
                                             Parameters.of("d_tax", Double.parseDouble(columns[8])),
                                             Parameters.of("d_ytd", Double.parseDouble(columns[9])),
                                             Parameters.of("d_next_o_id", Long.parseLong(columns[10]))).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
