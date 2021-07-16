#include <jni.h>
#include <string>
#include "fix_method.h"
#include <android/log.h>
#define  LOG_TAG    "method_replace"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)


/**native替换ArtMethod地址，从而实现方法替换*/
extern "C"
JNIEXPORT void JNICALL
Java_com_openxu_fixlibs_NativeMethodFixMng_replaceWhole(JNIEnv *env, jclass clazz,
        jobject bug_method,
jobject fix_method, jint size) {
    FixMethod* bmeth = (FixMethod*) env->FromReflectedMethod(bug_method);
    FixMethod* fmeth = (FixMethod*) env->FromReflectedMethod(fix_method);
    LOGD("xxxxx原始方法指针：%d",bmeth);
    LOGD("√√√√修复方法指针：%d",fmeth);
    //memcpy拷贝地址值。
    memcpy(bmeth, fmeth, size);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_openxu_fixlibs_NativeMethodFixMng_getSizeOfArtMethod(JNIEnv *env, jclass clazz,
jobject f1, jobject f2) {
    //通过反射的Method获取ArtMethod的指针
    FixMethod* f1Method = (FixMethod*) env->FromReflectedMethod(f1);
    FixMethod* f2Method = (FixMethod*) env->FromReflectedMethod(f2);
    //在计算差值时必须先强转为size_t(专门用来存储内存地址值的类型)，否则得到的差值为1，这是因为直接相减被当成是数组相邻两个元素的索引差值
    size_t artMethodSize = (size_t)f2Method - (size_t)f1Method;
    LOGD("第一种方法：f1的起始地址:%d  f2的起始地址：%d  ArtMethod的size:%d", f1Method, f2Method, artMethodSize);
    return artMethodSize;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_openxu_fixlibs_NativeMethodFixMng_getSizeOfArtMethod1(JNIEnv *env, jclass clazz,
jclass native_structs_mode_clazz) {
    //得到NativeStructsMode类中两个静态方法的起始地址
    size_t f1Method = (size_t)env->GetStaticMethodID(native_structs_mode_clazz, "f1", "()V");
    size_t f2Method = (size_t)env->GetStaticMethodID(native_structs_mode_clazz, "f2", "()V");
    //起始地址差值就是ArtMethod的大小
    size_t artMethodSize = f2Method - f1Method;
    LOGD("第二种方法：f1的起始地址:%d  f2的起始地址：%d  ArtMethod的size:%d", f1Method, f2Method, artMethodSize);
    return artMethodSize;
}