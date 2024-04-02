package com.tsurugidb.tsubakuro.examples.cancel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;

public class Insert {
    final String url;
    SqlClient sqlClient;
    Session session;

    public Insert(String url) throws IOException, ServerException, InterruptedException, TimeoutException {
        this.url = url;
        session = SessionBuilder.connect(url)
            .withCredential(new UsernamePasswordCredential("user", "pass"))
            .create(10, TimeUnit.SECONDS);
        sqlClient = SqlClient.attach(session);
    }

    public void createTable() throws IOException, ServerException, InterruptedException {
        String createTable = "CREATE TABLE ORDERS (o_id BIGINT NOT NULL, o_d_id BIGINT NOT NULL, o_w_id BIGINT NOT NULL, o_c_id BIGINT NOT NULL, o_entry_d CHAR(25) NOT NULL, o_carrier_id BIGINT, o_ol_cnt BIGINT NOT NULL, o_all_local BIGINT NOT NULL, PRIMARY KEY(o_w_id, o_d_id, o_id))";
        String createIndex = "CREATE INDEX ORDERS_SECONDARY ON ORDERS (o_w_id, o_d_id, o_c_id, o_id)";
        try (Transaction transaction = sqlClient.createTransaction().await()) {
            try {
                transaction.executeStatement(createTable).get();
                transaction.executeStatement(createIndex).get();
                transaction.commit().get();
            } catch (ServerException e) {
                transaction.rollback().get();
            }
        }
    }

    public void prepareAndInsert() throws IOException, ServerException, InterruptedException {
        String sql = "INSERT INTO ORDERS (o_id, o_d_id, o_w_id, o_c_id, o_entry_d, o_carrier_id, o_ol_cnt, o_all_local) VALUES (:o_id, :o_d_id, :o_w_id, :o_c_id, :o_entry_d, :o_carrier_id, :o_ol_cnt, :o_all_local)";
        try (var preparedStatement = sqlClient.prepare(sql,
                                                       Placeholders.of("o_id", long.class),
                                                       Placeholders.of("o_d_id", long.class),
                                                       Placeholders.of("o_w_id", long.class),
                                                       Placeholders.of("o_c_id", long.class),
                                                       Placeholders.of("o_entry_d", String.class),
                                                       Placeholders.of("o_carrier_id", long.class),
                                                       Placeholders.of("o_ol_cnt", long.class),
                                                       Placeholders.of("o_all_local", long.class)).get();

             Transaction transaction = sqlClient.createTransaction().await()) {

            try {
                var result = transaction.executeStatement(preparedStatement,
                    Parameters.of("o_id", (long) 99999999),
                    Parameters.of("o_d_id", (long) 3),
                    Parameters.of("o_w_id", (long) 1),
                    Parameters.of("o_c_id", (long) 1234),
                    Parameters.of("o_entry_d", "20210620"),
                    Parameters.of("o_carrier_id", (long) 3),
                    Parameters.of("o_ol_cnt", (long) 7),
                    Parameters.of("o_all_local", (long) 0));
                result.close();
                transaction.commit().get();
            } catch (ServerException e) {
                transaction.rollback().get();
            }
        }
    }
    public void insertByText() throws IOException, ServerException, InterruptedException {
        //        String sql = "INSERT INTO ORDERS (o_id, o_d_id, o_w_id, o_c_id, o_entry_d, o_carrier_id, o_ol_cnt, o_all_local) VALUES (99999999, 3, 1, 5678, '20221120', 2, 6, 0)";
        String sql = "INSERT INTO ORDERS VALUES (99999999, 3, 1, 5678, '20221120', 2, 6, 0)";
        try (Transaction transaction = sqlClient.createTransaction().await()) {
            try {
                var result = transaction.executeStatement(sql).get(100, TimeUnit.SECONDS);
                transaction.commit().get();
                //            } catch (ServerException | TimeoutException e) {
            } catch (ServerException | TimeoutException e) {
                System.out.println(e);
                transaction.rollback().get();
            }
        }
    }

    public void close() throws IOException, ServerException, InterruptedException {
        session.close();
    }
}
