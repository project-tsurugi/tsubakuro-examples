package com.tsurugidb.tsubakuro.examples.multipleSelect;

import java.io.IOException;
import java.util.ArrayList;

import com.tsurugidb.tsubakuro.exception.ServerException;

public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");

    private static class Selector extends Thread {
        public void run() {
            try {
                Select.doSelect(url);
            } catch (IOException | ServerException | InterruptedException e) {
                System.out.println(e);
                e.printStackTrace();
                return;
            }
        }
    }
        
    public static void main(String[] args) {
        try {
            CreateTable.doCreateTable(url);
        } catch (IOException | ServerException | InterruptedException e) {
            System.out.println(e);
            e.printStackTrace();
            return;
        }
        try {
            Insert.doInsert(url);
        } catch (IOException | ServerException | InterruptedException e) {
            System.out.println(e);
            e.printStackTrace();
            return;
        }
        ArrayList<Selector> selectors = new ArrayList<>();
        int sessions = 1;
        if (args.length > 0) {
            sessions = Integer.parseInt(args[0]);
        }
        for (int i = 0; i < sessions; i++) {
            var selector = new Selector();
            selectors.add(selector);
            selector.start();
        }
        for ( var e : selectors ) {
            try {
                e.join();
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
        }
    }

    private Main(String[] args) {
    }
}
