package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;

class Worker extends Thread {
    private final Session session;
    private final SqlClient sqlClient;
    private final Tasks tasks;
    private boolean success;
    
    Worker(String url, Tasks tasks) throws IOException {
        try {
            session = SessionBuilder.connect(url)
                .withCredential(new UsernamePasswordCredential("user", "pass"))
                .create(10, TimeUnit.SECONDS);
            sqlClient = SqlClient.attach(session);
            this.tasks = tasks;
        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    public void run() {
        try {
            while (true) {
                var entry = tasks.poll();
                if (entry == null) {
                    success = true;
                    return;
                }
                entry.tableAccessor().insert(sqlClient, entry.csvReader());
            }
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
            success = false;
            return;
        } finally {
            try {
                sqlClient.close();
                session.close();
            } catch (IOException | ServerException | InterruptedException e) {
                System.err.println(e);
                e.printStackTrace();
                success = false;
            }
        }
    }

    boolean success() {
        return success;
    }
}
