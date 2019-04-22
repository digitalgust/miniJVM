/*

Copyright (c) 2005-2008, Simon Howard

Permission to use, copy, modify, and/or distribute this software 
for any purpose with or without fee is hereby granted, provided 
that the above copyright notice and this permission notice appear 
in all copies. 

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL 
WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE 
AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR 
CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM 
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, 
NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN      
CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE. 

 */

/* Hash table implementation */

#include <stdlib.h>
#include <stdio.h>
#include "d_type.h"
#include "hashtable.h"
#include "math.h"

static s32 HASH_TABLE_DEFAULT_SIZE = 16;

static int hash_table_allocate_table(Hashtable *hash_table, u64 size) {
    if (size) {
        hash_table->table = jvm_calloc((unsigned int) size *
                                       sizeof(HashtableEntry *));
        if (hash_table->table)hash_table->table_size = size;
    }
    return hash_table->table != NULL;
}


u64 DEFAULT_HASH_FUNC(HashtableKey kmer) {
    return ((u64) (intptr_t) kmer) >> 4;
}

int DEFAULT_HASH_EQUALS_FUNC(HashtableValue value1, HashtableValue value2) {
    return (value1 == value2);
}


static inline HashtableEntry *_hashtable_get_entry(Hashtable *hash_table) {
    return jvm_calloc(sizeof(HashtableEntry));
}

static inline void _hashtable_free_entry(Hashtable *hash_table, HashtableEntry *entry) {

    if (hash_table->key_free_func != NULL) {
        hash_table->key_free_func(entry->key);
    }

    if (hash_table->value_free_func != NULL) {
        hash_table->value_free_func(entry->value);
    }
    jvm_free(entry);
}


//===================== public =============================


Hashtable *hashtable_create(HashtableHashFunc hash_func,
                            HashtableEqualFunc equal_func) {
    Hashtable *hash_table;
    hash_table = (Hashtable *) jvm_calloc(sizeof(Hashtable));

    if (hash_table == NULL) {
        return NULL;
    }

    hash_table->hash_func = hash_func;
    hash_table->equal_func = equal_func;
    hash_table->key_free_func = NULL;
    hash_table->value_free_func = NULL;
    hash_table->entries = 0;

    if (!hash_table_allocate_table(hash_table, HASH_TABLE_DEFAULT_SIZE)) {
        jvm_free(hash_table);

        return NULL;
    }

    spin_init(&hash_table->spinlock, 0);

    return hash_table;
}

void hashtable_destory(Hashtable *hash_table) {
    HashtableEntry *rover;
    HashtableEntry *next;
    u64 i;
    spin_lock(&hash_table->spinlock);
    {
        for (i = 0; i < hash_table->table_size; ++i) {
            rover = hash_table->table[i];
            while (rover != NULL) {
                next = rover->next;
                _hashtable_free_entry(hash_table, rover);
                rover = next;
            }
        }
    }
    spin_unlock(&hash_table->spinlock);

    spin_destroy(&hash_table->spinlock);
    jvm_free(hash_table->table);
    jvm_free(hash_table);

}


void hashtable_clear(Hashtable *hash_table) {
    HashtableEntry *rover;
    HashtableEntry *next;
    u64 i;
    spin_lock(&hash_table->spinlock);
    {
        for (i = 0; i < hash_table->table_size; ++i) {
            rover = hash_table->table[i];
            while (rover != NULL) {
                next = rover->next;
                _hashtable_free_entry(hash_table, rover);
                rover = next;
            }
            hash_table->table[i] = NULL;
        }
        hash_table->entries = 0;


        if (hash_table->table_size > HASH_TABLE_DEFAULT_SIZE) {
            jvm_free(hash_table->table);
            hash_table->table = NULL;
            hash_table->table_size = 0;
            if (!hash_table_allocate_table(hash_table, HASH_TABLE_DEFAULT_SIZE)) {
                jvm_free(hash_table);
            }
        }
    }
    spin_unlock(&hash_table->spinlock);
}

void hashtable_register_free_functions(Hashtable *hash_table,
                                       HashtableKeyFreeFunc key_free_func,
                                       HashtableValueFreeFunc value_free_func) {
    hash_table->key_free_func = key_free_func;
    hash_table->value_free_func = value_free_func;
}

int hashtable_put(Hashtable *hash_table, HashtableKey key, HashtableValue value) {
    HashtableEntry *rover;
    HashtableEntry *newentry;
    u64 index;
    int success = 0;

    spin_lock(&hash_table->spinlock);
    {
        if ((hash_table->entries << 1) >= hash_table->table_size) {
            hashtable_resize(hash_table, hash_table->table_size << 1);
        }
        index = hash_table->hash_func(key) % hash_table->table_size;
        rover = hash_table->table[index];

        while (rover != NULL) {
            if (hash_table->equal_func(rover->key, key) != 0) {
                if (hash_table->value_free_func != NULL) {
                    hash_table->value_free_func(rover->value);
                }

                if (hash_table->key_free_func != NULL) {
                    hash_table->key_free_func(rover->key);
                }

                rover->key = key;
                rover->value = value;
                success = 1;
                break;
            }
            rover = rover->next;
        }
        if (!success) {
            newentry = _hashtable_get_entry(hash_table);

            if (newentry != NULL) {
                newentry->key = key;
                newentry->value = value;
                newentry->next = hash_table->table[index];
                hash_table->table[index] = newentry;
                ++hash_table->entries;

                success = 1;
            }
        }
    }
    spin_unlock(&hash_table->spinlock);
    return success;
}

