package com.tsurugidb.tsubakuro.examples.resultsetClose;

// for command options
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.util.ArrayList;

import com.tsurugidb.tsubakuro.exception.ServerException;

public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");
    private static boolean testResponse = false;

    public static void main(String[] args) {
        // コマンドラインオプションの設定
        Options options = new Options();

        options.addOption(Option.builder("z").argName("prepare").desc("do simple test.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        if (cmd.hasOption("i")) {
            testResponse = true;
        }

        try {
            CreateTable.doCreateTable(url);
        } catch (IOException | ServerException | InterruptedException e) {
            System.out.println(e);
            e.printStackTrace();
            return;
        }
        try {
            Insert.doInsert(url, testResponse);
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
