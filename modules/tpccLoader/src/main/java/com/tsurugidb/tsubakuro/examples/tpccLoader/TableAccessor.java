package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;

import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;

interface TableAccessor {
    String tableName();
    void createTable(Transaction transaction) throws IOException;
    void insert(SqlClient client, CsvReader csvReader) throws IOException;
}
