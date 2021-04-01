//
// Created by Gust on 2018/1/30.
//

#ifndef JNI_GUI_GLAD_JNI_ASSIST_H
#define JNI_GUI_GLAD_JNI_ASSIST_H

// dont delete comment ,for generate jni
/*
typedef const GLubyte * (APIENTRYP PFNGLGETSTRINGPROC)(GLenum name);
GLAPI PFNGLGETSTRINGPROC glad_glGetString;
#define glGetString glad_glGetString

*/


//int init() {
//    gladLoadGLLoader((GLADloadproc) glfwGetProcAddress);
//    return 0;
//}

//
//int org_mini_gl_GL_glCompileShader(Runtime *runtime, Class *clazz) {
//    JniEnv *env = runtime->jnienv;
//    s32 pos = 0;
//    s32 shader = env->localvar_getInt(runtime, pos++);
//    glCompileShader((GLuint) shader);
//
//    GLint compileResult = GL_TRUE;
//    glGetShaderiv(shader, GL_COMPILE_STATUS, &compileResult);
//    if (compileResult == GL_FALSE) {
//        char szLog[1024] = {0};
//        GLsizei logLen = 0;
//        glGetShaderInfoLog(shader, 1024, &logLen, szLog);
//        fprintf(stderr, "Compile Shader fail error log: %s \nshader code:\n", szLog);
//        glDeleteShader(shader);
//        shader = 0;
//    }
//    return 0;
//}


//int org_mini_gl_GL_glGetString(Runtime *runtime, Class *clazz) {
//    JniEnv *env = runtime->jnienv;
//    s32 pos = 0;
//    s32 name = env->localvar_getInt(runtime, pos++);
//    c8 *cstr = (c8 *) glGetString((GLenum) name);
//    if (cstr) {
//        Instance *jstr = createJavaString(runtime, cstr);
//        env->push_ref(runtime->stack, jstr);
//    } else {
//        env->push_ref(runtime->stack, NULL);
//    }
//    return 0;
//}




#endif //JNI_GUI_GLAD_JNI_ASSIST_H
