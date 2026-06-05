package com.tsurugidb.tsubakuro.examples.handshakeBlob;

import java.util.concurrent.TimeUnit;

import com.tsurugidb.tsubakuro.common.BlobTransferMedium;
import com.tsurugidb.tsubakuro.common.BlobTransferType;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;

public final class Main {
    private Main(String[] args) {
    }

    // set dbname as follows
    // -Ptsurugi.dbname=ipc:tsurugi or
    // -Ptsurugi.dbname=tcp://localhost:12345
    // when run this example by `./gradlew run` command
    private static String url = System.getProperty("tsurugi.dbname");
    private static long timeout = 500;  // milliseconds

    public static void main(String[] args) {
        checkBlobTransferType(BlobTransferType.DOES_NOT_USE);
        checkBlobTransferType(BlobTransferType.DEFAULT);
        checkBlobTransferType(BlobTransferType.RELAY);
        checkBlobTransferType(BlobTransferType.PRIVILEGED);
    }

    static void checkBlobTransferType(BlobTransferType type) {
        System.out.println("==== check " + type);
        try (
             Session session = SessionBuilder.connect(url)
             .withBlobTransfer(type)
             .create(timeout, TimeUnit.MILLISECONDS)) {
                
            var blobTransferMedium = session.getBlobTransferMedium();
            System.out.println("success, returns " + blobTransferMedium.getBlobTransferType());
        } catch (Exception e) {
            System.out.println("fail:" + e);
        }
    }
}
