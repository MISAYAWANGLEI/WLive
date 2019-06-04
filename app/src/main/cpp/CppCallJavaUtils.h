#ifndef CPP_UTILS_H
#define CPP_UTILS_H

#include <jni.h>

class CppCallJavaUtils {
private:
    JavaVM *vm;
    JNIEnv *env;
    jobject instance;
    jmethodID onPrepareMethodID;
public:
    CppCallJavaUtils(JavaVM *vm,JNIEnv *env,jobject instance);
    ~CppCallJavaUtils();
    void onPrepare(int threadID,int isSuccess);//0失败，1成功
};

#endif
