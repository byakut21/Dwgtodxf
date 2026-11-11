cmake_minimum_required(VERSION 3.22.1)
project(dwgconv C)

# LibreDWG source is fetched by the build (see app/build.gradle.kts -> fetchLibreDwg)
set(LIBREDWG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../libredwg)
if(NOT EXISTS ${LIBREDWG_DIR}/CMakeLists.txt)
  message(FATAL_ERROR "LibreDWG source missing at ${LIBREDWG_DIR}")
endif()

set(LIBREDWG_LIBONLY ON CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(DISABLE_WERROR ON CACHE BOOL "" FORCE)
set(ENABLE_LTO OFF CACHE BOOL "" FORCE)
set(LIBREDWG_DISABLE_JSON ON CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(CMAKE_DISABLE_FIND_PACKAGE_Iconv ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
add_subdirectory(${LIBREDWG_DIR} libredwg)

add_library(dwgconv SHARED jni_bridge.c convert.c)
target_include_directories(dwgconv PRIVATE
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${LIBREDWG_DIR}/include
  ${LIBREDWG_DIR}/src
  ${CMAKE_CURRENT_BINARY_DIR}/libredwg/src)
target_link_libraries(dwgconv redwg m log)
