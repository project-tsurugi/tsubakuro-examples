# Copyright 2018-2021 tsurugi project.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

file(GLOB SOURCES
    "*.cpp"
)

# remove this because tateyama includes TracyClient.cpp
#
#if(TRACY_ENABLE)
#    file(GLOB TRACY_CLIENT
#            "../../third_party/tracy/TracyClient.cpp"
#            )
#    list(APPEND SOURCES ${TRACY_CLIENT})
#endif()

add_executable(tpcc
    ${SOURCES}
)

set_target_properties(tpcc
    PROPERTIES
        RUNTIME_OUTPUT_NAME "tpcc-datagen"
)

target_include_directories(tpcc
    PRIVATE .
)

target_link_libraries(tpcc
    PRIVATE Threads::Threads
    PRIVATE gflags::gflags
    PRIVATE Boost::filesystem
    PRIVATE glog::glog
)

set_compile_options(tpcc)

install_custom(tpcc ${export_name})
