package com.tsurugidb.tsubakuro.examples.tpcc;

public class Profile {
    static final int MUL = 10;
    static final int DIV = 10;
    static final int RESPONSE_TIME_DIV = MUL * DIV + 1;

    static class ResponseTimeDistribution {
        private String name;
        private long[] counters = new long[RESPONSE_TIME_DIV];

        ResponseTimeDistribution(String name) {
            this.name = name;
        }
        void add(ResponseTimeDistribution result) {
            for (int i = 0; i < RESPONSE_TIME_DIV; i++) {
                counters[i] += result.counters[i];
            }
        }
        void put(long t) {
            counters[index(t)]++;
        }
        private int index(long t) {  // ns -> ms
            int i = (int) ((t - 500000) / 1000000);
            if (i < 0) {
                return 0;
            }
            if (i >= RESPONSE_TIME_DIV) {
                return RESPONSE_TIME_DIV - 1;
            }
            return i;
        }
        void print() {
            System.out.printf("response time distribution for %s transaction%n", name);
            for (int i = 0; i < MUL; i++) {
                System.out.printf("%3d-%3d(ms):", i * DIV + 1, (i + 1)  * DIV);
                for (int j = 0; j < DIV; j++) {
                    int idx = i * DIV + j;
                    System.out.printf(" %6d", counters[idx]);
                }
                System.out.printf("%n");
            }
            System.out.printf("%3d(ms)-   : %6d", DIV * MUL + 1, counters[RESPONSE_TIME_DIV - 1]);
        }
    }
    static class Counter {
        long newOrder;
        long payment;
        long orderStatus;
        long delivery;
        long stockLevel;
        
        Counter() {
            this.newOrder = 0;
            this.payment = 0;
            this.orderStatus = 0;
            this.delivery = 0;
            this.stockLevel = 0;
        }
        void add(Counter counter) {
            newOrder += counter.newOrder;
            payment += counter.payment;
            orderStatus += counter.orderStatus;
            delivery += counter.delivery;
            stockLevel += counter.stockLevel;
        }
    }
    
    public long warehouses;
    public long threads;
    public long index;
    public boolean fixThreadMapping;
    public Counter invocation;
    public Counter completion;
    public Counter retryOnStatement;
    public Counter retryOnCommit;
    public Counter error;
    public Counter districtTable;
    public Counter warehouseTable;
    public Counter ordersTable;
    public Counter customerTable;
    public Counter stockTable;
    public long newOrderIntentionalRollback;
    public long elapsed;
    public long count;
    public long inconsistentIndexCount;  // for temporary use
    private Counter time;
    private ResponseTimeDistribution newOrderDistribution;
    private boolean printDistribution;
    
