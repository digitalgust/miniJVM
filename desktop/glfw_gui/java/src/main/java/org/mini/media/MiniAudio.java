/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.media;

import org.mini.glfw.Glfw;

/**
 * @author Gust
 */
public class MiniAudio {

    static {
        Glfw.loadLib();
    }


    public static final int //
            mal_format_unknown = 0, // Mainly used for indicating an error, but also used as the default for the output format for decoders.
            mal_format_u8 = 1,
            mal_format_s16 = 2, // Seems to be the most widely supported format.
            mal_format_s24 = 3, // Tightly packed. 3 bytes per sample.
            mal_format_s32 = 4,
            mal_format_f32 = 5;
    public static final int //
            mal_device_type_playback = 1,
            mal_device_type_capture = 2,
            ma_device_type_duplex = mal_device_type_playback | mal_device_type_capture;

    public static final int //
            ma_attenuation_model_none = 0,          /* No distance attenuation and no spatialization. */
            ma_attenuation_model_inverse = 1,       /* Equivalent to OpenAL's AL_INVERSE_DISTANCE_CLAMPED. */
            ma_attenuation_model_linear = 2,        /* Linear attenuation. Equivalent to OpenAL's AL_LINEAR_DISTANCE_CLAMPED. */
            ma_attenuation_model_exponential = 3    /* Exponential attenuation. Equivalent to OpenAL's AL_EXPONENT_DISTANCE_CLAMPED. */;

    public static final int //
            MA_SOUND_FLAG_STREAM = 0x00000001,   /* MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_STREAM */
            MA_SOUND_FLAG_DECODE = 0x00000002,   /* MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_DECODE */
            MA_SOUND_FLAG_ASYNC = 0x00000004,   /* MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_ASYNC */
            MA_SOUND_FLAG_WAIT_INIT = 0x00000008,   /* MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_WAIT_INIT */
            MA_SOUND_FLAG_NO_DEFAULT_ATTACHMENT = 0x00000010,   /* Do not attach to the endpoint by default. Useful for when setting up nodes in a complex graph system. */
            MA_SOUND_FLAG_NO_PITCH = 0x00000020,   /* Disable pitch shifting with ma_sound_set_pitch() and ma_sound_group_set_pitch(). This is an optimization. */
            MA_SOUND_FLAG_NO_SPATIALIZATION = 0x00000040; /* Disable spatialization. */
    //


    //error define
    public static final int //
            MA_SUCCESS = 0,
            MA_ERROR = -1,  /* A generic error. */
            MA_INVALID_ARGS = -2,
            MA_INVALID_OPERATION = -3,
            MA_OUT_OF_MEMORY = -4,
            MA_OUT_OF_RANGE = -5,
            MA_ACCESS_DENIED = -6,
            MA_DOES_NOT_EXIST = -7,
            MA_ALREADY_EXISTS = -8,
            MA_TOO_MANY_OPEN_FILES = -9,
            MA_INVALID_FILE = -10,
            MA_TOO_BIG = -11,
            MA_PATH_TOO_LONG = -12,
            MA_NAME_TOO_LONG = -13,
            MA_NOT_DIRECTORY = -14,
            MA_IS_DIRECTORY = -15,
            MA_DIRECTORY_NOT_EMPTY = -16,
            MA_AT_END = -17,
            MA_NO_SPACE = -18,
            MA_BUSY = -19,
            MA_IO_ERROR = -20,
            MA_INTERRUPT = -21,
            MA_UNAVAILABLE = -22,
            MA_ALREADY_IN_USE = -23,
            MA_BAD_ADDRESS = -24,
            MA_BAD_SEEK = -25,
            MA_BAD_PIPE = -26,
            MA_DEADLOCK = -27,
            MA_TOO_MANY_LINKS = -28,
            MA_NOT_IMPLEMENTED = -29,
            MA_NO_MESSAGE = -30,
            MA_BAD_MESSAGE = -31,
            MA_NO_DATA_AVAILABLE = -32,
            MA_INVALID_DATA = -33,
            MA_TIMEOUT = -34,
            MA_NO_NETWORK = -35,
            MA_NOT_UNIQUE = -36,
            MA_NOT_SOCKET = -37,
            MA_NO_ADDRESS = -38,
            MA_BAD_PROTOCOL = -39,
            MA_PROTOCOL_UNAVAILABLE = -40,
            MA_PROTOCOL_NOT_SUPPORTED = -41,
            MA_PROTOCOL_FAMILY_NOT_SUPPORTED = -42,
            MA_ADDRESS_FAMILY_NOT_SUPPORTED = -43,
            MA_SOCKET_NOT_SUPPORTED = -44,
            MA_CONNECTION_RESET = -45,
            MA_ALREADY_CONNECTED = -46,
            MA_NOT_CONNECTED = -47,
            MA_CONNECTION_REFUSED = -48,
            MA_NO_HOST = -49,
            MA_IN_PROGRESS = -50,
            MA_CANCELLED = -51,
            MA_MEMORY_ALREADY_MAPPED = -52,
            MA_FORMAT_NOT_SUPPORTED = -100,// /* General miniaudio-specific errors. */
            MA_DEVICE_TYPE_NOT_SUPPORTED = -101,
            MA_SHARE_MODE_NOT_SUPPORTED = -102,
            MA_NO_BACKEND = -103, MA_NO_DEVICE = -104,
            MA_API_NOT_FOUND = -105,
            MA_INVALID_DEVICE_CONFIG = -106,
            MA_LOOP = -107,
            MA_DEVICE_NOT_INITIALIZED = -200,// /* State errors. */
            MA_DEVICE_ALREADY_INITIALIZED = -201,
            MA_DEVICE_NOT_STARTED = -202,
            MA_DEVICE_NOT_STOPPED = -203,
            MA_FAILED_TO_INIT_BACKEND = -300,// /* Operation errors. */
            MA_FAILED_TO_OPEN_BACKEND_DEVICE = -301,
            MA_FAILED_TO_START_BACKEND_DEVICE = -302,
            MA_FAILED_TO_STOP_BACKEND_DEVICE = -303;//


