package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;

class CreateTableWorker {
    private final String url;

    CreateTableWorker(String url) throws IOException {
        this.url = url;
    }

    void createTables(TableAccessor item, TableAccessor[] tables) throws IOException {
        try (var session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             var sqlClient = SqlClient.attach(session);
             var transaction = sqlClient.createTransaction().get();) {
            item.createTable(transaction);
            for (var table : tables) {
                table.createTable(transaction);
            }
            transaction.commit();
        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
    }
}
