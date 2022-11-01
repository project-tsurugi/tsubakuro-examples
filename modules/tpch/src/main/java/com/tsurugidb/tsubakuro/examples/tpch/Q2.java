package com.tsurugidb.tsubakuro.examples.tpch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.Placeholders;
import com.tsurugidb.tsubakuro.sql.Parameters;

public class Q2 {
    SqlClient sqlClient;
    PreparedStatement prepared1;
    PreparedStatement prepared2;
    static final int PARTKEY_SIZE = 200000;
    Map<Integer, Long> q2intermediate;

    public Q2(SqlClient sqlClient) throws IOException, ServerException, InterruptedException {
        this.sqlClient = sqlClient;
        this.q2intermediate = new HashMap<>();
        prepare();
    }

    public void prepare() throws IOException, ServerException, InterruptedException {
        String sql1 = "SELECT MIN(PS_SUPPLYCOST) "
                + "FROM PARTSUPP, SUPPLIER, NATION, REGION "
                + "WHERE "
                + "PS_SUPPKEY = S_SUPPKEY "
                + "AND S_NATIONKEY = N_NATIONKEY "
                + "AND N_REGIONKEY = R_REGIONKEY "
                + "AND R_NAME = :region "
                + "AND PS_PARTKEY = :partkey ";
        prepared1 = sqlClient.prepare(sql1,
            Placeholders.of("region", String.class),
            Placeholders.of("partkey", long.class)).get();

        String sql2 = "SELECT S_ACCTBAL, S_NAME, N_NAME, P_MFGR, S_ADDRESS, S_PHONE, S_COMMENT "
                + "FROM PART, SUPPLIER, PARTSUPP, NATION, REGION "
                + "WHERE "
                + "S_SUPPKEY = PS_SUPPKEY "
                + "AND S_NATIONKEY = N_NATIONKEY "
                + "AND N_REGIONKEY = R_REGIONKEY "
                + "AND PS_PARTKEY = :partkey "
                + "AND P_SIZE = :size "
                + "AND P_TYPE3 = :type "
                + "AND R_NAME = :region "
                + "AND PS_SUPPLYCOST = :mincost "
                + "ORDER BY S_ACCTBAL DESC, N_NAME, S_NAME, P_PARTKEY";
        prepared2 = sqlClient.prepare(sql2,
            Placeholders.of("partkey", long.class),
            Placeholders.of("size", long.class),
            Placeholders.of("type", String.class),
            Placeholders.of("region", String.class),
            Placeholders.of("mincost", long.class)).get();
    }

    void q21(boolean qvalidation, Transaction transaction) throws IOException, ServerException, InterruptedException {
        for (int partkey = 1; partkey <= PARTKEY_SIZE; partkey++) {
            var future = transaction.executeQuery(prepared1,
                Parameters.of("region", qvalidation ? "EUROPE                   " : "ASIA                     "),
                Parameters.of("partkey", (long) partkey));

            try (var resultSet = future.get()) {
                if (resultSet.nextRow()) {
                    resultSet.nextColumn();
                    if (!resultSet.isNull()) {
                        q2intermediate.put(partkey, resultSet.fetchInt8Value());
                    }
                } else {
                    throw new IOException("no record");
                }
            } catch (ServerException e) {
                throw new IOException(e);
            }
        }
    }

    void q22(boolean qvalidation, Transaction transaction) throws IOException, ServerException, InterruptedException {
        for (Map.Entry<Integer, Long> entry : q2intermediate.entrySet()) {
            int partkey = entry.getKey();
            var future = transaction.executeQuery(prepared2,
                Parameters.of("type", qvalidation ? "BRASS" : "STEEL"),
                Parameters.of("region", qvalidation ? "EUROPE                   " : "ASIA                     "),
                Parameters.of("size", (long) (qvalidation ? 15 : 16)),
                Parameters.of("partkey", (long) partkey),
                Parameters.of("mincost", (long) entry.getValue()));

            try (var resultSet = future.get()) {
                if (resultSet.nextRow()) {
                    resultSet.nextColumn();
                    var sAcctbal = resultSet.fetchInt8Value();
                    resultSet.nextColumn();
                    var sName = resultSet.fetchCharacterValue();
                    resultSet.nextColumn();
                    var nName = resultSet.fetchCharacterValue();
                    resultSet.nextColumn();
                    var pMfgr = resultSet.fetchCharacterValue();
                    resultSet.nextColumn();
                    var sAddress = resultSet.fetchCharacterValue();
                    resultSet.nextColumn();
                    var sPhone = resultSet.fetchCharacterValue();
                    resultSet.nextColumn();
                    var sCommnent = resultSet.fetchCharacterValue();

                    System.out.println(sAcctbal + "," + sName + "," + nName + "," + partkey + "," + pMfgr + "," + sAddress + "," + sPhone + "," + sCommnent);
                }
            } catch (ServerException e) {
                throw new IOException(e);
            }
        }
    }

    public void run21(Profile profile) throws IOException, ServerException, InterruptedException {
        long start = System.currentTimeMillis();
        var transaction = sqlClient.createTransaction(profile.transactionOption.build()).get();

        q21(profile.queryValidation, transaction);
        try {
            transaction.commit().get();
        } catch (ServerException e) {
            throw new IOException(e);
        } finally {
            profile.q21 = System.currentTimeMillis() - start;
        }
    }

    public void run2(Profile profile) throws IOException, ServerException, InterruptedException {
        long start = System.currentTimeMillis();
        var transaction = sqlClient.createTransaction(profile.transactionOption.build()).get();

        q21(profile.queryValidation, transaction);
        q22(profile.queryValidation, transaction);

        try {
            transaction.commit().get();
        } catch (ServerException e) {
            throw new IOException(e);
        } finally {
            profile.q22 = System.currentTimeMillis() - start;
        }
    }
}