    public static native long ma_context_init();

    public static native void ma_context_uninit(long handle_context);

    public static native long ma_decoder_init_file(byte[] b);

    public static native long ma_decoder_init_file_ex(byte[] b, int format, int channels, int sampleRate);

    public static native long ma_decoder_init_memory(byte[] data);

    public static native long ma_decoder_init_memory_ex(byte[] data, int format, int channels, int sampleRate);

    public static native void ma_decoder_get_para(long handle_decoder, int[] arr);

    public static native int ma_decoder_read(long handle_device, int frameCount, long pSamples);

    public static native void ma_decoder_uninit(long handle_decoder);

    public static native long ma_data_source_get_next(long handle_datasource);

    public static native void ma_data_source_set_next(long handle_datasource, long handle_datasource_next);

    public static native long ma_device_init(long handle_context, int deviceType, long handle_decode, int format, int channels, int sampleRate);

    public static native void ma_device_uninit(long handle_device);

    public static native void ma_device_start(long handle_device);

    public static native void ma_device_stop(long handle_device);

    public static native int ma_device_is_started(long handle_device);

    public static native long ma_engine_init();

    public static native void ma_engine_uninit(long handle_engine);

    public static native int ma_engine_play_sound(long handle_engine, byte[] path);

    public static native int ma_engine_start(long handle_engine);

    public static native int ma_engine_stop(long handle_engine);

    public static native long ma_engine_get_device(long handle_engine);

    public static native int ma_engine_get_channels(long handle_engine);

    public static native int ma_engine_get_sample_rate(long handle_engine);

    public static native int ma_engine_get_format(long handle_engine);

    public static native void ma_engine_set_volume(long handle_sound, float v);

    public static native float ma_engine_get_volume(long handle_sound);

    public static native void ma_engine_listener_set_position(long handle_engine, int listenerIdx, float x, float y, float z);

    public static native void ma_engine_listener_set_direction(long handle_engine, int listenerIdx, float x, float y, float z);

    public static native void ma_engine_listener_set_world_up(long handle_engine, int listenerIdx, float x, float y, float z);

    public static native long ma_sound_init_from_file(long handle_engine, byte[] path, int flag, long haldle_group, long handle_fence);

    public static native long ma_sound_init_copy(long handle_engine, long haldle_src, int flag, long haldle_group);

    public static native long ma_sound_init_from_data_source(long handle_engine, long haldle_datasrc, int flag, long haldle_group);

    public static native void ma_sound_uninit(long handle_sound);

    public static native void ma_sound_set_min_distance(long handle_sound, float min);

    public static native void ma_sound_set_max_distance(long handle_sound, float max);

    public static native void ma_sound_set_fade_in_milliseconds(long handle_sound, float begin, float end, long millesecond);

    public static native void ma_sound_set_position(long handle_sound, float x, float y, float z);

    public static native void ma_sound_get_position(long handle_sound, float[] position);

    public static native void ma_sound_set_volume(long handle_sound, float v);

    public static native float ma_sound_get_volume(long handle_sound);

    public static native void ma_sound_set_looping(long handle_sound, boolean v);

    public static native boolean ma_sound_is_looping(long handle_sound);

    public static native void ma_sound_set_spatialization_enabled(long handle_sound, boolean enable);

    public static native void ma_sound_set_attenuation_model(long handle_sound, int attenuationMode);

    public static native int ma_sound_start(long handle_sound);

    public static native int ma_sound_stop(long handle_sound);

    public static native boolean ma_sound_is_playing(long handle_sound);

    public static native boolean ma_sound_at_end(long handle_sound);
}
