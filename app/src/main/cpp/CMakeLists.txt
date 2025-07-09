cmake_minimum_required(VERSION 3.22.1)

project("clasificador_app")

set(OpenCV_STATIC on)

set(OpenCV_DIR /home/jorgemrqz/OpenCV-android-sdk/sdk/native/jni/)

find_package(OpenCV REQUIRED)

include_directories(/home/jorgemrqz/OpenCV-android-sdk/sdk/native/jni/
        include)

add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        native-lib.cpp)

find_library(
        jnigraphics-lib jnigraphics
)

find_library( # Sets the name of the path variable.
        log-lib
        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        ${OpenCV_LIBS}
        ${jnigraphics-lib}
        ${log-lib}
        log)