package com.tsurugidb.tsubakuro.kvs.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tsurugidb.tsubakuro.kvs.KvsClient;
import com.tsurugidb.tsubakuro.kvs.KvsServiceCode;
import com.tsurugidb.tsubakuro.kvs.KvsServiceException;
import com.tsurugidb.tsubakuro.kvs.RecordBuffer;
import com.tsurugidb.tsubakuro.kvs.util.Utils;

class SecondaryIndexTableTest {

    private static final String TABLE_NAME = "table" + SecondaryIndexTableTest.class.getSimpleName();
    private static final String KEY_NAME = "k1";
    private static final String VALUE_NAME = "v1";

    @BeforeAll
    static void setup() throws Exception {
        String schema = String.format("%s BIGINT PRIMARY KEY, %s BIGINT", KEY_NAME, VALUE_NAME);
        Utils.createTable(TABLE_NAME, schema);
        String sql = String.format("CREATE INDEX %s on %s(%s)", "index" + VALUE_NAME, TABLE_NAME, VALUE_NAME);
        Utils.executeStatement(sql);
    }

    @Test
    public void basicPutGetRemove() throws Exception {
        final long key1 = 1L;
        final long value1 = 100L;
        try (var session = Utils.getNewSession(); var kvs = KvsClient.attach(session)) {
            try (var tx = kvs.beginTransaction().await()) {
                RecordBuffer buffer = new RecordBuffer();
                buffer.add(KEY_NAME, key1);
                buffer.add(VALUE_NAME, value1);
                KvsServiceException ex = assertThrows(KvsServiceException.class, () -> {
                    kvs.put(tx, TABLE_NAME, buffer).await();
                });
                assertEquals(KvsServiceCode.NOT_IMPLEMENTED, ex.getDiagnosticCode());
                System.err.println(Utils.getLineNumber() + "\t" + ex.getMessage());
                kvs.rollback(tx).await();
            }
            RecordBuffer keyBuffer = new RecordBuffer();
            keyBuffer.add(KEY_NAME, key1);
            try (var tx = kvs.beginTransaction().await()) {
                var get = kvs.get(tx, TABLE_NAME, keyBuffer).await();
                kvs.commit(tx).await();
                assertEquals(get.size(), 0);
                assertEquals(get.isEmpty(), true);
                assertEquals(get.isSingle(), false);
            }
            try (var tx = kvs.beginTransaction().await()) {
                KvsServiceException ex = assertThrows(KvsServiceException.class, () -> {
                    kvs.remove(tx, TABLE_NAME, keyBuffer).await();
                });
                assertEquals(KvsServiceCode.NOT_IMPLEMENTED, ex.getDiagnosticCode());
                System.err.println(Utils.getLineNumber() + "\t" + ex.getMessage());
                kvs.rollback(tx).await();
            }
        }
   }
}