HashtableValue hashtable_get(Hashtable *hash_table, HashtableKey key) {
    HashtableEntry *rover;
    u64 index;
    HashtableValue value = HASH_NULL;

    spin_lock(&hash_table->spinlock);
    {
        index = hash_table->hash_func(key) % hash_table->table_size;

        rover = hash_table->table[index];

        while (rover != NULL) {
            if (hash_table->equal_func(key, rover->key) != 0) {
                value = rover->value;
                break;
            }
            rover = rover->next;
        }
    }
    spin_unlock(&hash_table->spinlock);
    return value;
}

int hashtable_remove(Hashtable *hash_table, HashtableKey key, int resize) {
    HashtableEntry *rover;
    HashtableEntry *pre;
    HashtableEntry *next;
    u64 index;
    int success = 0;
    spin_lock(&hash_table->spinlock);
    {
        if (resize && (hash_table->entries << 3) < hash_table->table_size) {
            hashtable_resize(hash_table, hash_table->table_size >> 1);
        }
        index = hash_table->hash_func(key) % hash_table->table_size;

        rover = hash_table->table[index];
        pre = rover;

        while (rover != NULL) {
            next = rover->next;
            if (hash_table->equal_func(key, rover->key) != 0) {
                if (pre == rover)hash_table->table[index] = next;
                else pre->next = next;
                _hashtable_free_entry(hash_table, rover);
                --hash_table->entries;
                success = 1;
                break;
            }
            pre = rover;
            rover = next;
        }
    }
    spin_unlock(&hash_table->spinlock);
    return success;
}

u64 hashtable_num_entries(Hashtable *hash_table) {
    return hash_table->entries;
}

void hashtable_iterate(Hashtable *hash_table, HashtableIterator *iterator) {
    u64 chain;
    iterator->hash_table = hash_table;
    iterator->next_entry = NULL;
    for (chain = 0; chain < hash_table->table_size; ++chain) {

        if (hash_table->table[chain] != NULL) {
            iterator->next_entry = hash_table->table[chain];
            iterator->next_chain = chain;
            break;
        }
    }
}

int hashtable_iter_has_more(HashtableIterator *iterator) {
    return iterator->next_entry != NULL;
}

HashtableEntry *hashtable_iter_next_entry(HashtableIterator *iterator) {
    HashtableEntry *current_entry;
    Hashtable *hash_table;
    u64 chain;

    hash_table = iterator->hash_table;

    if (iterator->next_entry == NULL) {
        return HASH_NULL;
    }

    current_entry = iterator->next_entry;

    if (current_entry->next != NULL) {
        iterator->next_entry = current_entry->next;
    } else {
        chain = iterator->next_chain + 1;
        iterator->next_entry = NULL;
        while (chain < hash_table->table_size) {
            if (hash_table->table[chain] != NULL) {
                iterator->next_entry = hash_table->table[chain];
                break;
            }
            ++chain;
        }
        iterator->next_chain = chain;
    }
    return current_entry;
}

HashtableKey hashtable_iter_remove(HashtableIterator *iterator) {
    return HASH_NULL;
}

HashtableValue hashtable_iter_next_value(HashtableIterator *iterator) {
    HashtableEntry *current_entry = hashtable_iter_next_entry(iterator);

    if (current_entry)return current_entry->value;
    return HASH_NULL;
}

HashtableKey hashtable_iter_next_key(HashtableIterator *iterator) {
    HashtableEntry *current_entry = hashtable_iter_next_entry(iterator);

    if (current_entry)return current_entry->key;
    return HASH_NULL;
}

void hashtable_iter_safe(Hashtable *hash_table, HashtableIteratorFunc func, void *para) {
    HashtableIterator hti;
    spin_lock(&hash_table->spinlock);
    {
        hashtable_iterate(hash_table, &hti);
        for (; hashtable_iter_has_more(&hti);) {
            HashtableEntry *entry = hashtable_iter_next_entry(&hti);
            func(entry->key, entry->value, para);
        }
    }
    spin_unlock(&hash_table->spinlock);
}

int hashtable_resize(Hashtable *hash_table, u64 size) {
    HashtableEntry **old_table;
    u64 old_table_size;
    HashtableEntry *rover;
    HashtableEntry *next;
    u64 index;
    u64 i;


    if (size) {
        old_table = hash_table->table;
        old_table_size = hash_table->table_size;

        if (!hash_table_allocate_table(hash_table, size)) {
            printf("CRITICAL: FAILED TO ALLOCATE HASH TABLE!\n");

            hash_table->table = old_table;
            hash_table->table_size = old_table_size;

            return 0;
        }

        for (i = 0; i < old_table_size; ++i) {
            rover = old_table[i];

            while (rover != NULL) {
                next = rover->next;
                index = hash_table->hash_func(rover->key) % hash_table->table_size;
                rover->next = hash_table->table[index];
                hash_table->table[index] = rover;
                rover = next;
            }
        }
        jvm_free(old_table);
    }

    return 1;
}

