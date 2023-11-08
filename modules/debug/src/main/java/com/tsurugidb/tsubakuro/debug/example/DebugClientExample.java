package com.tsurugidb.tsubakuro.debug.example;

import java.net.URI;

import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.debug.DebugClient;
import com.tsurugidb.tsubakuro.debug.LogLevel;

/**
 * Example program to test DebugClient
 */
public class DebugClientExample {

    private final URI endpoint;
    private final String message;

    DebugClientExample(String[] args) {
        this.endpoint = URI.create(System.getProperty("tsurugi.dbname", "ipc:tsurugi"));
        this.message = args[0];
    }

    private void output() throws Exception {
        try (var session = SessionBuilder.connect(endpoint).withCredential(NullCredential.INSTANCE).create();
                var debug = DebugClient.attach(session)) {
            debug.logging(message);
            debug.logging(LogLevel.INFO, message);
            debug.logging(LogLevel.WARN, message);
            debug.logging(LogLevel.ERROR, message);
        }
    }

    /**
     * @param args command arguments
     * args[0] message to be shown in server log.
     * The message will be shown 4 times at default log level(info), info, warn, and error.
     * @throws Exception some exceptional situation occurred
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java [-Dtsurugi.dbname=name] DebugClientExample [message]");
            System.err.println("\tex\tjava DebugClientExample hello");
            System.err.println("\tex\tjava -Dtsurugi.dbname=ipc:tsurugi DebugClientExample hello");
            return;
        }
        DebugClientExample app = new DebugClientExample(args);
        app.output();
    }

}
