#include <iostream>
#include <array>
#include <thread>
#include <exception>

#include <gflags/gflags.h>
#include <boost/filesystem.hpp>

#include "tpcc_common.h"
#include "tpcc_schema.h"
#include "csv_file.h"

DECLARE_bool(display_progress);  //NOLINT
DECLARE_bool(f);  //NOLINT

namespace tpcc {

/* Functions */
int generate_Items(boost::filesystem::path); // item
int generate_Ware(boost::filesystem::path, uint32_t); // warehouse, Stock(), District(), thread
int generate_Ord(boost::filesystem::path, uint32_t); // Orders() for all districts, thread, use no randomGeneratorClass
int generate_Cust(boost::filesystem::path, uint32_t); // Customer() for all districts, thread, use no randomGeneratorClass
int Stock(boost::filesystem::path, uint32_t); // stock, thread
int District(boost::filesystem::path, randomGeneratorClass *, uint32_t); // district
int Customer(uint32_t, uint32_t, csv_file&, csv_file&); // customer
int Orders(uint32_t, uint32_t, csv_file&, csv_file&, csv_file&); // orders, new_order, order_line

/* Global SQL Variables */
// EXEC SQL BEGIN DECLARE SECTION;
// char timestamp[20];
static TIMESTAMP timestamp;
// long count_ware;
// static uint32_t count_ware;
// EXEC SQL END DECLARE SECTION;
/* Global Variables */
// static int i;

void tpcc_gen_initialize(boost::filesystem::path db) {
    gettimestamp(static_cast<char *>(timestamp));

    boost::system::error_code error;
    if (const bool result = boost::filesystem::exists(db, error); result && !error) {
        if (!FLAGS_f) {
            throw std::runtime_error(db.string() + " already exists");
        }
        boost::filesystem::remove_all(db);
    }

    if (const bool result = boost::filesystem::create_directory(db, error); !result || error) {
        throw std::runtime_error(std::string("cannot create ") + db.string());
    }
    for ( auto& e : {"WAREHOUSE", "DISTRICT", "CUSTOMER", "NEW_ORDER", "ORDERS", "ORDER_LINE", "ITEM", "STOCK", "HISTORY"} ) {
        boost::filesystem::path table(e);
        if (const bool result = boost::filesystem::create_directory(db / table, error); !result || error) {
            throw std::runtime_error(std::string("cannot create ") + (db / table).string());
        }
    }
}

int tpcc_items_gen(boost::filesystem::path db)
{
    generate_Items(db);
    return 0;
}
    
int tpcc_warehouse_gen(boost::filesystem::path db, std::uint16_t w_id)
{
    auto threadWare = std::thread([&db, w_id](){generate_Ware(db, w_id);});
    auto threadCust = std::thread([&db, w_id](){generate_Cust(db, w_id);});
    auto threadOrd = std::thread([&db, w_id](){generate_Ord(db, w_id);});
    auto threadStock = std::thread([&db, w_id](){Stock(db, w_id);});
        
    threadWare.join();
    threadCust.join();
    threadOrd.join();
    threadStock.join();
    
    return 0;
}

/*==================================================================+
  | ROUTINE NAME
  | Items
  | DESCRIPTION
  | generate_s the Item table
  | ARGUMENTS
  | path to root directory
  +==================================================================*/
int generate_Items(boost::filesystem::path db)
{
    csv_file f(db / boost::filesystem::path("ITEM"), 1);
    std::unique_ptr<randomGeneratorClass> randomGenerator = std::make_unique<randomGeneratorClass>();
    //  EXEC SQL BEGIN DECLARE SECTION;
    uint32_t i_id;
    //  char i_name[24];
    VARCHAR24 i_name;
    //  float i_price;
    double i_price;
    //  char i_data[50];
    VARCHAR50 i_data;
    //  EXEC SQL END DECLARE SECTION;
    int idatasiz;
    //  EXEC SQL WHENEVER SQLERROR GOTO sqlerr;
    if (FLAGS_display_progress) printf("Loading Item \n");  // NOLINT

    std::array<uint32_t, scale::items +1> orig_for_items = {};
    uint32_t pos;
    
    for (i_id=1; i_id<= scale::items; i_id++) { orig_for_items.at(i_id) = 0; }
    for (uint32_t i=0; i< scale::items /10; i++) {
        do {
            pos = randomGenerator->RandomNumber(1L, scale::items);
        } while (orig_for_items.at(pos) == 1);
        orig_for_items.at(pos) = 1;
    }

    for (i_id=1; i_id<= scale::items; i_id++) {
        /* Generate Item Data */
        randomGenerator->MakeAlphaString(14, 24, i_name);
        i_price=(static_cast<double>(randomGenerator->RandomNumber(100L,10000L)))/100.0;
        idatasiz=randomGenerator->MakeAlphaString(26,50, static_cast<char *>(i_data));
        if (orig_for_items.at(i_id) == 1)
        {
            uint32_t pos = randomGenerator->RandomNumber(0L,idatasiz-8);
            i_data[pos]='o';
            i_data[pos+1]='r';
            i_data[pos+2]='i';
            i_data[pos+3]='g';
            i_data[pos+4]='i';
            i_data[pos+5]='n';
            i_data[pos+6]='a';
            i_data[pos+7]='l';
        }
        f.set_int8("i_id", static_cast<std::int64_t>(i_id));
        f.set_character("i_name", i_name);
        f.set_float8("i_price", i_price);
        f.set_character("i_data", i_data);
        f.end_of_row();
        if (FLAGS_display_progress) {
            if ((i_id % 100) == 0) {
                printf("i");  // NOLINT
                //      EXEC SQL COMMIT WORK;
                if ( !(i_id % 5000) ) printf(" %d\n",i_id);  // NOLINT
            }
        }
    }
    f.close();
    if (FLAGS_display_progress) printf("Item Done. \n");  // NOLINT
    return 0;
}

/*==================================================================+
  | ROUTINE NAME
  | generate_Ware
  | DESCRIPTION
  | generate_s the Warehouse table
  | generate_s District as Warehouses are created
  | ARGUMENTS
  | path to root directory, warehouse id
  +==================================================================*/
int generate_Ware(boost::filesystem::path db, uint32_t w_id)
{
    csv_file f(db / boost::filesystem::path("WAREHOUSE"), w_id);
    std::unique_ptr<randomGeneratorClass> randomGenerator = std::make_unique<randomGeneratorClass>();
    
    //  EXEC SQL BEGIN DECLARE SECTION;
    //  uint32_t w_id;
    //  char w_name[10];
    VARCHAR10 w_name;
    //  char w_street_1[20];
    VARCHAR20 w_street_1;
    //  char w_street_2[20];
    VARCHAR20 w_street_2;
    //  char w_city[20];
    VARCHAR20 w_city;
    //  char w_state[2];
    CHAR2 w_state;
    //  char w_zip[9];
    CHAR9 w_zip;
    //  float w_tax;
    double w_tax;
    //  float w_ytd;
    double w_ytd;
    //  EXEC SQL END DECLARE SECTION;
    //  EXEC SQL WHENEVER SQLERROR GOTO sqlerr;
    if (FLAGS_display_progress) printf("Loading Warehouse \n");  // NOLINT

    /* Generate Warehouse Data */
    randomGenerator->MakeAlphaString( 6, 10,  static_cast<char *>(w_name));
    randomGenerator->MakeAddress(static_cast<char *>(w_street_1), static_cast<char *>(w_street_2), static_cast<char *>(w_city), static_cast<char *>(w_state), static_cast<char *>(w_zip));
    w_tax=(static_cast<double>(randomGenerator->RandomNumber(10L,20L)))/100.0;
    w_ytd=3000000.00;
    //              if ( option_debug )
    //                printf( "WID = %d, Name= %16s, Tax = %5.2f\n",
    //                        w_id, w_name, w_tax );
    //    EXEC SQL INSERT INTO
    //      warehouse (w_id, w_name,
    //                 w_street_1, w_street_2, w_city, w_state, w_zip,
    //                 w_tax, w_ytd)
    //      values (:w_id, :w_name,
    //              :w_street_1, :w_street_2, :w_city, :w_state,
    //              :w_zip, :w_tax, :w_ytd);
    /** Make Rows associated with Warehouse **/
    f.set_int8("w_id", static_cast<std::int64_t>(w_id));
    f.set_character("w_name", w_name);
    f.set_character("w_street_1", w_street_1);
    f.set_character("w_street_2", w_street_2);
    f.set_character("w_city", w_city);
    f.set_character("w_state", w_state);
    f.set_character("w_zip", w_zip);
    f.set_float8("w_tax", w_tax);
    f.set_float8("w_ytd", w_ytd);
    f.end_of_row();
    f.close();

    District(db, randomGenerator.get(), w_id);
    
    return 0;
}

/*==================================================================+
  | ROUTINE NAME
  | generate_Cust
  | DESCRIPTION
  | generate_s the Customer Table
  | ARGUMENTS
  | path to root directory, warehouse id
  +==================================================================*/
int generate_Cust(boost::filesystem::path db, uint32_t w_id)
{
    csv_file fc(db / boost::filesystem::path("CUSTOMER"), w_id);
    csv_file fh(db / boost::filesystem::path("HISTORY"), w_id);

    //  EXEC SQL BEGIN DECLARE SECTION;
    //  EXEC SQL END DECLARE SECTION;
    //  uint32_t w_id;
    uint32_t d_id;

    //  EXEC SQL WHENEVER SQLERROR GOTO sqlerr;
    for (d_id=1L; d_id<= scale::districts; d_id++) {
        Customer(d_id,w_id, fc, fh);
    }
    fc.close();
    fh.close();

    /* Just in case */
    return 0;
}

/*==================================================================+
  | ROUTINE NAME
  | generate_Ord
  | DESCRIPTION
  | generate_s the Orders and Order_Line Tables
  | ARGUMENTS
  | path to root directory, warehouse id
  +==================================================================*/
int generate_Ord(boost::filesystem::path db, uint32_t w_id)
{
    csv_file fno(db / boost::filesystem::path("NEW_ORDER"), w_id);
    csv_file fo(db / boost::filesystem::path("ORDERS"), w_id);
    csv_file fol(db / boost::filesystem::path("ORDER_LINE"), w_id);
    //  EXEC SQL BEGIN DECLARE SECTION;
    //  uint32_t w_id;
    //  float w_tax;
    //  float d_tax;
    uint32_t d_id;
    //  EXEC SQL END DECLARE SECTION;
    //  EXEC SQL WHENEVER SQLERROR GOTO sqlerr;
    for (d_id=1L; d_id <= scale::districts; d_id++) {
        Orders(d_id, w_id, fno, fo, fol);
        //  EXEC SQL COMMIT WORK;
        /* Just in case */
    }
    fno.close();
    fo.close();
    fol.close();
    return 0;
}

/*==================================================================+
  | ROUTINE NAME
  | Stock
  | DESCRIPTION
  | generate_s the Stock table
  | ARGUMENTS
  | path to root directory, warehouse id
  +==================================================================*/
int Stock(boost::filesystem::path db, uint32_t w_id)
{
    csv_file f(db / boost::filesystem::path("STOCK"), w_id);
    std::unique_ptr<randomGeneratorClass> randomGenerator = std::make_unique<randomGeneratorClass>();
    
    std::array<uint32_t, scale::items +1> orig_for_stock = {};
    uint32_t pos;
    unsigned int i;
        
    for (i=0; i<= scale::items; i++) { orig_for_stock.at(i)=0; }
    for (i=0; i< scale::items /10; i++) {
        do {
            pos=randomGenerator->RandomNumber(1L, scale::items);
        } while (orig_for_stock.at(pos) == 1);
        orig_for_stock.at(pos) = 1;
    }

    //  EXEC SQL BEGIN DECLARE SECTION;
    uint32_t s_i_id = 1;
    uint32_t s_w_id;
    uint32_t s_quantity;
    //  char s_dist_01[24];
    VARCHAR50 s_dist_01;
    //  char s_dist_02[24];
    VARCHAR50 s_dist_02;
    //  char s_dist_03[24];
    VARCHAR50 s_dist_03;
    //  char s_dist_04[24];
    VARCHAR50 s_dist_04;
    //  char s_dist_05[24];
    VARCHAR50 s_dist_05;
    //  char s_dist_06[24];
    VARCHAR50 s_dist_06;
    //  char s_dist_07[24];
    VARCHAR50 s_dist_07;
    //  char s_dist_08[24];
    VARCHAR50 s_dist_08;
    //  char s_dist_09[24];
    VARCHAR50 s_dist_09;
    //  char s_dist_10[24];
    VARCHAR50 s_dist_10;
    //  char s_data[50];
    VARCHAR50 s_data;
    //  EXEC SQL END DECLARE SECTION;
    int sdatasiz;
    //  EXEC SQL WHENEVER SQLERROR GOTO sqlerr;
    if (FLAGS_display_progress) printf("generating Stock for i_id=%d, w_id=%d\n", s_i_id, w_id);  // NOLINT
    
    for (s_i_id = 1; s_i_id <= scale::items; s_i_id++) {
        s_w_id = w_id;
        /* Generate Stock Data */
        s_quantity=randomGenerator->RandomNumber(10L,100L);
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_01));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_02));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_03));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_04));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_05));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_06));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_07));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_08));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_09));
        randomGenerator->MakeAlphaString(24,24,static_cast<char *>(s_dist_10));
        sdatasiz=randomGenerator->MakeAlphaString(26,50,static_cast<char *>(s_data));
        if (orig_for_stock.at(s_i_id) == 1)
        {
            uint32_t pos=randomGenerator->RandomNumber(0L,sdatasiz-8);
            s_data[pos]='o';
            s_data[pos+1]='r';
            s_data[pos+2]='i';
            s_data[pos+3]='g';
            s_data[pos+4]='i';
            s_data[pos+5]='n';
            s_data[pos+6]='a';
            s_data[pos+7]='l';
        }
        //    EXEC SQL INSERT INTO
        //      stock (s_i_id, s_w_id, s_quantity,
        //             s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
        //             s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10,
        //             s_data, s_ytd, s_cnt_order, s_cnt_remote)
        //      values (:s_i_id, :s_w_id, :s_quantity,
        //              :s_dist_01, :s_dist_02, :s_dist_03, :s_dist_04, :s_dist_05,
        //              :s_dist_06, :s_dist_07, :s_dist_08, :s_dist_09, :s_dist_10,
        //              :s_data, 0, 0, 0);
        f.set_int8("s_i_id", static_cast<std::int64_t>(s_i_id));
        f.set_int8("s_w_id", static_cast<std::int64_t>(s_w_id));
        f.set_int8("s_quantity", static_cast<std::int64_t>(s_quantity));
        f.set_character("s_dist_01", s_dist_01);
        f.set_character("s_dist_02", s_dist_02);
        f.set_character("s_dist_03", s_dist_03);
        f.set_character("s_dist_04", s_dist_04);
        f.set_character("s_dist_05", s_dist_05);
        f.set_character("s_dist_06", s_dist_06);
        f.set_character("s_dist_07", s_dist_07);
        f.set_character("s_dist_08", s_dist_08);
        f.set_character("s_dist_09", s_dist_09);
        f.set_character("s_dist_10", s_dist_10);
        f.set_character("s_data", s_data);
        f.end_of_row();
    }

    if (FLAGS_display_progress) {
        if ((s_i_id % 100) == 0) {
            //      EXEC SQL COMMIT WORK;
            printf("s");  // NOLINT
            if ( !(s_i_id % 5000) ) printf(" %d\n",s_i_id);  // NOLINT
        }
    }

    if (FLAGS_display_progress) printf("Stock Done.\n");  // NOLINT

    return 0;
}

