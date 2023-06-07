package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CsvReader {

    private final File file;
    private final BufferedReader br;

    CsvReader(String root, String tableName, long index) throws IOException {
        this.file = Paths.get(root, tableName, Long.valueOf(index).toString() + ".csv").toFile();
        if (!file.exists()) {
            throw new IOException("file does not exist");
        }
        this.br = new BufferedReader(new FileReader(file));
    }

    String[] getContents() throws IOException {
        String line = br.readLine();
        if (line == null) {
            return null;
        }
        return line.split(",");
    }
}
