cmake_minimum_required(VERSION 3.22.1)
project("rvc_module_engine")

set(RVC_SOURCES rvc_engine.cpp)

add_library(
    rvc_main_engine
    SHARED
    ${RVC_SOURCES}
)

target_compile_features(rvc_main_engine PRIVATE cxx_std_20)

target_compile_options(rvc_main_engine PRIVATE
    -Wall
    -fPIC
    -O3
)

target_link_libraries(rvc_main_engine
    android
    log
    z
    m
)
