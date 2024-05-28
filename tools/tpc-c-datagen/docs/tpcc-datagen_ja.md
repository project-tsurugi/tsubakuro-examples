# TPC-C ベンチマーク初期データ作成プログラム　コマンドライン仕様
2023.05.18
horikawa

## NAME
tpcc-datagen

## SYNOPSIS
tpcc-datagen [-w NUM] [-o DIR] [-f]

## DESCRIPTION
* TPC-Cベンチマークの初期データをCSV形式で作成する。
* 初期データファイルは以下の通りに作成する。
  * -oで指定したディレクトリを作成する。
  * その下に各表名のディレクトリを作成する。
  * その下にwarehouse別のCSVファイルを作成する。
  * 以下にw=2として作成したファイルを示す
```
db + WAREHOUSE + 1.csv
               + 2.csv
   + DISTRICT + 1.csv
              + 2.csv
   + CUSTOMER + 1.csv
              + 2.csv
   + NEW_ORDER + 1.csv
               + 2.csv
   + ORDERS + 1.csv
            + 2.csv
   + ORDER_LINE + 1.csv
                + 2.csv
   + STOCK + 1.csv
           + 2.csv
   + HISTORY + 1.csv
               2.csv
   + ITEM + 1.csv  （ITEM表はwarehouse数とは関係なく、全warehouseに共通しているので、1.csvのみが作成される）
```

## OPTIONS
* -w NUM 作成するTPC-Cベンチマーク初期データのwarehouse数を指定する。default値は1。
* -o DIR 作成するTPC-Cベンチマーク初期データを書き込むディレクトリ名を指定する。defaultは'db'。
* -f `-o DIR`で指定したディレクトリが存在する場合は、ディレクトリに存在するファイルを消去してからTPC-Cベンチマーク初期データを作成する。`-f`を指定せず、かつ、`-o DIR`で指定したディレクトリが存在する場合は、初期データを作成せず、ディレクトリが既に存在する旨のエラーとして終了する。