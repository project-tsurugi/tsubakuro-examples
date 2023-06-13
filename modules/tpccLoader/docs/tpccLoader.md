# TPC-C ベンチマーク初期データ作成をtsurugiに設定するプログラム　オプション仕様
2023.06.13
horikawa

## NAME
com.tsurugidb.tsubakuro.examples.tpccLoader.Main

## DESCRIPTION
* tpcc-genで作成したTPC-Cベンチマークの初期データ（CSV形式）をtsurugiに設定する。
* tsurugiへの接続方法は、システムプロパティ（名称はtsurugi.dbname）で指定する。

## OPTIONS
* -w NUM tsurugiに設定するTPC-Cベンチマーク初期データのwarehouse数を指定する。defaultはtpcc-genで作成したTPC-Cベンチマークのwarehouse数（「用意したデータのwarehouse数」と呼ぶ）。用意したデータのwarehouse数を超える値を設定した場合は、warehouse数が大きすぎる旨のメッセージを出力し、-wに用意したデータのwarehouse数が設定されたものとして動作する。
* -d tsurugiに設定するTPC-Cベンチマーク初期データが配置されているディレクトリ名を指定する。defaultは'db'。
* -v TPC-Cベンチマーク初期データをtsurugiに設定する操作の途中経過を表示する。隠しオプションとして公開しない可能性あり。