# 该脚本中可看作是调用CMake的一些命令，每个命令后跟着()来配置命令的参数，
# 在Gradle构建是就会触发Cmake命令的执行从而构建出so动态库
# 设置所需的CMake的最低版本。
cmake_minimum_required(VERSION 3.4.1)


# 将编译生成的so库导出到jniLibs目录下
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/jniLibs/${ANDROID_ABI})


# 创建并命名库，将其设置为静态或共享，并提供指向其源代码的相对路径。CMake会为您构建这些库。Gradle会自动将共享库打包到APK中。
add_library(
    XFix    # 指定动态库的名字，将生成libXFix.so
    SHARED       # static 就是a文件，shared 就是so文件
    # 提供源文件的相对路径
        src/main/cpp/fix_method.h
        src/main/cpp/method_replace.cpp
)


# 下面两个命令相当于build.gradle中的dependencies {implementation log-lib}依赖外部库
# 添加Android NDK提供的Android原生API和库，比如我们需要在c++代码中使用logcat，就需要依赖log-lib库
# 添加 NDK API：https://developer.android.google.cn/studio/projects/configure-cmake?hl=zh_cn
find_library(
    log-lib   # ndk库名称（库的id），唯一且不可改变
    log       # 库名称，可随便填写
)
# 为了让您的原生库能够调用log库中的函数，需要使用CMake构建脚本中的target_link_libraries()命令来关联这些库：
target_link_libraries(
    XFix    # 指定目标库
    ${log-lib}   # 将目标库链接到日志库
)
