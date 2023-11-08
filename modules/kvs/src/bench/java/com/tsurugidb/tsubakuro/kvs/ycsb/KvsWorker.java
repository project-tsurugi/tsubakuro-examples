package com.tsurugidb.tsubakuro.kvs.ycsb;

import java.net.URI;

import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.kvs.KvsClient;
import com.tsurugidb.tsubakuro.kvs.PutType;
import com.tsurugidb.tsubakuro.kvs.RecordBuffer;
import com.tsurugidb.tsubakuro.kvs.util.RunManager;

/**
 * benchmark worker using KvsClient
 */
public class KvsWorker extends Worker {

    KvsWorker(RunManager mgr, URI endpoint, int numClient, int clientId, int rratio) throws Exception {
        super(mgr, endpoint, numClient, clientId, rratio);
    }

    @Override
    public Long benchmark() throws Exception {
        long numTx = 0;
        try (var session = SessionBuilder.connect(endpoint).withCredential(NullCredential.INSTANCE).create();
            var kvs = KvsClient.attach(session)) {
            mgr.addReadyWorker();
            mgr.waitUntilWorkerStartTime();
            int optId = 0;
            while (!mgr.isQuit()) {
                try (var tx = kvs.beginTransaction().await()) {
                    for (int i = 0; i < Constants.OPS_PER_TX; i++, optId++) {
                        var op = operations.get(optId % operations.size());
                        RecordBuffer buffer = new RecordBuffer();
                        buffer.add(Constants.KEY_NAME, Long.valueOf(op.key()));
                        if (op.isGet()) {
                            kvs.get(tx, tableName, buffer).await();
                        } else {
                            buffer.add(Constants.VALUE_NAME, Long.valueOf(100L * i));
                            kvs.put(tx, tableName, buffer, PutType.IF_PRESENT).await();
                        }
                    }
                    kvs.commit(tx).await();
                }
                numTx++;
            }
        }
        return Long.valueOf(numTx);
    }

}