    public Profile(boolean printDist) {
        time = new Counter();
        invocation = new Counter();
        completion = new Counter();
        retryOnStatement = new Counter();
        retryOnCommit = new Counter();
        error = new Counter();
        districtTable = new Counter();
        warehouseTable = new Counter();
        ordersTable = new Counter();
        customerTable = new Counter();
        stockTable = new Counter();
        newOrderDistribution = new ResponseTimeDistribution("new order");
        newOrderIntentionalRollback = 0;
        count = 0;
        printDistribution = printDist;
        inconsistentIndexCount = 0;  // for temporary use
    }
    public Profile() {
        this(false);
    }
    public void add(Profile profile) {
        time.add(profile.time);
        invocation.add(profile.invocation);
        completion.add(profile.completion);
        retryOnStatement.add(profile.retryOnStatement);
        retryOnCommit.add(profile.retryOnCommit);
        error.add(profile.error);
        districtTable.add(profile.districtTable);
        warehouseTable.add(profile.warehouseTable);
        ordersTable.add(profile.ordersTable);
        customerTable.add(profile.customerTable);
        stockTable.add(profile.stockTable);
        newOrderDistribution.add(profile.newOrderDistribution);
        newOrderIntentionalRollback += profile.newOrderIntentionalRollback;
        elapsed += profile.elapsed;
        count++;
        inconsistentIndexCount += profile.inconsistentIndexCount;  // for temporary use
        printDistribution |= profile.printDistribution;
    }
    long ns2us(long t) {
        return (t + 500) / 1000;
    }
    long div(long a, long b) {
        if (b == 0) {
            return a;
        }
        return a / b;
    }
    void addTimeNewOrder(long t) {
        time.newOrder += t;
        newOrderDistribution.put(t);
    }
    void addTimePayment(long t) {
        time.payment += t;
    }
    void addTimeOrderStatus(long t) {
        time.orderStatus += t;
    }
    void addTimeDelivery(long t) {
        time.delivery += t;
    }
    void addTimeStockLevel(long t) {
        time.stockLevel += t;
    }
    public void print(int n) {
        if (inconsistentIndexCount > 0) {  // for temporary use
            System.out.printf("retry due to inconsistent_index: %d times%n%n", inconsistentIndexCount);
        }
        System.out.printf("duration(mS): %d%n", elapsed / count);
        System.out.println("===============================================================================================");
        System.out.printf("   new order: %12d / %8d = %6d (us)%n", ns2us(time.newOrder), completion.newOrder + newOrderIntentionalRollback, ns2us(div(time.newOrder , (completion.newOrder + newOrderIntentionalRollback))));
        System.out.printf("     payment: %12d / %8d = %6d (us)%n", ns2us(time.payment), completion.payment, ns2us(div(time.payment, completion.payment)));
        System.out.printf("order status: %12d / %8d = %6d (us)%n", ns2us(time.orderStatus), completion.orderStatus, ns2us(div(time.orderStatus, completion.orderStatus)));
        System.out.printf("    delivery: %12d / %8d = %6d (us)%n", ns2us(time.delivery), completion.delivery, ns2us(div(time.delivery, completion.delivery)));
        System.out.printf(" stock level: %12d / %8d = %6d (us)%n", ns2us(time.stockLevel), completion.stockLevel, ns2us(div(time.stockLevel, completion.stockLevel)));
        System.out.println("===============================================================================================");
        System.out.println("     tx type: invocation:completion(:intentional rollback) - retry on statement:retry on commit");
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.printf("   new order: %8d:%8d:%8d - %8d:%8d%n", invocation.newOrder, completion.newOrder, newOrderIntentionalRollback, retryOnStatement.newOrder, retryOnCommit.newOrder);
        System.out.printf("     payment: %8d:%8d          - %8d:%8d%n", invocation.payment, completion.payment, retryOnStatement.payment, retryOnCommit.payment);
        System.out.printf("order status: %8d:%8d          - %8d:%8d%n", invocation.orderStatus, completion.orderStatus, retryOnStatement.orderStatus, retryOnCommit.orderStatus);
        System.out.printf("    delivery: %8d:%8d          - %8d:%8d%n", invocation.delivery, completion.delivery, retryOnStatement.delivery, retryOnCommit.delivery);
        System.out.printf(" stock level: %8d:%8d          - %8d:%8d%n", invocation.stockLevel, completion.stockLevel, retryOnStatement.stockLevel, retryOnCommit.stockLevel);
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.printf("   new order: ORDERS %6d DISTRICT %6d WAREHOUSE %6d CUSTOMER %6d STOCK %6d%n",
                          ordersTable.newOrder, districtTable.newOrder, warehouseTable.newOrder, customerTable.newOrder, stockTable.newOrder);
        System.out.printf("     payment: ORDERS %6d DISTRICT %6d WAREHOUSE %6d CUSTOMER %6d STOCK %6d%n",
                          ordersTable.payment, districtTable.payment, warehouseTable.payment, customerTable.payment, stockTable.payment);
        System.out.printf("order status: ORDERS %6d DISTRICT %6d WAREHOUSE %6d CUSTOMER %6d STOCK %6d%n",
                          ordersTable.orderStatus, districtTable.orderStatus, warehouseTable.orderStatus, customerTable.orderStatus, stockTable.orderStatus);
        System.out.printf("    delivery: ORDERS %6d DISTRICT %6d WAREHOUSE %6d CUSTOMER %6d STOCK %6d%n",
                          ordersTable.delivery, districtTable.delivery, warehouseTable.delivery, customerTable.delivery, stockTable.delivery);
        System.out.printf(" stock level: ORDERS %6d DISTRICT %6d WAREHOUSE %6d CUSTOMER %6d STOCK %6d%n",
                          ordersTable.stockLevel, districtTable.stockLevel, warehouseTable.stockLevel, customerTable.stockLevel, stockTable.stockLevel);
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.printf("##NoTPM=%.2f%n", ((double) completion.newOrder * 60.0 * 1000.0) / ((double) elapsed / (double) n));
        if (printDistribution) {
            System.out.printf("\n");
            newOrderDistribution.print();
        }
    }
}
