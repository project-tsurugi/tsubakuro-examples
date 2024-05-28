# tpc-c-datagen - tpc-c table initial data generator program for tsurugidb

## Requirements

* CMake `>= 3.16`
* C++ Compiler `>= C++17`

## build and install
### build

```sh
mkdir -p build
cd build
cmake -G Ninja -DCMAKE_BUILD_TYPE=Release ..
cmake --build .
```

### install

```sh
cmake --build . --target install
```

## run
```sh
tpcc-datagen -w ${warehouse} -o ${directory}
```

where \${warehouse} is the number of warehouses, \${directory} is the directory name where the created data will be stored.
