package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

final class CustomerAccessor implements TableAccessor {
    private final String tableName = "CUSTOMER";
    private final String createTable = "CREATE TABLE CUSTOMER (c_id INT NOT NULL, c_d_id INT NOT NULL, c_w_id INT NOT NULL, c_first VARCHAR(16) NOT NULL, c_middle CHAR(2) NOT NULL, c_last VARCHAR(16) NOT NULL, c_street_1 VARCHAR(20) NOT NULL, c_street_2 VARCHAR(20) NOT NULL, c_city VARCHAR(20) NOT NULL, c_state CHAR(2) NOT NULL, c_zip  CHAR(9) NOT NULL, c_phone CHAR(16) NOT NULL, c_since CHAR(24) NOT NULL, c_credit CHAR(2) NOT NULL, c_credit_lim DOUBLE NOT NULL, c_discount DOUBLE NOT NULL, c_balance DOUBLE NOT NULL, c_ytd_payment DOUBLE NOT NULL, c_payment_cnt INT NOT NULL, c_delivery_cnt INT NOT NULL, c_data VARCHAR(500) NOT NULL, PRIMARY KEY(c_w_id, c_d_id, c_id))";
    private final String insert = "INSERT INTO CUSTOMER (c_id, c_d_id, c_w_id, c_first, c_middle, c_last, c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, c_since, c_credit, c_credit_lim, c_discount, c_balance, c_data, c_ytd_payment, c_payment_cnt, c_delivery_cnt) VALUES (:c_id, :c_d_id, :c_w_id, :c_first, :c_middle, :c_last, :c_street_1, :c_street_2, :c_city, :c_state, :c_zip, :c_phone, :c_since, :c_credit, :c_credit_lim, :c_discount, :c_balance, :c_data, 10.0, 1, 0)";
    private final SqlRequest.Placeholder[] placeholders = {
        Placeholders.of("c_id", long.class),
        Placeholders.of("c_d_id", long.class),
        Placeholders.of("c_w_id", long.class),
        Placeholders.of("c_first", String.class),
        Placeholders.of("c_middle", String.class),
        Placeholders.of("c_last", String.class),
        Placeholders.of("c_street_1", String.class),
        Placeholders.of("c_street_2", String.class),
        Placeholders.of("c_city", String.class),
        Placeholders.of("c_state", String.class),
        Placeholders.of("c_zip", String.class),
        Placeholders.of("c_phone", String.class),
        Placeholders.of("c_since", String.class),
        Placeholders.of("c_credit", String.class),
        Placeholders.of("c_credit_lim", double.class),
        Placeholders.of("c_discount", double.class),
        Placeholders.of("c_balance", double.class),
        Placeholders.of("c_data", String.class) };

    CustomerAccessor() {
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
                                             Parameters.of("c_first", columns[3]),
                                             Parameters.of("c_middle", columns[4]),
                                             Parameters.of("c_last", columns[5]),
                                             Parameters.of("c_street_1", columns[6]),
                                             Parameters.of("c_street_2", columns[7]),
                                             Parameters.of("c_city", columns[8]),
                                             Parameters.of("c_state", columns[9]),
                                             Parameters.of("c_zip", columns[10]),
                                             Parameters.of("c_phone", columns[11]),
                                             Parameters.of("c_since", columns[12]),
                                             Parameters.of("c_credit", columns[13]),
                                             Parameters.of("c_credit_lim", Double.parseDouble(columns[14])),
                                             Parameters.of("c_discount", Double.parseDouble(columns[15])),
                                             Parameters.of("c_balance", Double.parseDouble(columns[16])),
                                             Parameters.of("c_data", columns[17])).get();
            } while (true);
            transaction.commit().get();
        } catch (ServerException | InterruptedException e) {
            System.err.println(e);
            throw new IOException(e);
        }
    }
}
