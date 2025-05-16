#include "miniz.h"
#include "miniz_wrapper.h"


s32 zip_loadfile(char const *jarpath, char const *filename, ByteBuf *buf) {
    int file_index = 0;
    mz_zip_archive zipArchive = {0};
    mz_zip_archive_file_stat file_stat = {0};

    //skit the first '/'
    if (filename && filename[0] == '/') {
        filename += 1;
    }

    int ret = 0;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        ret = -1;
    } else {

        file_index = mz_zip_reader_locate_file(&zipArchive, filename, NULL, 0);//
        if (!mz_zip_reader_file_stat(&zipArchive, file_index, &file_stat)) {
            ret = -1;
        } else {
            size_t uncompressed_size = (size_t) file_stat.m_uncomp_size;
            void *p = mz_zip_reader_extract_file_to_heap(&zipArchive, file_stat.m_filename, &uncompressed_size, 0);
            if (!p) {
                ret = -1;
            } else {

                bytebuf_write_batch(buf, p, (s32) uncompressed_size);
                mz_free(p);
            }
        }
    }
    mz_zip_reader_end(&zipArchive);
    return ret;
}


s32 zip_loadfile_to_mem(char const *jarpath, char const *filename, c8 *buf, s64 bufsize) {
    int file_index = 0;
    mz_zip_archive zipArchive = {0};
    mz_zip_archive_file_stat file_stat = {0};

    //skit the first '/'
    if (filename && filename[0] == '/') {
        filename += 1;
    }

    int ret = 0;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        ret = -1;
    } else {

        file_index = mz_zip_reader_locate_file(&zipArchive, filename, NULL, 0);//
        if (!mz_zip_reader_file_stat(&zipArchive, file_index, &file_stat)) {
            ret = -1;
        } else {
            size_t uncompressed_size = (size_t) file_stat.m_uncomp_size;
            mz_bool op = mz_zip_reader_extract_file_to_mem(&zipArchive, file_stat.m_filename, buf, bufsize, MZ_ZIP_FLAG_CASE_SENSITIVE);
            if (!op) {
                ret = -1;
            }
        }
    }
    mz_zip_reader_end(&zipArchive);
    return ret;
}

s64 zip_get_file_unzip_size(char const *jarpath, char const *filename) {
    int file_index = 0;
    mz_zip_archive zipArchive = {0};
    mz_zip_archive_file_stat file_stat = {0};

    //skit the first '/'
    if (filename && filename[0] == '/') {
        filename += 1;
    }

    s64 ret = 0;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        ret = -1;
    } else {

        file_index = mz_zip_reader_locate_file(&zipArchive, filename, NULL, 0);//
        if (!mz_zip_reader_file_stat(&zipArchive, file_index, &file_stat)) {
            ret = -1;
        } else {
            size_t uncompressed_size = (size_t) file_stat.m_uncomp_size;
            ret = uncompressed_size;
        }
    }
    mz_zip_reader_end(&zipArchive);
    return ret;
}

s32 zip_get_file_index(char const *jarpath, char const *filename) {
    int file_index = 0;
    mz_zip_archive zipArchive = {0};
    mz_zip_archive_file_stat file_stat = {0};

    //skit the first '/'
    if (filename && filename[0] == '/') {
        filename += 1;
    }

    int ret = 0;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        ret = -1;
    } else {

        file_index = mz_zip_reader_locate_file(&zipArchive, filename, NULL, 0);//
        if (!mz_zip_reader_file_stat(&zipArchive, file_index, &file_stat)) {
            ret = -1;
        } else {
            ret = file_index;
        }
    }
    mz_zip_reader_end(&zipArchive);
    return ret;
}


s32 zip_savefile_mem(char const *jarpath, char const *filename, char const *buf, int size) {
    int file_index = 0;
    mz_zip_archive zipArchive = {0};
    mz_zip_archive_file_stat file_stat = {0};

    int ret = 0;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        if (mz_zip_writer_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
            ret = -1;
        }
    } else {
        if (mz_zip_writer_init_from_reader(&zipArchive, jarpath) == MZ_FALSE) {//
            ret = -1;
        }
    }
    if (ret == 0) {
        if (mz_zip_writer_add_mem(&zipArchive, filename, buf, size, MZ_DEFAULT_COMPRESSION) == MZ_FALSE) {//
            ret = -1;
        }
        if (mz_zip_writer_finalize_archive(&zipArchive) == MZ_FALSE) {//
            ret = -1;
        }
    }
    mz_zip_writer_end(&zipArchive);
    return ret;
}


