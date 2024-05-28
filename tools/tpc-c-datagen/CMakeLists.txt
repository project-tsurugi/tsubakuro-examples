# Copyright 2020-2021 tsurugi project.
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

cmake_minimum_required(VERSION 3.10)

project(tpcc-datagen
    VERSION 0.0.1
    DESCRIPTION "TPC-C benchmark program running on Jogasaki SQL Execution Engine"
    LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

list(APPEND CMAKE_MODULE_PATH "${PROJECT_SOURCE_DIR}/../cmake")

option(ENABLE_SANITIZER "enable sanitizer on debug build" ON)
option(ENABLE_UB_SANITIZER "enable undefined behavior sanitizer on debug build" OFF)
option(ENABLE_COVERAGE "enable coverage on debug build" OFF)
option(BUILD_DOCUMENTS "build documents" ON)
option(ENABLE_GOOGLE_PERFTOOLS "Enable Google Perftools" OFF)
option(TRACY_ENABLE "enable tracy profiler" OFF)

if (ENABLE_GOOGLE_PERFTOOLS)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DENABLE_GOOGLE_PERFTOOLS")
endif()

if(NOT DEFINED SHARKSFIN_IMPLEMENTATION)
    set(
            SHARKSFIN_IMPLEMENTATION "memory"
            CACHE STRING
            "sharksfin target name to link"
            FORCE
    )
endif()

find_package(Doxygen)
find_package(glog REQUIRED)
find_package(gflags REQUIRED)
find_package(Threads REQUIRED)
find_package(Boost
        COMPONENTS filesystem thread system container stacktrace_backtrace
        REQUIRED
        )
find_package(mpdecpp REQUIRED)
include(GNUInstallDirs)
include(CMakePackageConfigHelpers)
include(CompileOptions)
include(InstallOptions)

# add_subdirectory(third_party) # should be before enable_testing()

set(export_name "tpcc-datagen")
set(package_name "tpcc-datagen")

configure_file(
    ${CMAKE_CURRENT_SOURCE_DIR}/../cmake/Config.cmake.in
    ${CMAKE_CURRENT_BINARY_DIR}/../${package_name}-config.cmake
    @ONLY
)

# install(
#     EXPORT ${package_name}
#     NAMESPACE ${package_name}-
#     FILE ${package_name}-targets.cmake
#     DESTINATION ${CMAKE_INSTALL_LIBDIR}/cmake/${package_name}
#     EXPORT_LINK_INTERFACE_LIBRARIES
# )

add_subdirectory(src)
