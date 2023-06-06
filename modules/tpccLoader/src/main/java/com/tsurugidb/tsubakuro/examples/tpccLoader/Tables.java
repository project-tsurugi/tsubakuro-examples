package com.tsurugidb.tsubakuro.examples.tpccLoader;

final class Tables {

    static private final String itemTable = "ITEM";
    static private final String[] tables = {
        "WAREHOUSE",
        "DISTRICT",
        "CUSTOMER",
        "ORDERS",
        "NEW_ORDER",
        "ORDER_LINE",
        "STOCK",
        "HISTORY"
    };
    
    private Tables() {
    }

    static String itemTable() {
        return itemTable;
    }
    static String[] tables() {
        return tables;
    }
}
