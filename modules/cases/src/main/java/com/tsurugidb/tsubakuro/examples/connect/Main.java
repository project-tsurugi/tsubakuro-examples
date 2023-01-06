package com.tsurugidb.tsubakuro.examples.connect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.channel.common.connection.Connector;

public final class Main {
    private Main(String[] args) {
    }

    public static void main(String[] args) {
        tsubakuro();
        System.out.println("tsubakuro done");
    }

    private static void tsubakuro() {
        var endpoint = "tcp://localhost:12345";
        System.out.println("endpoint=" + endpoint);
        var connector = Connector.create(endpoint);

        var sessionList = new CopyOnWriteArrayList<Session>();
        var threadList = new ArrayList<Thread>();
        var alive = new AtomicBoolean(true);
        for (int i = 0; i < 60; i++) {
            var thread = new Thread(() -> {
                Session session;
                try {
                    session = SessionBuilder.connect(connector).create();
                } catch (Exception e) {
                    System.out.println("connect error. " + e.getClass().getName() + ":" + e.getMessage());
                    return;
                }
                System.out.println("session created. " + session);
                sessionList.add(session);

                while (alive.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            threadList.add(thread);
            thread.start();
        }

        alive.set(false);

        System.out.println("thread join start");
        for (var thread : threadList) {
            try {
                thread.join();
            } catch (Exception e) {
                System.out.println("thread join error" + e);
            }
        }
        System.out.println("thread join end");

        System.out.println("close session start");
        for (var session : sessionList) {
            try {
                session.close();
            } catch (Exception e) {
                System.out.println("close error" + e);
            }
        }
        System.out.println("close session end");
    }
}
