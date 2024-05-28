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
#pragma once

#include <cstring>
#include <random>
#include <ctime>
#include <array>
#include <chrono>
#include <iomanip>
#include <functional>

#include "tpcc_schema.h"

namespace tpcc {

namespace scale {
// cf. from tpc-c_V5.11.0.pdf Page 117
// #define MAXITEMS 100000
// #define CUST_PER_DIST 3000
// #define DIST_PER_WARE 10
// #define ORD_PER_DIST 3000

    /** Number of warehouses. Does not grow dynamically */
    extern std::uint16_t warehouses;

    /** Number of items per warehouse. Does not grow dynamically  */
    static constexpr inline std::uint32_t items = 100000U;

    /** Number of districts per warehouse. Does not grow dynamically  */
    static constexpr inline std::uint8_t districts = 10U;

    /** Number of customers per district. Does not grow dynamically  */
    static constexpr inline std::uint32_t customers = 3000U;

    /** Number of orders per district. Does grow dynamically. */
    static constexpr inline std::uint32_t orders = 3000U;

    /** Number of orderlines per order. Does not grow dynamically. */
    static constexpr inline std::uint16_t max_ol_count = 15U;
    static constexpr inline std::uint8_t min_ol_count = 5U;
    static constexpr inline std::uint16_t max_ol = max_ol_count + 1U;

    /** Number of variations of last names. Does not grow dynamically. */
    static constexpr inline std::uint32_t lnames = 1000U;
} // namespace scale


  // random
  class randomGeneratorClass {
  private:
    std::mt19937 mt;
  
  public:
    randomGeneratorClass() {
      std::random_device rnd;
      mt.seed(rnd());
    }
    unsigned int uniformWithin(unsigned int low, unsigned int high)
    {
      std::uniform_int_distribution<> randlh(low, high);
      return randlh(mt);
    }
    unsigned int nonUniformWithin(unsigned int A, unsigned int x, unsigned int y)
    {
      unsigned int C = uniformWithin(0, A);
      return (((uniformWithin(0, A) | uniformWithin(x, y)) + C) % (y - x + 1)) + x;
    }
    void MakeAddress(char *str1, char *str2, char *city, char *state, char *zip)
    {
      MakeAlphaString(10,20,str1); /* Street 1*/
      MakeAlphaString(10,20,str2); /* Street 2*/
      MakeAlphaString(10,20,city); /* City */
      MakeAlphaString(2,2,state); /* State */
      MakeNumberString(9,9,zip); /* Zip */
    }
    int MakeAlphaString(int min, int max, char *str)
    {
      const char character = 'a';

      int length = uniformWithin(min, max);
      for (int i = 0; i < length;  ++i) {
        *str++ = static_cast<char>(character + uniformWithin(0, 25));
      }
      *str = '\0';                     // NOLINT
      return length;
    }
    int MakeNumberString(int min, int max, char *str)
    {
      const char character = '0';
      
      int length = uniformWithin(min, max);
      for (int i = 0; i < length; ++i) {
        *str++ = static_cast<char>(character + uniformWithin(0, 9));
      }
      *str = '\0';                     // NOLINT
      return length;
    }
    inline int RandomNumber(int low, int high) {
      return uniformWithin(low, high); }
  };
  

  // for data generation
  static inline void
  gettimestamp(char *buf, int deltam=0)
  {
    time_t now_t = time(NULL);
    now_t -= deltam * 60L;
    struct tm * now = localtime(&now_t);
    size_t result = strftime(buf, sizeof(TIMESTAMP), "%Y-%m-%e (%a) %H:%M:%S", now);
    *(buf + result) = '\0';  // NOLINT
  }

  static inline
  void getdatestamp(char *buf, int deltad=0)
  {
    time_t now = time(nullptr);
    now -= deltad * (24L * 60L * 60L);
    struct tm *timeptr = localtime(&now);;
    strftime(buf,12,"%Y-%m-%d",timeptr);
  }

  static inline
  void Lastname(int num, char *name)
  {
      const static std::array<const char *,10> n =
          {"BAR", "OUGHT", "ABLE", "PRI", "PRES",
           "ESE", "ANTI", "CALLY", "ATION", "EING"};
      strcpy(name,(n.at(num/100)));
      strcat(name,(n.at((num/10)%10)));
      strcat(name,(n.at(num%10)));
  }

  template<std::size_t N>
  class Permutation {
      std::array<bool, N> cid_array{};
  public:
      Permutation() {
          for (unsigned int i = 0; i < N; i++) cid_array.at(i) = false;
      };
      Permutation(const Permutation& other) = default;
      Permutation(Permutation&& other) = default;
      Permutation& operator=(const Permutation& other) = default;
      Permutation& operator=(Permutation&& other) = default;
      ~Permutation() = default;

      int get_permutation(randomGeneratorClass *randomGenerator)
      {
          while (true) {
              uint32_t r = randomGenerator->RandomNumber(0L, scale::customers -1);
              if (cid_array.at(r)) {       /* This number already taken */
                  continue;
              }
              cid_array.at(r) = true;      /* mark taken */
              return r+1;
          }
      }
  };

  void tpcc_gen_initialize(boost::filesystem::path);
  int tpcc_items_gen(boost::filesystem::path);
  int tpcc_warehouse_gen(boost::filesystem::path, std::uint16_t);

}  // namespace tpcc
