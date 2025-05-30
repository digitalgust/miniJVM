/*this file generated by Nanovg_java_2_c.java ,dont modify it manual.*/
#include <stdio.h>
#include <string.h>
#include <minial/miniaudio.h>

//#define MA_DEBUG_OUTPUT

//#define MA_NO_COREAUDIO

#define MINIAUDIO_IMPLEMENTATION

#include "minial/miniaudio.h"


#include "jvm.h"
#include "media.h"

//==========================================================================================
//callback
//==========================================================================================

typedef struct _S24Int {
    u8 c0;
    u8 c1;
    u8 c2;
} S24Int;

void scaleSample(void *pSamples, ma_format format, s32 channels, s32 len, float scale) {
    if (len) {
        switch (format) {
            case ma_format_s16: {
                ma_int16 *dataPtr = pSamples;
                s32 i, imax;
                for (i = 0, imax = len * channels; i < imax; i++) {
                    dataPtr[i] *= scale;
                }
                break;
            }
            case ma_format_u8: {
                ma_uint8 *dataPtr = pSamples;
                s32 i, imax;
                for (i = 0, imax = len * channels; i < imax; i++) {
                    dataPtr[i] *= scale;
                }
                break;
            }
            case ma_format_f32: {
                f32 *dataPtr = pSamples;
                s32 i, imax;
                for (i = 0, imax = len * channels; i < imax; i++) {
                    dataPtr[i] *= scale;
                }
                break;
            }
            case ma_format_s24: {
                S24Int *dataPtr = pSamples;
                S24Int elem;
                s32 i, imax;
                for (i = 0, imax = len * channels; i < imax; i++) {
                    elem = dataPtr[i];
                    s32 d = elem.c0 | (elem.c1 << 8) | (elem.c2 << 16);
                    d *= scale;
                    elem.c0 = d;
                    elem.c1 = d >> 8;
                    elem.c2 = d >> 16;
                    dataPtr[i] = elem;
                }
                break;
            }
            case ma_format_s32: {
                ma_int32 *dataPtr = pSamples;
                s32 i, imax;
                for (i = 0, imax = len * channels; i < imax; i++) {
                    dataPtr[i] *= scale;
                }
                break;
            }
            default: {
            }
        }

    }
}


void on_recv_frames(ma_device *pDevice, ma_uint32 frameCount, const void *pSamples) {
    if (refers._callback_minial_on_recv_frames) {
        Runtime *runtime;
        runtime = getRuntimeCurThread(refers.env);
        if (runtime) {
            //scaleSample(pSamples,pDevice->format,pDevice->channels,frameCount,1.f);

            JniEnv *env = refers.env;
            env->push_long(runtime->stack, (s64) (intptr_t)
                    pDevice);
            env->push_int(runtime->stack, frameCount);
            env->push_long(runtime->stack, (s64) (intptr_t)
                    pSamples);
            s32 ret = env->execute_method(refers._callback_minial_on_recv_frames, runtime);
            if (ret) {
                env->print_exception(runtime);
            }
            runtime = NULL;
        }
    }
}


ma_uint32 on_send_frames(ma_device *pDevice, ma_uint32 frameCount, void *pSamples) {
    if (refers._callback_minial_on_send_frames) {
        Runtime *runtime;
        runtime = getRuntimeCurThread(refers.env);
        if (runtime) {
            JniEnv *env = refers.env;
            env->push_long(runtime->stack, (s64) (intptr_t)
                    pDevice);
            env->push_int(runtime->stack, frameCount);
            env->push_long(runtime->stack, (s64) (intptr_t)
                    pSamples);
            s32 ret = env->execute_method(refers._callback_minial_on_send_frames, runtime);
            if (ret) {
                env->print_exception(runtime);
            }
            s32 v = env->pop_int(runtime->stack);
            //scaleSample(pSamples,pDevice->format,pDevice->channels,v,1.f);
            runtime = NULL;
            return v;
        }
    }
    return 0;
}


void on_stop(ma_device *pDevice) {
    if (refers._callback_minial_on_stop) {
        Runtime *runtime;
        runtime = getRuntimeCurThread(refers.env);
        if (runtime) {
            JniEnv *env = refers.env;
            env->push_long(runtime->stack, (s64) (intptr_t) pDevice);
            s32 ret = env->execute_method(refers._callback_minial_on_stop, runtime);
            if (ret) {
                env->print_exception(runtime);
            }
            runtime = NULL;
        }
    }
}

