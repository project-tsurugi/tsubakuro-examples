package com.tsurugidb.tsubakuro.examples.tpccLoader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static List<CsvReader> list = new ArrayList<CsvReader>();
    private static int warehouses = 1;
    private static String rootDirectory = "db";
    
    private Main() {
    }

    public static void main(String[] args) {
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
        
        try {
            list.add( new CsvReader( rootDirectory, Tables.itemTable(), 1) );
        } catch (IOException e) {
            System.out.printf("can't find data files in %s", rootDirectory);
            return;
        }
        
        long index = 1;
        try {
            for (index = 1; index <= warehouses; index++ ) {
                for (String table : Tables.tables()) {
                    list.add( new CsvReader( rootDirectory, table, index) );
                }
            }
        } catch (IOException e) {
            System.out.printf("csv files for index %d does not exist, so limit to %d.", index, index - 1);
        }
        try {
            for (var csvReader : list) {
                csvReader.run();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
