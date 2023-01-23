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

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tateyama or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");

    public static void main(String[] args) {
        try (
             Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS);
             DatastoreClient client = DatastoreClient.attach(session);
             var backup = client.beginBackup(BackupType.STANDARD).await();) {

            while (true) {
                // 次のファイルセットを取り出す
                var block = backup.nextEntries();

                // 末尾まで読んだら完了
                if (block == null) {
                    break;
                }

                // 全てのファイルをバックアップする
                for (var entry : block) {
                    System.out.println("from: " + entry.getSourcePath() + ", to: " + entry.getDestinationPath() + ", mutable : " + entry.isMutable() + ", detached : " + entry.isDetached());
                }

            }
        } catch (IOException | ServerException | InterruptedException | TimeoutException e) {
            System.out.println(e);
        }
    }
}