/*==================================================================+
  | ROUTINE NAME
  | District
  | DESCRIPTION
  | generate_s the District table
  | ARGUMENTS
  | path to root directory, random generator, warehouse id
  +==================================================================*/
int District(boost::filesystem::path db, randomGeneratorClass *randomGenerator, uint32_t w_id)
{
    csv_file f(db / boost::filesystem::path("DISTRICT"), w_id);

    //  EXEC SQL BEGIN DECLARE SECTION;
    uint32_t d_id;
    uint32_t d_w_id;
    //  char d_name[10];
    VARCHAR10 d_name;
    //  char d_street_1[20];
    VARCHAR20 d_street_1;
    //  char d_street_2[20];
    VARCHAR20 d_street_2;
    //  char d_city[20];
    VARCHAR20 d_city;
    //  char d_state[2];
    CHAR2 d_state;
    //  char d_zip[9];
    CHAR9 d_zip;
    //  float d_tax;
    double d_tax;
    //  float d_ytd;
    double d_ytd;
    uint32_t d_next_o_id;
    //  EXEC SQL END DECLARE SECTION;
    //  EXEC SQL WHENEVER SQLERROR GOTO sqlerr;

    if (FLAGS_display_progress) printf("Loading District\n");  // NOLINT
    d_w_id=w_id;
    d_ytd=30000.0;
    d_next_o_id= scale::orders +1;

    for (d_id=1; d_id <= scale::districts; d_id++) {
        /* Generate District Data */
        randomGenerator->MakeAlphaString(6L,10L,static_cast<char *>(d_name));
        randomGenerator->MakeAddress(static_cast<char *>(d_street_1), static_cast<char *>(d_street_2), static_cast<char *>(d_city), static_cast<char *>(d_state), static_cast<char *>(d_zip));
        d_tax=(static_cast<double>(randomGenerator->RandomNumber(10L,20L)))/100.0;
        //    EXEC SQL INSERT INTO
        //      district (d_id, d_w_id, d_name,
        //                d_street_1, d_street_2, d_city, d_state, d_zip,
        //                d_tax, d_ytd, d_next_o_id)
        //      values (:d_id, :d_w_id, :d_name,
        //              :d_street_1, :d_street_2, :d_city, :d_state, :d_zip,
        //              :d_tax, :d_ytd, :d_next_o_id);
        f.set_int8("d_id", static_cast<std::int64_t>(d_id));
        f.set_int8("d_w_id", static_cast<std::int64_t>(d_w_id));
        f.set_character("d_name", d_name);
        f.set_character("d_street_1", d_street_1);
        f.set_character("d_street_2", d_street_2);
        f.set_character("d_city", d_city);
        f.set_character("d_state", d_state);
        f.set_character("d_zip", d_zip);
        f.set_float8("d_tax", d_tax);
        f.set_float8("d_ytd", d_ytd);
        f.set_int8("d_next_o_id", static_cast<std::int64_t>(d_next_o_id));
        f.end_of_row();
    }
    f.close();
    return 0;
}

