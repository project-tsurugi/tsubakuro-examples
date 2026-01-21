package com.tsurugidb.tsubakuro.examples.datastore;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.datastore.DatastoreClient;
import com.tsurugidb.tsubakuro.datastore.BackupType;

public final class RegisterTransactionTpmId {
    private RegisterTransactionTpmId(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    public static void main(String[] args) {
        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             DatastoreClient client = DatastoreClient.attach(session);) {

             client.registerTransactionTpmId("TID-11223344556677889900", 12345678L).get();

        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            System.out.println(e);
        }
    }
}
