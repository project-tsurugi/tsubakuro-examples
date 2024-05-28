/*
 * Copyright 2018-2021 tsurugi project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <iostream>
#include <thread>
#include <vector>
#include <exception>

#include <gflags/gflags.h>
#include <boost/filesystem.hpp>

#include "tpcc_common.h"

DEFINE_uint32(w, 1, "Database size (TPC-C scale factor).");  //NOLINT
DEFINE_bool(f, false, "Overwrite location.");  //NOLINT
DEFINE_string(o, "db", "database location on file system");  // NOLINT
DEFINE_bool(display_progress, false, "Display progress of data generation");  //NOLINT

namespace tpcc {

std::uint16_t scale::warehouses = 1U;

int driver_main(int argc, char **argv)
{
    gflags::SetUsageMessage("TPC-C data generator");
    gflags::ParseCommandLineFlags(&argc, &argv, true);
    scale::warehouses = FLAGS_w;

    boost::filesystem::path top(FLAGS_o);
    try {
        tpcc_gen_initialize(top);
    } catch (std::exception& e) {
        std::cerr << e.what() << std::endl;
        return 1;
    }

    std::vector<std::thread> threads;
    threads.emplace_back(std::thread([&top](){ tpcc_items_gen(top); }));
    for (std::uint16_t wid = 1; wid <= scale::warehouses; wid++) {
        threads.emplace_back(std::thread([&top, wid](){ tpcc_warehouse_gen(boost::filesystem::path(FLAGS_o), wid); }));
    }

    for (auto &t : threads) {
        t.join();
    }
    
    return 0;
}

}  // namespace tpcc

int main(int argc, char **argv) {
    return tpcc::driver_main(argc, argv);
}
