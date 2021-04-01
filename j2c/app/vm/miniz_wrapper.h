#ifndef __TINY_JVM_MINIZ_WRAPPER_H__
#define __TINY_JVM_MINIZ_WRAPPER_H__

#include "bytebuf.h"
#include "arraylist.h"
#include "utf8string.h"

s32 zip_loadfile(char *jarpath, char *filename, ByteBuf *buf);

s32 zip_savefile(char *jarpath, char *filename, ByteBuf *buf);

s32 zip_savefile_mem(char *jarpath, char *filename, char *buf, int size);

ArrayList *zip_get_filenames(char *jarpath);

void zip_destory_filenames_list(ArrayList *list);

s32 zip_filecount(char *jarpath);

s32 zip_is_directory(char *jarpath, int index);

s32 zip_compress(char *data, int size, ByteBuf *zip_data);

s32 zip_extract(char *zip_data, int size, ByteBuf *data);

#endif
