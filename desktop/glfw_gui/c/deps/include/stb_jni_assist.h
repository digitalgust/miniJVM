//
// Created by Gust on 2018/2/1.
//

#ifndef JNI_GUI_STB_JNI_ASSIST_H
#define JNI_GUI_STB_JNI_ASSIST_H

NUTIL_API int stbtt_InitFont(stbtt_fontinfo *info, const unsigned char *data2, int fontstart);

NUTIL_API float stbtt_ScaleForPixelHeight(const stbtt_fontinfo *info, float pixels);

NUTIL_API void stbtt_GetFontVMetrics(const stbtt_fontinfo *info, int *ascent, int *descent, int *lineGap);

NUTIL_API void stbtt_GetCodepointBitmapBox(const stbtt_fontinfo *font, int codepoint, float scale_x, float scale_y, int *ix0, int *iy0, int *ix1, int *iy1);

NUTIL_API void stbtt_MakeCodepointBitmapOffset(const stbtt_fontinfo *info, unsigned char *output, int output_offset, int out_w, int out_h, int out_stride, float scale_x, float scale_y, int codepoint);

NUTIL_API void stbtt_GetCodepointHMetrics(const stbtt_fontinfo *info, int codepoint, int *advanceWidth, int *leftSideBearing);

NUTIL_API int  stbtt_GetCodepointKernAdvance(const stbtt_fontinfo *info, int ch1, int ch2);

NUTIL_API struct stbtt_fontinfo stbtt_MakeFontInfo();

NUTIL_API int stbi_write_png(char const *filename, int w, int h, int comp, const void *data, int stride_in_bytes);

NUTIL_API int stbi_write_bmp(char const *filename, int w, int h, int comp, const void *data);

NUTIL_API int stbi_write_tga(char const *filename, int w, int h, int comp, const void *data);

NUTIL_API stbi_uc *stbi_load(char const *filename, int *x, int *y, int *comp, int req_comp);

NUTIL_API stbi_uc access_mem(stbi_uc *ptr);

NUTIL_API stbi_uc *stbi_load_from_memory(stbi_uc const *buffer, int len, int *x, int *y, int *comp, int req_comp);

NUTIL_API void stbi_image_free(void *retval_from_stbi_load);

//implementations

NUTIL_API struct stbtt_fontinfo stbtt_MakeFontInfo() {
    stbtt_fontinfo info;
    memset(&info, 0, sizeof(stbtt_fontinfo));
    return info;
}

NUTIL_API void
stbtt_MakeCodepointBitmapOffset(const stbtt_fontinfo *info, unsigned char *output, int output_offset, int out_w,
                                int out_h, int out_stride, float scale_x, float scale_y, int codepoint) {
    stbtt_MakeCodepointBitmap(info, output + output_offset, out_w, out_h, out_stride, scale_x, scale_y,
                              codepoint);
}


NUTIL_API stbi_uc access_mem(stbi_uc *ptr){
    return *ptr;
}

#endif //JNI_GUI_STB_JNI_ASSIST_H
