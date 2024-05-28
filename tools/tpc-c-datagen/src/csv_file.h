/*
 * Copyright 2023-2023 tsurugi project.
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
#pragma once

#include <iostream>

#include <boost/filesystem.hpp>
#include <boost/filesystem/fstream.hpp>

namespace tpcc {

    class csv_file {
      public:
        csv_file(boost::filesystem::path path, std::size_t n) {
            do {
                ofs_.open(path / boost::filesystem::path((std::to_string(n) + ".csv").c_str()));
                if (ofs_.is_open()) {
                    return;
                }
                if (!already_notified_) {
                    std::cerr << "reached open file limit, consider increasing open file limit" << std::endl;
                    already_notified_ = true;
                }
                ofs_.close();
                struct timespec delay = {1, 0};
                nanosleep(&delay, NULL);
            } while (true);
        }

        void set_int8(std::string_view, std::int64_t v) {
            separator();
            ofs_ << std::dec << v;
        }
        void set_float8(std::string_view, double v) {
            separator();
            ofs_.precision(2);
            ofs_ << std::fixed << std::showpoint << v;
        }
        void set_character(std::string_view, std::string_view v) {
            separator();
            ofs_ << v;
        }
        void set_null(std::string_view) {
            separator();
        }
        void end_of_row() {
            state_ = new_line;
        }
        void close() {
            separator();
            ofs_.close();
        }

      private:
        boost::filesystem::ofstream ofs_;
        enum {brand_new, cont, new_line} state_{brand_new};
        bool already_notified_{};

        void separator() {
            switch (state_) {
            case brand_new: break;
            case cont: ofs_ << ",";  break;
            case new_line: ofs_ << std::endl; break;
            }
            state_ = cont;
        }
    };

} // namespace tpcc
