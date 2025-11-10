package com.tsurugidb.tsubakuro.examples.applicationExit;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.common.ShutdownType;

public class ExitWithActiveSessionTsubakuro {
    private static String url = System.getProperty("tsurugi.dbname");

    public static void main(String[] args) throws Exception {
        var session = SessionBuilder.connect(url).create();
        session.shutdown(ShutdownType.FORCEFUL);
    }
}