s32 zip_savefile(char const *jarpath, char const *filename, ByteBuf *buf) {
    return zip_savefile_mem(jarpath, filename, buf->buf, buf->wp);
}

s32 zip_filecount(char *jarpath) {
    int file_index = 0;
    mz_zip_archive zipArchive = {0};
    mz_zip_archive_file_stat file_stat = {0};

    int ret = 0;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        ret = -1;
    } else {
        ret = mz_zip_reader_get_num_files(&zipArchive);
    }
    mz_zip_reader_end(&zipArchive);
    return ret;
}

ArrayList *zip_get_filenames(char *jarpath) {
    mz_zip_archive zipArchive = {0};

    ArrayList *list = arraylist_create(0);
    int ret = 0;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        ret = -1;
    } else {
        int count = mz_zip_reader_get_num_files(&zipArchive);
        int i;
        for (i = 0; i < count; i++) {
            char buf[1024];
            mz_uint requiremore = mz_zip_reader_get_filename(&zipArchive, i, buf, 1024);
            if (strlen(buf)) {
                Utf8String *ustr = utf8_create_c(buf);
                arraylist_push_back_unsafe(list, ustr);
            }
        }

    }
    mz_zip_reader_end(&zipArchive);
    return list;
}

void zip_destory_filenames_list(ArrayList *list) {
    s32 i;
    for (i = 0; i < list->length; i++) {
        Utf8String *ustr = arraylist_get_value_unsafe(list, i);
        if (ustr) {
            utf8_destory(ustr);
        }
    }
    arraylist_destory(list);
}

s32 zip_is_directory(char *jarpath, int index) {
    mz_zip_archive zipArchive = {0};

    int ret = -1;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        ret = -1;
    } else {

        ret = mz_zip_reader_is_file_a_directory(&zipArchive, index);
    }
    mz_zip_reader_end(&zipArchive);
    return ret;
}

s32 zip_extract(char *zip_data, int size, ByteBuf *data) {
    mz_zip_archive zipArchive = {0};
    mz_zip_archive_file_stat file_stat = {0};
    int ret = 0;
    if (mz_zip_reader_init_mem(&zipArchive, zip_data, size, 0) == MZ_FALSE) {//
        ret = -1;
    } else {
        if (!mz_zip_reader_file_stat(&zipArchive, 0, &file_stat)) {
            ret = -1;
        } else {
            size_t uncompressed_size = (size_t) file_stat.m_uncomp_size;
            void *p = mz_zip_reader_extract_to_heap(&zipArchive, 0, &uncompressed_size, 0);
            if (!p) {
                ret = -1;
            } else {

                bytebuf_write_batch(data, p, (s32) uncompressed_size);
                mz_free(p);
            }
        }
        mz_zip_reader_end(&zipArchive);
    }
    return ret;
}

s32 zip_compress(char *data, int size, ByteBuf *zip_data) {
    mz_zip_archive zipArchive = {0};
    int ret = 0;
    if (mz_zip_writer_init_heap(&zipArchive, 0, 0) == MZ_FALSE) {//
        ret = -1;
    } else {
        if (mz_zip_writer_add_mem(&zipArchive, "", data, size, MZ_DEFAULT_COMPRESSION) == MZ_FALSE) {//
            ret = -1;
        } else {
            void *buf = NULL;
            size_t outSize = 0;
            if (mz_zip_writer_finalize_heap_archive(&zipArchive, &buf, &outSize) == MZ_FALSE) {//
                ret = -1;
            } else {
                bytebuf_write_batch(zip_data, buf, (s32) outSize);
            }
        }
        mz_zip_writer_end(&zipArchive);
    }
    return ret;
}