/*==================================================================+
  | ROUTINE NAME
  | Customer
  | DESCRIPTION
  | generate_s Customer Table
  | Also inserts corresponding history record
  | ARGUMENTS
  | path to root directory, district id, warehouse id
  +==================================================================*/
int Customer(uint32_t d_id, uint32_t w_id, csv_file& fc, csv_file& fh)
{
    std::unique_ptr<randomGeneratorClass> randomGenerator = std::make_unique<randomGeneratorClass>();
    
    //  EXEC SQL BEGIN DECLARE SECTION;
    uint32_t c_id;
    uint32_t c_d_id;
    uint32_t c_w_id;
    //  char c_first[16];
    VARCHAR16 c_first;
    //  char c_middle[2];
    CHAR2 c_middle;
    //  char c_last[16];
    VARCHAR16 c_last;
    //  char c_street_1[20];
    VARCHAR20 c_street_1;
    //  char c_street_2[20];
    VARCHAR20 c_street_2;
    //  char c_city[20];
    VARCHAR20 c_city;
    //  char c_state[2];
    CHAR2 c_state;
    //  char c_zip[9];
    CHAR9 c_zip;
    //  char c_phone[16];
    CHAR16 c_phone;
    //  char c_since[11];
    VARCHAR10 c_since;
    //  char c_credit[2];
    CHAR2 c_credit;
    uint32_t c_credit_lim;
    //  float c_discount;
    double c_discount;
    //  float c_balance;
    double c_balance;
    //  char c_data[500];
    VARCHAR500 c_data;
    //  float h_amount;
    double h_amount;
    //  char h_data[24];
    VARCHAR24 h_data;
    //  EXEC SQL END DECLARE SECTION;
    //  EXEC SQL WHENEVER SQLERROR GOTO sqlerr;
    if (FLAGS_display_progress) printf("generate_ing Customer for d_id=%d, w_id=%d\n", d_id, w_id);  // NOLINT
    
    for (c_id = 1; c_id <= scale::customers; c_id++) {
        /* Generate Customer Data */
        c_d_id=d_id;
        c_w_id=w_id;
        randomGenerator->MakeAlphaString( 8, 16, static_cast<char *>(c_first) );
        c_middle[0]='O'; c_middle[1]='E'; c_middle[2]='\0';
        if (c_id <= 1000) {
            Lastname(c_id-1,static_cast<char *>(c_last));
        } else {
            Lastname(randomGenerator->nonUniformWithin(255,0,999),static_cast<char *>(c_last));
        }
        randomGenerator->MakeAddress( static_cast<char *>(c_street_1), static_cast<char *>(c_street_2), static_cast<char *>(c_city), static_cast<char *>(c_state), static_cast<char *>(c_zip) );
        randomGenerator->MakeNumberString( 16, 16, static_cast<char *>(c_phone) );
        if (randomGenerator->RandomNumber(0L,1L) > 0) {
            c_credit[0]='G';
        } else {
            c_credit[0]='B';
        }
        c_credit[1]='C'; c_credit[2]='\0';
        c_credit_lim=50000;
        c_discount=(static_cast<double>(randomGenerator->RandomNumber(0L,50L)))/100.0;
        c_balance= -10.0;
        getdatestamp(static_cast<char *>(c_since), randomGenerator->RandomNumber(1L,365L*50L));
        randomGenerator->MakeAlphaString(300,500,static_cast<char *>(c_data));
        
        //    EXEC SQL INSERT INTO
        //      customer (c_id, c_d_id, c_w_id,
        //              c_first, c_middle, c_last,
        //              c_street_1, c_street_2, c_city, c_state, c_zip,
        //              c_phone, c_since, c_credit,
        //              c_credit_lim, c_discount, c_balance, c_data,
        //              c_ytd_payment, c_cnt_payment, c_cnt_delivery)
        //      values (:c_id, :c_d_id, :c_w_id,
        //            :c_first, :c_middle, :c_last,
        //            :c_street_1, :c_street_2, :c_city, :c_state, :c_zip,
        //            :c_phone, :timestamp, :c_credit,
        //            :c_credit_lim, :c_discount, :c_balance, :c_data,
        //            10.0, 1, 0) ;
        fc.set_int8("c_id", static_cast<std::int64_t>(c_id));
        fc.set_int8("c_d_id", static_cast<std::int64_t>(c_d_id));
        fc.set_int8("c_w_id", static_cast<std::int64_t>(c_w_id));
        fc.set_character("c_first", c_first);
        fc.set_character("c_middle", c_middle);
        fc.set_character("c_last", c_last);
        fc.set_character("c_street_1", c_street_1);
        fc.set_character("c_street_2", c_street_2);
        fc.set_character("c_city", c_city);
        fc.set_character("c_state", c_state);
        fc.set_character("c_zip", c_zip);
        fc.set_character("c_phone", c_phone);
        fc.set_character("c_since", c_since);
        fc.set_character("c_credit", c_credit);
        fc.set_float8("c_credit_lim", static_cast<double>(c_credit_lim));
        fc.set_float8("c_discount", c_discount);
        fc.set_float8("c_balance", c_balance);
        fc.set_character("c_data", c_data);
        fc.end_of_row();
        
        h_amount=10.0;
        randomGenerator->MakeAlphaString(12,24,static_cast<char *>(h_data));
        //    EXEC SQL INSERT INTO
        //      history (h_c_id, h_c_d_id, h_c_w_id,
        //             h_w_id, h_d_id, h_date, h_amount, h_data)
        //      values (:c_id, :c_d_id, :c_w_id,
        //            :c_w_id, :c_d_id, :timestamp, :h_amount, :h_data);
        //      if ( option_debug )
        //        printf( "CID = %d, LST = %s, P# = %s\n",
        //                c_id, c_last, c_phone );
        fh.set_int8("h_c_id", c_id);
        fh.set_int8("h_c_d_id", c_d_id);
        fh.set_int8("h_c_w_id", c_w_id);
        fh.set_int8("h_w_id", c_w_id);
        fh.set_int8("h_d_id", c_d_id);
        fh.set_character("h_date", timestamp);
        fh.set_float8("h_amount", h_amount);
        fh.set_character("h_data", h_data);
        fh.end_of_row();
        
        if (FLAGS_display_progress) {
            if ((c_id % 100) == 0) {
                //      EXEC SQL COMMIT WORK;
                printf("c");  // NOLINT
                if ( !(c_id % 1000) ) printf(" %d\n",c_id);  // NOLINT
            }
        }
    }

    if (FLAGS_display_progress) printf("Customer Done.\n");  // NOLINT
    return 0;
}

