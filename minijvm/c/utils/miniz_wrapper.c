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
        mz_zip_reader_end(&zipArchive);
    }
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

//    if (filename[0] == '0' && filename[1] == '\0') {
//        s32 debug = 1;
//    }
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
        mz_zip_reader_end(&zipArchive);
    }
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
        mz_zip_reader_end(&zipArchive);
    }
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
        mz_zip_reader_end(&zipArchive);
    }
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
        mz_zip_writer_end(&zipArchive);
    }
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
        mz_zip_reader_end(&zipArchive);
    }
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

        mz_zip_reader_end(&zipArchive);
    }
    return list;
}

void zip_destroy_filenames_list(ArrayList *list) {
    s32 i;
    for (i = 0; i < list->length; i++) {
        Utf8String *ustr = arraylist_get_value_unsafe(list, i);
        if (ustr) {
            utf8_destroy(ustr);
        }
    }
    arraylist_destroy(list);
}

s32 zip_is_directory(char *jarpath, int index) {
    mz_zip_archive zipArchive = {0};

    int ret = -1;
    if (mz_zip_reader_init_file(&zipArchive, jarpath, 0) == MZ_FALSE) {//
        ret = -1;
    } else {

        ret = mz_zip_reader_is_file_a_directory(&zipArchive, index);
        mz_zip_reader_end(&zipArchive);
    }
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