// 修改gzip_compress函数，生成完整的GZIP格式
s32 gzip_compress(char *data, int size, ByteBuf *gzip_data) {
    mz_stream stream = {0};
    int status;
    mz_ulong crc = mz_crc32(MZ_CRC32_INIT, (const unsigned char *) data, size);

    // 分配压缩缓冲区 - 预留头部(10字节)和尾部(8字节)的空间
    mz_ulong compressed_size = mz_compressBound(size) + 18;
    unsigned char *compressed_data = (unsigned char *) MZ_MALLOC(compressed_size);
    if (!compressed_data) {
        return -1;
    }

    // 写入GZIP头部
    compressed_data[0] = 0x1F;  // ID1
    compressed_data[1] = 0x8B;  // ID2
    compressed_data[2] = 0x08;  // CM = DEFLATE
    compressed_data[3] = 0x00;  // FLG
    compressed_data[4] = 0x00;  // MTIME (4 bytes)
    compressed_data[5] = 0x00;
    compressed_data[6] = 0x00;
    compressed_data[7] = 0x00;
    compressed_data[8] = 0x00;  // XFL
    compressed_data[9] = 0xFF;  // OS = unknown

    // 设置压缩流
    stream.next_in = (const unsigned char *) data;
    stream.avail_in = size;
    stream.next_out = compressed_data + 10;  // 跳过GZIP头
    stream.avail_out = compressed_size - 18;  // 减去头和尾的大小

    // 初始化deflate
    status = mz_deflateInit2(&stream, MZ_DEFAULT_LEVEL, MZ_DEFLATED, -MZ_DEFAULT_WINDOW_BITS, 8, MZ_DEFAULT_STRATEGY);
    if (status != MZ_OK) {
        MZ_FREE(compressed_data);
        return -1;
    }

    // 执行压缩
    status = mz_deflate(&stream, MZ_FINISH);
    if (status != MZ_STREAM_END) {
        mz_deflateEnd(&stream);
        MZ_FREE(compressed_data);
        return -1;
    }

    // 获取压缩后的大小
    mz_ulong compressed_len = stream.total_out;
    mz_deflateEnd(&stream);

    // 写入CRC32和原始大小(8字节尾部)
    unsigned char *footer = compressed_data + 10 + compressed_len;
    *(mz_uint32 *) footer = crc;
    *(mz_uint32 *) (footer + 4) = size;

    // 写入到输出缓冲区
    bytebuf_write_batch(gzip_data, (char *) compressed_data, compressed_len + 18);
    MZ_FREE(compressed_data);

    return 0;
}

// 修改gzip_extract函数，解析标准GZIP格式
s32 gzip_extract(char *gzip_data, int size, ByteBuf *data) {
    if (size < 18 ||
        (unsigned char) gzip_data[0] != 0x1F ||
        (unsigned char) gzip_data[1] != 0x8B ||
        (unsigned char) gzip_data[2] != 0x08) {
        return -1;  // 无效的GZIP格式
    }

    mz_stream stream = {0};
    int status;

    // 读取原始大小(从尾部)
    mz_uint32 original_size = *(mz_uint32 *) (gzip_data + size - 4);
    mz_uint32 expected_crc = *(mz_uint32 *) (gzip_data + size - 8);

    // 分配解压缓冲区
    unsigned char *uncompressed_data = (unsigned char *) MZ_MALLOC(original_size);
    if (!uncompressed_data) {
        return -1;
    }

    // 设置解压流
    stream.next_in = (unsigned char *) gzip_data + 10;  // 跳过GZIP头
    stream.avail_in = size - 18;  // 减去头和尾的大小
    stream.next_out = uncompressed_data;
    stream.avail_out = original_size;

    // 初始化inflate
    status = mz_inflateInit2(&stream, -MZ_DEFAULT_WINDOW_BITS);
    if (status != MZ_OK) {
        MZ_FREE(uncompressed_data);
        return -1;
    }

    // 执行解压
    status = mz_inflate(&stream, MZ_FINISH);
    mz_inflateEnd(&stream);

    if (status != MZ_STREAM_END || stream.total_out != original_size) {
        MZ_FREE(uncompressed_data);
        return -1;
    }

    // 验证CRC32
    mz_uint32 computed_crc = mz_crc32(MZ_CRC32_INIT, uncompressed_data, original_size);
    if (computed_crc != expected_crc) {
        MZ_FREE(uncompressed_data);
        return -1;
    }

    // 写入到输出缓冲区
    bytebuf_write_batch(data, (char *) uncompressed_data, original_size);
    MZ_FREE(uncompressed_data);

    return 0;
}
