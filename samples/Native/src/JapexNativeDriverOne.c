
#include <stdio.h>
#include <sys/time.h>
#include <com_sun_japex_jdsl_nativecode_JapexNativeDriver.h>

void setLongParam(JNIEnv *env, jobject this, const char *name, long value);
long getLongParam(JNIEnv *env, jobject this, const char *name);

/*
 * Class:     com_sun_japex_jdsl_nativecode_JapexNativeDriver
 * Method:    initializeDriver
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_initializeDriver
  (JNIEnv *env, jobject this) 
{
    printf("JapexNativeDriverOne: initializeDriver()\n");

    /* --- THE FOLLOWING TWO LINES SHOW HOW TO THROW A RUNTIME EXCEPTION --- 
    jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, exceptionClass, "Error found!"); */
}

/*
 * Class:     com_sun_japex_jdsl_nativecode_JapexNativeDriver
 * Method:    prepare
 * Signature: (Lcom/sun/japex/TestCase;)V
 */
JNIEXPORT void JNICALL Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_prepare
  (JNIEnv *env, jobject this, jobject testCase) 
{
    printf("JapexNativeDriverOne: prepare()\n");
}

/*
 * Class:     com_sun_japex_jdsl_nativecode_JapexNativeDriver
 * Method:    warmup
 * Signature: (Lcom/sun/japex/TestCase;)V
 */
JNIEXPORT void JNICALL Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_warmup
  (JNIEnv *env, jobject this, jobject testCase) 
{
    printf("JapexNativeDriverOne: warmup()\n");
}

/*
 * Class:     com_sun_japex_jdsl_nativecode_JapexNativeDriver
 * Method:    run
 * Signature: (Lcom/sun/japex/TestCase;)V
 */
JNIEXPORT void JNICALL Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_run
  (JNIEnv *env, jobject this, jobject testCase) 
{
    printf("JapexNativeDriverOne: run()\n");
}

/*
 * Class:     com_sun_japex_jdsl_nativecode_JapexNativeDriver
 * Method:    finish
 * Signature: (Lcom/sun/japex/TestCase;)V
 */
JNIEXPORT void JNICALL Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_finish
  (JNIEnv *env, jobject this, jobject testCase) 
{
    printf("JapexNativeDriverOne: finish()\n");
}

/*
 * Class:     com_sun_japex_jdsl_nativecode_JapexNativeDriver
 * Method:    terminateDriver
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_terminateDriver
  (JNIEnv *env, jobject this) 
{
    printf("JapexNativeDriverOne: terminateDriver()\n");
}

/* --- THE FOLLOWING TWO METHODS SHOW HOW TO ACCESS DRIVER PARAMS ----- */

void setLongParam(JNIEnv *env, jobject this, const char *name, long value) 
{
    jclass cls;
    jmethodID mid;

    cls = (*env)->GetObjectClass(env, this);
    mid = (*env)->GetMethodID(env, cls, "setLongParam", "(Ljava/lang/String;J)V");
    (*env)->CallVoidMethod(env, this, mid, (*env)->NewStringUTF(env, name), value);
}

long getLongParam(JNIEnv *env, jobject this, const char *name) 
{
    jclass cls;
    jmethodID mid;

    cls = (*env)->GetObjectClass(env, this);
    mid = (*env)->GetMethodID(env, cls, "getLongParam", "(Ljava/lang/String;)J");
    return (*env)->CallLongMethod(env, this, mid, (*env)->NewStringUTF(env, name));
}

/* ---------------------- DO NOT EDIT BELOW THIS LINE ------------------ */

jlong timeMillis() {
    struct timeval t;
    gettimeofday(&t, 0);
    return (jlong)(((long)t.tv_sec) * 1000L + ((long)t.tv_usec) / 1000L);
}

/*
 * Class:     japexjni_NativeDriver
 * Method:    runLoopDuration
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_runLoopDuration
  (JNIEnv *env, jobject this, jlong duration) 
{
    jclass cls;
    jfieldID fid;
    jobject _testCase;

    /* Get a reference to _testCase */
    cls = (*env)->GetObjectClass(env, this);
    fid = (*env)->GetFieldID(env, cls, "_testCase", "Lcom/sun/japex/TestCaseImpl;");
    _testCase = (*env)->GetObjectField(env, this, fid);
    
    jlong startTime = timeMillis();
    jlong endTime = startTime + duration;

    jlong currentTime = 0;
    jint iterations = 0;
    do {
        Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_run(env, this, _testCase);
        iterations++;
        currentTime = timeMillis();
    } while (endTime >= currentTime);

    return iterations;
}

/*
 * Class:     japexjni_NativeDriver
 * Method:    runLoopIterations
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_runLoopIterations
  (JNIEnv *env, jobject this, jint iterations) 
{
    jclass cls;
    jfieldID fid;
    jobject _testCase;

    /* Get a reference to _testCase */
    cls = (*env)->GetObjectClass(env, this);
    fid = (*env)->GetFieldID(env, cls, "_testCase", "Lcom/sun/japex/TestCaseImpl;");
    _testCase = (*env)->GetObjectField(env, this, fid);

    int i;
    for (i = 0; i < iterations; i++) {
        Java_com_sun_japex_jdsl_nativecode_JapexNativeDriver_run(env, this, _testCase);
    }
}

