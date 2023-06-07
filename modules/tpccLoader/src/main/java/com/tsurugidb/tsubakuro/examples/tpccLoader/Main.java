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
    private static int warehouses = 1;
    private static String rootDirectory = "db";
    
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
        } catch (ParseException e) {
            System.err.println("cmd parser failed." + e);
        }
    }

    public static void main(String[] args) {
        parseArguments(args);
        
        try {
            tasks.add(itemTable, new CsvReader(rootDirectory, itemTable.tableName(), 1));
        } catch (IOException e) {
            System.out.printf("can't find data files in %s", rootDirectory);
            return;
        }
        long index = 1;
        try {
            for (index = 1; index <= warehouses; index++) {
                for (var table : tables) {
                    tasks.add(table, new CsvReader(rootDirectory, table.tableName(), index));
                }
            }
        } catch (IOException e) {
            System.out.printf("csv files for index %d does not exist, so limit to %d.", index, index - 1);
        }

        boolean exhausted = false;
        while (!exhausted) {
            try {
                Worker w = new Worker(url, tasks);
                workers.add(w);
            } catch (IOException e) {
                exhausted = true;
            }
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