void data_callback(ma_device *pDevice, void *pOutput, const void *pInput, ma_uint32 frameCount) {
    if (pInput)on_recv_frames(pDevice, frameCount, pInput);
    if (pOutput)on_send_frames(pDevice, frameCount, pOutput);
}
//==========================================================================================
//jni
//==========================================================================================

int org_mini_media_MiniAudio_ma_context_init(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;

    ma_context *handle_context = env->jvm_calloc(sizeof(ma_context));
    if (ma_context_init(NULL, 0, NULL, handle_context) != MA_SUCCESS) {
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t)
                handle_context);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_context_uninit(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_context *handle_context = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    if (handle_context)ma_context_uninit(handle_context);
    env->jvm_free(handle_context);
    return 0;
}


int org_mini_media_MiniAudio_ma_decoder_init_file(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    Instance *path = env->localvar_getRefer(runtime->localvar, pos++);

    ma_decoder *handle_decoder = env->jvm_calloc(sizeof(ma_decoder));
    if (ma_decoder_init_file(path->arr_body, NULL, handle_decoder) != MA_SUCCESS) {
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t)
                handle_decoder);
    }
    return 0;
}


int org_mini_media_MiniAudio_ma_decoder_init_file_ex(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    Instance *path = env->localvar_getRefer(runtime->localvar, pos++);
    s32 format = env->localvar_getInt(runtime->localvar, pos++);
    s32 channels = env->localvar_getInt(runtime->localvar, pos++);
    s32 sampleRate = env->localvar_getInt(runtime->localvar, pos++);


    ma_decoder_config mdc = ma_decoder_config_init(format, channels, sampleRate);

    ma_decoder *handle_decoder = env->jvm_calloc(sizeof(ma_decoder));
    if (ma_decoder_init_file(path->arr_body, &mdc, handle_decoder) != MA_SUCCESS) {
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t)
                handle_decoder);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_decoder_init_memory(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    Instance *data = env->localvar_getRefer(runtime->localvar, pos++);

    ma_decoder *handle_decoder = env->jvm_calloc(sizeof(ma_decoder));
    if (ma_decoder_init_memory(data->arr_body, data->arr_length, NULL, handle_decoder) != MA_SUCCESS) {
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t) handle_decoder);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_decoder_init_memory_ex(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    Instance *data = env->localvar_getRefer(runtime->localvar, pos++);
    s32 format = env->localvar_getInt(runtime->localvar, pos++);
    s32 channels = env->localvar_getInt(runtime->localvar, pos++);
    s32 sampleRate = env->localvar_getInt(runtime->localvar, pos++);


    ma_decoder_config mdc = ma_decoder_config_init(format, channels, sampleRate);

    ma_decoder *handle_decoder = env->jvm_calloc(sizeof(ma_decoder));
    if (ma_decoder_init_memory(data->arr_body, data->arr_length, &mdc, handle_decoder) != MA_SUCCESS) {
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t) handle_decoder);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_decoder_get_para(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_decoder *handle_decoder = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Instance *arr = env->localvar_getRefer(runtime->localvar, pos++);
    if (handle_decoder && arr->arr_length >= 3) {
        env->jarray_set_field(arr, 0, handle_decoder->outputFormat);
        env->jarray_set_field(arr, 1, handle_decoder->outputChannels);
        env->jarray_set_field(arr, 2, handle_decoder->outputSampleRate);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_decoder_read(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_decoder *handle_decoder = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 frameCount = env->localvar_getInt(runtime->localvar, pos++);
    __refer pSamples = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);


    if (handle_decoder) {
        u64 readCount = 0;
        if (ma_decoder_read_pcm_frames(handle_decoder, pSamples, frameCount, &readCount) != MA_SUCCESS) {
            env->push_int(runtime->stack, -1);
        } else {
            env->push_int(runtime->stack, (s32) readCount);
        }
    } else {
        env->push_int(runtime->stack, -1);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_decoder_uninit(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_decoder *handle_decoder = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    if (handle_decoder)ma_decoder_uninit(handle_decoder);
    env->jvm_free(handle_decoder);
    return 0;
}

int org_mini_media_MiniAudio_ma_data_source_get_next(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_data_source *handle_datasource = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    ma_data_source *handle_datasource_next = ma_data_source_get_next(handle_datasource);
    env->push_long(runtime->stack, (s64) (intptr_t) handle_datasource_next);
    return 0;
}

int org_mini_media_MiniAudio_ma_data_source_set_next(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_data_source *handle_datasource = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    ma_data_source *handle_datasource_next = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    ma_data_source_set_next(handle_datasource, handle_datasource_next);
    return 0;
}

void setupCallback(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    c8 *name_s;
    c8 *type_s;
    c8 *clsname_s;
    if (!refers._callback_minial_on_recv_frames) {
        clsname_s = "org/mini/media/MaDevice";
        name_s = "onReceiveFrames";
        type_s = "(JIJ)V";
        Utf8String *clsname = env->utf8_create_part_c(clsname_s, 0, strlen(clsname_s));
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_minial_on_recv_frames = env->find_methodInfo_by_name(clsname, name, type, clazz->jloader, runtime);
        env->utf8_destory(clsname);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    if (!refers._callback_minial_on_send_frames) {
        clsname_s = "org/mini/media/MaDevice";
        name_s = "onSendFrames";
        type_s = "(JIJ)I";
        Utf8String *clsname = env->utf8_create_part_c(clsname_s, 0, strlen(clsname_s));
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_minial_on_send_frames = env->find_methodInfo_by_name(clsname, name, type, clazz->jloader, runtime);
        env->utf8_destory(clsname);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }
    if (!refers._callback_minial_on_stop) {
        clsname_s = "org/mini/media/MaDevice";
        name_s = "onStop";
        type_s = "(J)V";
        Utf8String *clsname = env->utf8_create_part_c(clsname_s, 0, strlen(clsname_s));
        Utf8String *name = env->utf8_create_part_c(name_s, 0, strlen(name_s));
        Utf8String *type = env->utf8_create_part_c(type_s, 0, strlen(type_s));
        refers._callback_minial_on_stop = env->find_methodInfo_by_name(clsname, name, type, clazz->jloader, runtime);
        env->utf8_destory(clsname);
        env->utf8_destory(name);
        env->utf8_destory(type);
    }

}

int org_mini_media_MiniAudio_ma_device_init(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;

    ma_context *handle_context = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 deviceType = env->localvar_getInt(runtime->localvar, pos++);
    __refer handle_userdata = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;


    s32 format = env->localvar_getInt(runtime->localvar, pos++);
    s32 channels = env->localvar_getInt(runtime->localvar, pos++);
    s32 sampleRate = env->localvar_getInt(runtime->localvar, pos++);

    setupCallback(runtime, clazz);

    ma_device_config dev_cfg = ma_device_config_init(deviceType);
    dev_cfg.playback.format = format;
    dev_cfg.playback.channels = channels;
    dev_cfg.capture.format = format;
    dev_cfg.capture.channels = channels;
    dev_cfg.sampleRate = sampleRate;
    dev_cfg.dataCallback = data_callback;
    dev_cfg.stopCallback = on_stop;
    dev_cfg.pUserData = handle_userdata;

    ma_device *handle_device = env->jvm_calloc(sizeof(ma_device));
    if (ma_device_init(handle_context, &dev_cfg, handle_device) != MA_SUCCESS) {
        env->jvm_free(handle_device);
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t) handle_device);
    }

    return 0;
}

int org_mini_media_MiniAudio_ma_device_uninit(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_device *handle_device = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_device_uninit(handle_device);
    env->jvm_free(handle_device);
    return 0;
}

int org_mini_media_MiniAudio_ma_device_start(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_device *handle_device = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_device_start(handle_device);
    return 0;
}

int org_mini_media_MiniAudio_ma_device_stop(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_device *handle_device = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_device_stop(handle_device);
    return 0;
}

int org_mini_media_MiniAudio_ma_device_is_started(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_device *handle_device = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_bool32 r = ma_device_is_started(handle_device);
    env->push_int(runtime->stack, (s32) (intptr_t) r);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_init(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;

    ma_engine *handle_engine = env->jvm_calloc(sizeof(ma_engine));

    if (ma_engine_init(NULL, handle_engine) != MA_SUCCESS) {
        env->jvm_free(handle_engine);
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t) handle_engine);
    }

    return 0;
}

int org_mini_media_MiniAudio_ma_engine_uninit(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_engine_uninit(handle_engine);
    env->jvm_free(handle_engine);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_play_sound(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Instance *path = env->localvar_getRefer(runtime->localvar, pos++);

    ma_result r = ma_engine_play_sound(handle_engine, path->arr_body, NULL);
    env->push_int(runtime->stack, r);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_start(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_result r = ma_engine_start(handle_engine);
    env->push_int(runtime->stack, r);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_stop(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_result r = ma_engine_stop(handle_engine);
    env->push_int(runtime->stack, r);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_get_device(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    s64 r = (s64) (intptr_t) ma_engine_get_device(handle_engine);
    env->push_long(runtime->stack, r);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_get_channels(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    s32 r = ma_engine_get_channels(handle_engine);
    env->push_int(runtime->stack, r);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_get_sample_rate(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    s32 r = ma_engine_get_sample_rate(handle_engine);
    env->push_int(runtime->stack, r);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_get_format(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_device *dev = ma_engine_get_device(handle_engine);
    if (dev) {
        s32 r = dev->playback.format;
        env->push_int(runtime->stack, r);
    } else {
        env->push_int(runtime->stack, ma_format_unknown);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_set_volume(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Int2Float v;
    v.i = env->localvar_getInt(runtime->localvar, pos++);
    ma_engine_set_volume(handle_engine, v.f);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_get_volume(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    if (handle_engine) {
        f32 v = ma_node_output_bus_get_volume(ma_node_graph_get_endpoint(&handle_engine->nodeGraph));
        env->push_float(runtime->stack, v);
    } else {
        env->push_float(runtime->stack, 0.0);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_listener_set_position(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 listenerIdx = env->localvar_getInt(runtime->localvar, pos++);
    Int2Float worldPosX, worldPosY, worldPosZ;
    worldPosX.i = env->localvar_getInt(runtime->localvar, pos++);
    worldPosY.i = env->localvar_getInt(runtime->localvar, pos++);
    worldPosZ.i = env->localvar_getInt(runtime->localvar, pos++);
    ma_engine_listener_set_position(handle_engine, listenerIdx, worldPosX.f, worldPosY.f, worldPosZ.f);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_listener_set_direction(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 listenerIdx = env->localvar_getInt(runtime->localvar, pos++);
    Int2Float forwardX, forwardY, forwardZ;
    forwardX.i = env->localvar_getInt(runtime->localvar, pos++);
    forwardY.i = env->localvar_getInt(runtime->localvar, pos++);
    forwardZ.i = env->localvar_getInt(runtime->localvar, pos++);
    ma_engine_listener_set_direction(handle_engine, listenerIdx, forwardX.f, forwardY.f, forwardZ.f);
    return 0;
}

int org_mini_media_MiniAudio_ma_engine_listener_set_world_up(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 listenerIdx = env->localvar_getInt(runtime->localvar, pos++);
    Int2Float x, y, z;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);
    ma_engine_listener_set_world_up(handle_engine, listenerIdx, x.f, y.f, z.f);
    return 0;
}


int org_mini_media_MiniAudio_ma_sound_init_from_file(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Instance *path = env->localvar_getRefer(runtime->localvar, pos++);
    s32 pflag = env->localvar_getInt(runtime->localvar, pos++);
    ma_sound_group *handle_group = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    ma_fence *pDoneFence = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_sound *handle_sound = env->jvm_calloc(sizeof(ma_sound));

    if (ma_sound_init_from_file(handle_engine, path->arr_body, pflag, handle_group, pDoneFence, handle_sound) != MA_SUCCESS) {
        env->jvm_free(handle_sound);
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t) handle_sound);
    }

    return 0;
}

int org_mini_media_MiniAudio_ma_sound_init_copy(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    ma_sound *handle_sound_src = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 pflag = env->localvar_getInt(runtime->localvar, pos++);
    ma_sound_group *handle_group = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_sound *handle_sound = env->jvm_calloc(sizeof(ma_sound));

    if (ma_sound_init_copy(handle_engine, handle_sound_src, pflag, handle_group, handle_sound) != MA_SUCCESS) {
        env->jvm_free(handle_sound);
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t) handle_sound);
    }

    return 0;
}

int org_mini_media_MiniAudio_ma_sound_init_from_data_source(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_engine *handle_engine = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    ma_data_source *handle_datasource = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 pflag = env->localvar_getInt(runtime->localvar, pos++);
    ma_sound_group *handle_group = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;

    ma_sound *handle_sound = env->jvm_calloc(sizeof(ma_sound));

    if (ma_sound_init_from_data_source(handle_engine, handle_datasource, pflag, handle_group, handle_sound) != MA_SUCCESS) {
        env->jvm_free(handle_sound);
        env->push_long(runtime->stack, 0);
    } else {
        env->push_long(runtime->stack, (s64) (intptr_t) handle_sound);
    }

    return 0;
}

int org_mini_media_MiniAudio_ma_sound_uninit(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    ma_sound_uninit(handle_sound);
    env->jvm_free(handle_sound);
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_set_min_distance(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);

    Int2Float i2f;
    i2f.i = env->localvar_getInt(runtime->localvar, pos++);

    ma_sound_set_min_distance(handle_sound, i2f.f);

    return 0;
}

int org_mini_media_MiniAudio_ma_sound_set_max_distance(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);

    Int2Float i2f;
    i2f.i = env->localvar_getInt(runtime->localvar, pos++);

    ma_sound_set_max_distance(handle_sound, i2f.f);

    return 0;
}

int org_mini_media_MiniAudio_ma_sound_set_fade_in_milliseconds(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Int2Float i2f1, i2f2;
    i2f1.i = env->localvar_getInt(runtime->localvar, pos++);
    i2f2.i = env->localvar_getInt(runtime->localvar, pos++);
    s64 ms = env->localvar_getLong_2slot(runtime->localvar, pos);

    ma_sound_set_fade_in_milliseconds(handle_sound, i2f1.f, i2f2.f, ms);

    return 0;
}

int org_mini_media_MiniAudio_ma_sound_set_position(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);

    Int2Float x, y, z;
    x.i = env->localvar_getInt(runtime->localvar, pos++);
    y.i = env->localvar_getInt(runtime->localvar, pos++);
    z.i = env->localvar_getInt(runtime->localvar, pos++);

    ma_sound_set_position(handle_sound, x.f, y.f, z.f);

    return 0;
}

int org_mini_media_MiniAudio_ma_sound_get_position(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Instance *farr = env->localvar_getRefer(runtime->localvar, pos++);
    if (farr && farr->mb.arr_type_index == DATATYPE_FLOAT && farr->arr_length >= 3) {
        ma_vec3f position = ma_sound_get_position(handle_sound);
        Int2Float x, y, z;
        x.f = position.x;
        y.f = position.y;
        z.f = position.z;
        env->jarray_set_field(farr, 0, x.i);
        env->jarray_set_field(farr, 1, y.i);
        env->jarray_set_field(farr, 2, z.i);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_set_volume(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    Int2Float v;
    v.i = env->localvar_getInt(runtime->localvar, pos++);
    ma_sound_set_volume(handle_sound, v.f);
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_get_volume(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    if (handle_sound) {
        f32 v = ma_sound_get_volume(handle_sound);
        env->push_float(runtime->stack, v);
    } else {
        env->push_float(runtime->stack, 0.0);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_set_looping(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    int iloop = env->localvar_getInt(runtime->localvar, pos++);
    ma_sound_set_looping(handle_sound, iloop);
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_is_looping(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    if (handle_sound) {
        ma_bool32 v = ma_sound_is_looping(handle_sound);
        env->push_int(runtime->stack, v);
    } else {
        env->push_int(runtime->stack, 0);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_set_spatialization_enabled(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 en = env->localvar_getInt(runtime->localvar, pos++);
    ma_sound_set_spatialization_enabled(handle_sound, en ? MA_TRUE : MA_FALSE);
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_set_attenuation_model(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    s32 atten = env->localvar_getInt(runtime->localvar, pos++);
    ma_sound_set_attenuation_model(handle_sound, atten);
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_start(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);

    ma_result re = ma_sound_start(handle_sound);
    env->push_int(runtime->stack, re);
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_stop(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);

    ma_result re = ma_sound_stop(handle_sound);
    env->push_int(runtime->stack, re);
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_is_playing(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    if (handle_sound) {
        ma_bool32 v = ma_sound_is_playing(handle_sound);
        env->push_int(runtime->stack, v);
    } else {
        env->push_int(runtime->stack, 0);
    }
    return 0;
}

int org_mini_media_MiniAudio_ma_sound_at_end(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s32 pos = 0;
    ma_sound *handle_sound = (__refer) (intptr_t) env->localvar_getLong_2slot(runtime->localvar, pos);
    pos += 2;
    if (handle_sound) {
        ma_bool32 v = ma_sound_at_end(handle_sound);
        env->push_int(runtime->stack, v);
    } else {
        env->push_int(runtime->stack, 0);
    }
    return 0;
}

static java_native_method method_minial_table[] = {

        {"org/mini/media/MiniAudio", "ma_context_init",                     "()J",       org_mini_media_MiniAudio_ma_context_init},
        {"org/mini/media/MiniAudio", "ma_context_uninit",                   "(J)V",      org_mini_media_MiniAudio_ma_context_uninit},
        {"org/mini/media/MiniAudio", "ma_decoder_init_file",                "([B)J",     org_mini_media_MiniAudio_ma_decoder_init_file},
        {"org/mini/media/MiniAudio", "ma_decoder_init_file_ex",             "([BIII)J",  org_mini_media_MiniAudio_ma_decoder_init_file_ex},
        {"org/mini/media/MiniAudio", "ma_decoder_init_memory",              "([B)J",     org_mini_media_MiniAudio_ma_decoder_init_memory},
        {"org/mini/media/MiniAudio", "ma_decoder_init_memory_ex",           "([BIII)J",  org_mini_media_MiniAudio_ma_decoder_init_memory_ex},
        {"org/mini/media/MiniAudio", "ma_decoder_get_para",                 "(J[I)V",    org_mini_media_MiniAudio_ma_decoder_get_para},
        {"org/mini/media/MiniAudio", "ma_decoder_read",                     "(JIJ)I",    org_mini_media_MiniAudio_ma_decoder_read},
        {"org/mini/media/MiniAudio", "ma_decoder_uninit",                   "(J)V",      org_mini_media_MiniAudio_ma_decoder_uninit},
        {"org/mini/media/MiniAudio", "ma_data_source_get_next",             "(J)J",      org_mini_media_MiniAudio_ma_data_source_get_next},
        {"org/mini/media/MiniAudio", "ma_data_source_set_next",             "(JJ)V",     org_mini_media_MiniAudio_ma_data_source_set_next},
        {"org/mini/media/MiniAudio", "ma_device_init",                      "(JIJIII)J", org_mini_media_MiniAudio_ma_device_init},
        {"org/mini/media/MiniAudio", "ma_device_uninit",                    "(J)V",      org_mini_media_MiniAudio_ma_device_uninit},
        {"org/mini/media/MiniAudio", "ma_device_start",                     "(J)V",      org_mini_media_MiniAudio_ma_device_start},
        {"org/mini/media/MiniAudio", "ma_device_stop",                      "(J)V",      org_mini_media_MiniAudio_ma_device_stop},
        {"org/mini/media/MiniAudio", "ma_device_is_started",                "(J)I",      org_mini_media_MiniAudio_ma_device_is_started},
        {"org/mini/media/MiniAudio", "ma_engine_init",                      "()J",       org_mini_media_MiniAudio_ma_engine_init},
        {"org/mini/media/MiniAudio", "ma_engine_uninit",                    "(J)V",      org_mini_media_MiniAudio_ma_engine_uninit},
        {"org/mini/media/MiniAudio", "ma_engine_play_sound",                "(J[B)I",    org_mini_media_MiniAudio_ma_engine_play_sound},
        {"org/mini/media/MiniAudio", "ma_engine_start",                     "(J)I",      org_mini_media_MiniAudio_ma_engine_start},
        {"org/mini/media/MiniAudio", "ma_engine_stop",                      "(J)I",      org_mini_media_MiniAudio_ma_engine_stop},
        {"org/mini/media/MiniAudio", "ma_engine_get_device",                "(J)J",      org_mini_media_MiniAudio_ma_engine_get_device},
        {"org/mini/media/MiniAudio", "ma_engine_get_channels",              "(J)I",      org_mini_media_MiniAudio_ma_engine_get_channels},
        {"org/mini/media/MiniAudio", "ma_engine_get_sample_rate",           "(J)I",      org_mini_media_MiniAudio_ma_engine_get_sample_rate},
        {"org/mini/media/MiniAudio", "ma_engine_get_format",                "(J)I",      org_mini_media_MiniAudio_ma_engine_get_format},
        {"org/mini/media/MiniAudio", "ma_engine_set_volume",                "(JF)V",     org_mini_media_MiniAudio_ma_engine_set_volume},
        {"org/mini/media/MiniAudio", "ma_engine_get_volume",                "(J)F",      org_mini_media_MiniAudio_ma_engine_get_volume},
        {"org/mini/media/MiniAudio", "ma_engine_listener_set_position",     "(JIFFF)V",  org_mini_media_MiniAudio_ma_engine_listener_set_position},
        {"org/mini/media/MiniAudio", "ma_engine_listener_set_direction",    "(JIFFF)V",  org_mini_media_MiniAudio_ma_engine_listener_set_direction},
        {"org/mini/media/MiniAudio", "ma_engine_listener_set_world_up",     "(JIFFF)V",  org_mini_media_MiniAudio_ma_engine_listener_set_world_up},
        {"org/mini/media/MiniAudio", "ma_sound_init_from_file",             "(J[BIJJ)J", org_mini_media_MiniAudio_ma_sound_init_from_file},
        {"org/mini/media/MiniAudio", "ma_sound_init_copy",                  "(JJIJ)J",   org_mini_media_MiniAudio_ma_sound_init_copy},
        {"org/mini/media/MiniAudio", "ma_sound_init_from_data_source",      "(JJIJ)J",   org_mini_media_MiniAudio_ma_sound_init_from_data_source},
        {"org/mini/media/MiniAudio", "ma_sound_uninit",                     "(J)V",      org_mini_media_MiniAudio_ma_sound_uninit},
        {"org/mini/media/MiniAudio", "ma_sound_set_min_distance",           "(JF)V",     org_mini_media_MiniAudio_ma_sound_set_min_distance},
        {"org/mini/media/MiniAudio", "ma_sound_set_max_distance",           "(JF)V",     org_mini_media_MiniAudio_ma_sound_set_max_distance},
        {"org/mini/media/MiniAudio", "ma_sound_set_fade_in_milliseconds",   "(JFFJ)V",   org_mini_media_MiniAudio_ma_sound_set_fade_in_milliseconds},
        {"org/mini/media/MiniAudio", "ma_sound_set_position",               "(JFFF)V",   org_mini_media_MiniAudio_ma_sound_set_position},
        {"org/mini/media/MiniAudio", "ma_sound_get_position",               "(J[F)V",    org_mini_media_MiniAudio_ma_sound_get_position},
        {"org/mini/media/MiniAudio", "ma_sound_set_volume",                 "(JF)V",     org_mini_media_MiniAudio_ma_sound_set_volume},
        {"org/mini/media/MiniAudio", "ma_sound_get_volume",                 "(J)F",      org_mini_media_MiniAudio_ma_sound_get_volume},
        {"org/mini/media/MiniAudio", "ma_sound_set_looping",                "(JZ)V",     org_mini_media_MiniAudio_ma_sound_set_looping},
        {"org/mini/media/MiniAudio", "ma_sound_is_looping",                 "(J)Z",      org_mini_media_MiniAudio_ma_sound_is_looping},
        {"org/mini/media/MiniAudio", "ma_sound_set_spatialization_enabled", "(JZ)V",     org_mini_media_MiniAudio_ma_sound_set_spatialization_enabled},
        {"org/mini/media/MiniAudio", "ma_sound_set_attenuation_model",      "(JI)V",     org_mini_media_MiniAudio_ma_sound_set_attenuation_model},
        {"org/mini/media/MiniAudio", "ma_sound_start",                      "(J)I",      org_mini_media_MiniAudio_ma_sound_start},
        {"org/mini/media/MiniAudio", "ma_sound_stop",                       "(J)I",      org_mini_media_MiniAudio_ma_sound_stop},
        {"org/mini/media/MiniAudio", "ma_sound_is_playing",                 "(J)Z",      org_mini_media_MiniAudio_ma_sound_is_playing},
        {"org/mini/media/MiniAudio", "ma_sound_at_end",                     "(J)Z",      org_mini_media_MiniAudio_ma_sound_at_end},
};

s32 count_MiniAudioFuncTable() {
    return sizeof(method_minial_table) / sizeof(java_native_method);
}

__refer ptr_MiniAudioFuncTable() {
    return &method_minial_table[0];
}
