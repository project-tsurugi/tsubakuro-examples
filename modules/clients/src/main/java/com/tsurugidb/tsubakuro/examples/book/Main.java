package com.tsurugidb.tsubakuro.examples.book;

import java.io.IOException;

import com.tsurugidb.tsubakuro.exception.ServerException;

public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");

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
        try {
            Select.doSelect(url);
        } catch (IOException | ServerException | InterruptedException e) {
            System.out.println(e);
            e.printStackTrace();
            return;
        }
    }

    private Main(String[] args) {
    }
}