/*==================================================================+
  | ROUTINE NAME
  | Orders
  | DESCRIPTION
  | generate_s the Orders table
  | Also loads the Order_Line table on the fly
  | ARGUMENTS
  | path to root directory, district id, warehouse id
  +==================================================================*/
int Orders(uint32_t d_id, uint32_t w_id, csv_file& fno, csv_file& fo, csv_file& fol)
{
    std::unique_ptr<randomGeneratorClass> randomGenerator = std::make_unique<randomGeneratorClass>();
    
    //  EXEC SQL BEGIN DECLARE SECTION;
    uint32_t o_id = 1;
    uint32_t o_c_id;
    uint32_t o_d_id;
    uint32_t o_w_id;
    uint32_t o_carrier_id;
    uint32_t o_ol_cnt;
    uint32_t ol;
    uint32_t ol_i_id;
    uint32_t ol_supply_w_id;
    uint32_t ol_quantity;
    //  long ol_amount;
    //  float ol_amount;
    double ol_amount;
    //  char ol_dist_info[24];
    VARCHAR24 ol_dist_info;
    //  float i_price;
    //  float c_discount;
    //  EXEC SQL END DECLARE SECTION;
    //  EXEC SQL WHENEVER SQLERROR GOTO sqlerr;
    if (FLAGS_display_progress) printf("generateing Orders for d_id=%d, w_id=%d\n", d_id, w_id);  // NOLINT
    o_d_id=d_id;
    o_w_id=w_id;
    Permutation<scale::customers> permutation;
    
    for (o_id = 1; o_id <= scale::orders; o_id++) {
        /* Generate Order Data */
        o_c_id=permutation.get_permutation(randomGenerator.get());
        o_carrier_id=randomGenerator->RandomNumber(1L,10L);
        o_ol_cnt=randomGenerator->RandomNumber(5L,15L);
        if (o_id > ((scale::orders * 7) / 10)) /* the last 900 orders have not been delivered) */
        {
            //    EXEC SQL INSERT INTO
            //      orders (o_id, o_c_id, o_d_id, o_w_id,
            //              o_entry_d, o_carrier_id, o_ol_cnt, o_all_local)
            //      values (:o_id, :o_c_id, :o_d_id, :o_w_id,
            //              :timestamp, NULL, :o_ol_cnt, 1);
            // to set o_carrir_id NULL, we does not include this column in the SQL
            fo.set_int8("o_id", static_cast<std::int64_t>(o_id));
            fo.set_int8("o_c_id", static_cast<std::int64_t>(o_c_id));
            fo.set_int8("o_d_id", static_cast<std::int64_t>(o_d_id));
            fo.set_int8("o_w_id", static_cast<std::int64_t>(o_w_id));
            fo.set_character("o_entry_d", timestamp);
            fo.set_null("o_carrier_id");
            fo.set_int8("o_ol_cnt", static_cast<std::int64_t>(o_ol_cnt));
            fo.set_int8("o_all_local", static_cast<std::int64_t>(1));
            fo.end_of_row();

            //    EXEC SQL INSERT INTO
            //      new_order (no_o_id, no_d_id, no_w_id)
            //      values (:o_id, :o_d_id, :o_w_id);
            fno.set_int8("no_o_id", static_cast<std::int64_t>(o_id));
            fno.set_int8("no_d_id", static_cast<std::int64_t>(o_d_id));
            fno.set_int8("no_w_id", static_cast<std::int64_t>(o_w_id));
            fno.end_of_row();
        }
        else
            //      EXEC SQL INSERT INTO
            //      orders (o_id, o_c_id, o_d_id, o_w_id,
            //              o_entry_d, o_carrier_id, o_ol_cnt, o_all_local)
            //      values (:o_id, :o_c_id, :o_d_id, :o_w_id,
            //              :timestamp, :o_carrier_id, :o_ol_cnt, 1);
        {
            fo.set_int8("o_id", static_cast<std::int64_t>(o_id));
            fo.set_int8("o_c_id", static_cast<std::int64_t>(o_c_id));
            fo.set_int8("o_d_id", static_cast<std::int64_t>(o_d_id));
            fo.set_int8("o_w_id", static_cast<std::int64_t>(o_w_id));
            fo.set_character("o_entry_d", timestamp);
            fo.set_int8("o_carrier_id", static_cast<std::int64_t>(o_carrier_id));
            fo.set_int8("o_ol_cnt", static_cast<std::int64_t>(o_ol_cnt));
            fo.set_int8("o_all_local", static_cast<std::int64_t>(1));
            fo.end_of_row();
        }
        
        //              if ( option_debug )
        //                printf( "OID = %d, CID = %d, DID = %d, WID = %d\n",
        //                        o_id, o_c_id, o_d_id, o_w_id);
        TIMESTAMP datetime; gettimestamp(static_cast<char *>(datetime), randomGenerator->RandomNumber(1L,90L*24L*60L));
        for (ol=1; ol<=o_ol_cnt; ol++) {
            /* Generate Order Line Data */
            ol_i_id=randomGenerator->RandomNumber(1L, scale::items);
            ol_supply_w_id=o_w_id;
            ol_quantity=5;
            ol_amount=0.0;
                
            randomGenerator->MakeAlphaString(24,24,static_cast<char *>(ol_dist_info));
                
            if (o_id > ((scale::orders * 7) / 10))
            {
                //  EXEC SQL INSERT INTO
                //    order_line (ol_o_id, ol_d_id, ol_w_id, ol_number,
                //                ol_i_id, ol_supply_w_id, ol_quantity, ol_amount,
                //                ol_dist_info, ol_delivery_d)
                //    values (:o_id, :o_d_id, :o_w_id, :ol,
                //            :ol_i_id, :ol_supply_w_id, :ol_quantity, :ol_amount,
                //            :ol_dist_info, NULL);
                // to set ol_delivery_d NULL, we does not include this column in the SQL
                fol.set_int8("ol_o_id", static_cast<std::int64_t>(o_id));
                fol.set_int8("ol_d_id", static_cast<std::int64_t>(o_d_id));
                fol.set_int8("ol_w_id", static_cast<std::int64_t>(o_w_id));
                fol.set_int8("ol_number", static_cast<std::int64_t>(ol));
                fol.set_int8("ol_i_id", static_cast<std::int64_t>(ol_i_id));
                fol.set_int8("ol_supply_w_id", static_cast<std::int64_t>(ol_supply_w_id));
                fol.set_int8("ol_quantity", static_cast<std::int64_t>(ol_quantity));
                fol.set_float8("ol_amount", ol_amount);
                fol.set_character("ol_dist_info", ol_dist_info);
                fol.set_character("ol_delivery_d", {});
                fol.end_of_row();
            }
            else
                //    EXEC SQL INSERT INTO
                //      order_line (ol_o_id, ol_d_id, ol_w_id, ol_number,
                //                  ol_i_id, ol_supply_w_id, ol_quantity,
                //                  (float)(RandomNumber(10L, 10000L))/100.0,
                //                  ol_dist_info, ol_delivery_d)
                //      values (:o_id, :o_d_id, :o_w_id, :ol,
                //              :ol_i_id, :ol_supply_w_id, :ol_quantity,
                //              :ol_amount,
                //              :ol_dist_info, datetime);
            {
                    ol_amount = (static_cast<double>(randomGenerator->RandomNumber(10L, 10000L)))/100.0;
                    fol.set_int8("ol_o_id", static_cast<std::int64_t>(o_id));
                    fol.set_int8("ol_d_id", static_cast<std::int64_t>(o_d_id));
                    fol.set_int8("ol_w_id", static_cast<std::int64_t>(o_w_id));
                    fol.set_int8("ol_number", static_cast<std::int64_t>(ol));
                    fol.set_int8("ol_i_id", static_cast<std::int64_t>(ol_i_id));
                    fol.set_int8("ol_supply_w_id", static_cast<std::int64_t>(ol_supply_w_id));
                    fol.set_int8("ol_quantity", static_cast<std::int64_t>(ol_quantity));
                    fol.set_float8("ol_amount", ol_amount);
                    fol.set_character("ol_dist_info", ol_dist_info);
                    fol.set_character("ol_delivery_d", datetime);
                    fol.end_of_row();
            }
            //              if ( option_debug )
            //                printf( "OL = %d, IID = %d, QUAN = %d, AMT = %8.2f\n",
            //                        ol, ol_i_id, ol_quantity, ol_amount);
        }
            
        if (FLAGS_display_progress) {
            if ((o_id % 100) == 0) {
                printf("o");  // NOLINT
                //      EXEC SQL COMMIT WORK;
                if ( !(o_id % 1000) ) printf(" %d\n",o_id);  // NOLINT
            }
        }
        //  EXEC SQL COMMIT WORK;

    }
    return 0;
}

}  // namespace tpcc
