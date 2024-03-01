# tsubakuro-examples - example client programs using tsubakuro for tsurugidb

## Requirements

* Java `>= 11`

* dependent modules:
  * [Tsubakuro](https://github.com/project-tsurugi/tsubakuro)


## tpc-c
### setup
```
git clone git@github.com:project-tsurugi/tsubakuro-examples.git
pushd tsubakuro-examples/modules/tpccLoader
../../gradlew build
popd

pushd
cd tsubakuro-examples/modules/tpcc
../../gradlew build
popd
```

### table初期データ作成
https://github.com/project-tsurugi/jogasaki-benchmarks/tree/master/tpc-c-datagen/README.md 参照

### ベンチマーク実行
ベンチマーク実行は、tsurugidbを起動し、table初期データをloadした後にクライアントプログラムを実行する。

#### tsurugidb起動
```
tgctl start
```

#### table初期データload
```
cd tsubakuro-examples
./gradlew runTpccLoader --args="-w ${warehouse} -d ${directory}"
```

\${warehouse}はwarehouse数、\${directory}はtable初期データを格納したディレクトリ。なお、\${warehouse}はtable初期データ作成時に指定したwarehouse数と一致させる必要はない。大きなwarehouse数を指定して初期データを作成していた場合、\${warehouse}で指定したwarehouse数分のデータだけを読み込む。なお、小さなwarehouse数を指定して初期データを作成していた場合は、そのwarehouse数がベンチマーク実行のwarehouse数となる。


#### クライアントプログラム実行
```
cd tsubakuro-examples
./gradlew runTpcc --args="${threads} ${duration}"
```

\${threads}はクライアントスレッド数、\${duration}はベンチマーク実行時間（単位は秒）。各々のデフォルト値は、\${threads}が8、\${duration}が30。なお、warehouse数を指定する必要はない（ベンチマーク実行に先立ってtable初期データloadで準備したWAREHOUSE表をSELECT COUNTして得ている）。
