package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CsvReader {

    private final File file;

    CsvReader(String root, String tableName, long index) throws IOException {
        this.file = Paths.get(root, tableName, Long.valueOf(index).toString() + ".csv").toFile();
        if (!file.exists()) {
            throw new IOException( "file does not exist");
        }
    }

    void run() throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), Charset.forName("US-ASCII"));
        for (int i = 1; i < lines.size(); i++) {
            String[] data = lines.get(i).split(",");
            for (var item : data) {
                System.out.print(item + ",");
            }
            System.out.println();
        }
    }
}
