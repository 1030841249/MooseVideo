cmake_minimum_required(VERSION 3.18.1)
# 关闭 ssl 不加密 ${CMAKE_C_FLAGS} -DNO_CRYPTO
set(CMAKE_C_FLAGS "${CMAKE_C_FALGS} -DNO_CRYPTO")
file(GLOB rtmp_source *.c)
add_library(rtmp STATIC ${rtmp_source})