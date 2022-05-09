cmake_minimum_required(VERSION 3.18.1)

project("live")

add_subdirectory(librtmp)
include_directories(librtmp)
# 添加x264头文件
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
include_directories(${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include)
#静态库
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")

#搜索目录下所有cpp文件
file(GLOB cxx_source *.cpp)
add_library(live SHARED
        ${cxx_source}
        AudioChannel.cpp)

find_library(log-lib log)

target_link_libraries(
        live
        rtmp
        x264
        faac
        ${log-lib}
)