package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;

public final class Main {
    private static String url = System.getProperty("tsurugi.dbname");
    private static int warehouses = Integer.MAX_VALUE;
    private static String rootDirectory = "db";
    private static boolean verbose = false;
    private static int maxParallelism = 0;

    private static final TableAccessor itemTable = new ItemAccessor();
    private static final TableAccessor[] tables = {
        new WarehouseAccessor(),
        new DistrictAccessor(),
        new CustomerAccessor(),
        new OrdersAccessor(),
        new NewOrderAccessor(),
        new OrderLineAccessor(),
        new StockAccessor(),
        new HistoryAccessor()
    };
    private static Tasks tasks = new Tasks();
    private static List<Worker> workers = new ArrayList<>();

    private Main() {
    }

    private static void parseArguments(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("w").argName("warehouses").hasArg().desc("The number of warehouse.").build());
        options.addOption(Option.builder("d").argName("root_directory").hasArg().desc("root directory of table data.").build());
        options.addOption(Option.builder("v").argName("verbose").desc("print verbose message.").build());
        options.addOption(Option.builder("p").argName("parallelism").hasArg().desc("max worker threads.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("w")) {
                warehouses = Integer.parseInt(cmd.getOptionValue("w"));
            }
            if (cmd.hasOption("d")) {
                rootDirectory = cmd.getOptionValue("d");
            }
            if (cmd.hasOption("v")) {
                verbose = true;
            }
            if (cmd.hasOption("p")) {
                maxParallelism = Integer.parseInt(cmd.getOptionValue("p"));
            }
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }
    }

    public static void main(String[] args) {
        parseArguments(args);

        try {
            tasks.add(itemTable, new CsvReader(rootDirectory, itemTable.tableName(), 1, verbose));
        } catch (IOException e) {
            System.out.printf("can't find data files in %s", rootDirectory);
            return;
        }

        try {
            var createTableWorker = new CreateTableWorker(url);
            createTableWorker.createTables(itemTable, tables);
        } catch (IOException e) {
            System.err.println("something wrong in creating TPC-C tables");
            System.err.println(e);
            return;
        }            

        int index = 1;
        try {
            for (index = 1; index <= warehouses; index++) {
                for (var table : tables) {
                    tasks.add(table, new CsvReader(rootDirectory, table.tableName(), index, verbose));
                }
            }
        } catch (IOException e) {
            if (warehouses != Integer.MAX_VALUE) {
                System.out.printf("csv files for index %d does not exist, so limit to %d.%n", index, index - 1);
            } else if (verbose) {
                System.out.printf("the number of warehouses is %d.%n", index - 1);
            }
            warehouses = index - 1;
        }

        if (maxParallelism <= 0) {
            boolean exhausted = false;
            while (!exhausted) {
                try {
                    Worker w = new Worker(url, tasks);
                    workers.add(w);
                } catch (IOException e) {
                    exhausted = true;
                }
            }
        } else {
            for (int i = 1; i <= maxParallelism; i++) {
                try {
                    Worker w = new Worker(url, tasks);
                    workers.add(w);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (verbose) {
            System.out.printf("the number of workers is %d.%n", workers.size());
        }

        for (var w : workers) {
            w.start();
        }

        boolean success = true;
        for (var w : workers) {
            try {
                w.join();
                success &= w.success();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        if (!success) {
            System.err.println("something wrong in loading TPC-C initial data");
        }
    }
}
