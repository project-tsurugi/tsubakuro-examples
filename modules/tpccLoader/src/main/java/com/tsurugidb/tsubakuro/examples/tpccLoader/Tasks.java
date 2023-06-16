package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.util.concurrent.ConcurrentLinkedQueue;


class Tasks {
    private final ConcurrentLinkedQueue<Entry> queue = new ConcurrentLinkedQueue<>();

    class Entry {
        private final TableAccessor tableAccessor;
        private final CsvReader csvReader;
        Entry(TableAccessor tableAccessor, CsvReader csvReader) {
            this.tableAccessor = tableAccessor;
            this.csvReader = csvReader;
        }
        TableAccessor tableAccessor() {
            return tableAccessor;
        }
        CsvReader csvReader() {
            return csvReader;
        }
    }

    Tasks() {
    }

    void add(TableAccessor tableAccessor, CsvReader csvReader) {
        queue.add(new Entry(tableAccessor, csvReader));
    }
    Entry poll() {
        return queue.poll();
    }
}
