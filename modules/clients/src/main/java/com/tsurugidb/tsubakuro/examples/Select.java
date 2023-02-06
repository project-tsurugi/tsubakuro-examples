package com.tsurugidb.tsubakuro.examples;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;

public class Select {
    final String url;
    final int loopCount;
    final int selectCount;
    final int threadCount;
    final boolean suppressDisplay;
    final int sleepSeconds;
    final long timeout;

    static class ThreadForSelect extends Thread {
        final String url;
        final int id;
        final int selectCount;
        final boolean suppressDisplay;
        final int sleepSeconds;
        final long timeout;

        ThreadForSelect(String url, int id, int selectCount, boolean suppressDisplay, int sleepSeconds, long timeout) {
            this.url = url;
            this.id = id;
            this.selectCount = selectCount;
            this.suppressDisplay = suppressDisplay;
            this.sleepSeconds = sleepSeconds;
            this.timeout = timeout;
        }
        public void run() {
            String sql = "SELECT * FROM ORDERS WHERE o_w_id = :o_w_id AND o_d_id = :o_d_id AND o_id = :o_id";
            try (
                 Session session = SessionBuilder.connect(url)
                 .withCredential(new UsernamePasswordCredential("user", "pass"))
                 .create(10, TimeUnit.SECONDS);
                 SqlClient sqlClient = SqlClient.attach(session);
                 var preparedStatement = sqlClient.prepare(sql,
                     Placeholders.of("o_id", long.class),
                     Placeholders.of("o_d_id", long.class),
                     Placeholders.of("o_w_id", long.class)).get(timeout, TimeUnit.MILLISECONDS);

                var transaction = sqlClient.createTransaction().get(timeout, TimeUnit.MILLISECONDS)) {

                for (int i = 0; i < selectCount; i++) {
                    try (var resultSet = transaction.executeQuery(preparedStatement,
                                                                  Parameters.of("o_id", (long) 99999999),
                                                                  Parameters.of("o_d_id", (long) 3),
                                                                  Parameters.of("o_w_id", (long) 1)).get(timeout, TimeUnit.MILLISECONDS)) {
                        if (!suppressDisplay) {
                            printResultset(resultSet);
                        }
                    }
                }
                transaction.commit().await();

                if (sleepSeconds > 0) {
                    Thread.sleep(sleepSeconds * 1000);
                }

            } catch (ServerException | InterruptedException | IOException | TimeoutException e) {
                System.err.println(e);
                e.printStackTrace();
                return;
            }
        }

        void printResultset(ResultSet resultSet) throws InterruptedException, IOException, ServerException {
            int count = 1;

            while (resultSet.nextRow()) {
                System.out.println("---- ( id: " + id + " count: " + count + " )----");
                count++;
                int columnIndex = 0;
                var metadata = resultSet.getMetadata().getColumns();
                while (resultSet.nextColumn()) {
                    if (!resultSet.isNull()) {
                        switch (metadata.get(columnIndex).getAtomType()) {
                            case INT4:
                                System.out.println(resultSet.fetchInt4Value());
                                break;
                            case INT8:
                                System.out.println(resultSet.fetchInt8Value());
                                break;
                            case FLOAT4:
                                System.out.println(resultSet.fetchFloat4Value());
                                break;
                            case FLOAT8:
                                System.out.println(resultSet.fetchFloat8Value());
                                break;
                            case CHARACTER:
                                System.out.println(resultSet.fetchCharacterValue());
                                break;
                            default:
                                throw new IOException("the column type is invalid");
                        }
                    } else {
                        System.out.println("the column is NULL");
                    }
                    columnIndex++;
                }
            }
        }
    }

    public Select(String url, int loopCount, int selectCount, int threadCount, boolean suppressDisplay, int sleepSeconds, long timeout) throws IOException, ServerException, InterruptedException {
        this.url = url;
        this.loopCount = loopCount;
        this.selectCount = selectCount;
        this.threadCount = threadCount;
        this.suppressDisplay = suppressDisplay;
        this.sleepSeconds = sleepSeconds;
        this.timeout = timeout;
    }

    public void prepareAndSelect() throws IOException, ServerException, InterruptedException {
        Thread[] threads = new Thread[threadCount];
        for (int j = 0; j < loopCount; j++) {
            for (int i = 0; i < threadCount; i++) {
                threads[i] =  new ThreadForSelect(url, i, selectCount, suppressDisplay, sleepSeconds, timeout);
                threads[i].start();
            }
            for (int i = 0; i < threadCount; i++) {
                threads[i].join();
            }
        }
    }
}
