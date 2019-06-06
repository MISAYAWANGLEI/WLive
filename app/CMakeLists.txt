cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(src/main/cpp/librtmp)
add_subdirectory(src/main/cpp/libyuv)

#FILE(GLOB source_file src/main/cpp/*.cpp)

add_library(
        native-lib
        SHARED
        src/main/cpp/native-lib.cpp
        src/main/cpp/VideoLive.cpp
        src/main/cpp/CppCallJavaUtils.cpp
        src/main/cpp/AudioLive.cpp
        src/main/cpp/WYuvUtils.cpp)
include_directories(src/main/cpp/libyuv/include)
include_directories(src/main/cpp/include)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/src/main/cpp/libs/${ANDROID_ABI}")

target_link_libraries(
        native-lib
        rtmp
        yuv
        faac
        x264
        log)