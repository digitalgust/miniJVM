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
#include "string.h"

static s32 HASH_TABLE_DEFAULT_SIZE = 16;

static int hash_table_allocate_table(Hashtable *hash_table, s64 size) {
    if (!size || !hash_table) return 0;

    HashtableEntry **new_table = jvm_calloc((unsigned int) size * sizeof(HashtableEntry *));
    if (!new_table) return 0;

    hash_table->table = new_table;
    hash_table->table_size = size;
    return 1;
}


s64 DEFAULT_HASH_FUNC(HashtableKey kmer) {
    // Use FNV-1a hash algorithm for better distribution
    s64 hash = 14695981039346656037ULL; // FNV offset basis
    unsigned char *str = (unsigned char *) &kmer;
    for (size_t i = 0; i < sizeof(HashtableKey); i++) {
        hash ^= str[i];
        hash *= 1099511628211ULL; // FNV prime
    }
    return hash;
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

void hashtable_destroy(Hashtable *hash_table) {
    HashtableEntry *rover;
    HashtableEntry *next;
    s64 i;
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
    if (!hash_table) return;

    HashtableEntry *rover;
    HashtableEntry *next;
    s64 i;
    spin_lock(&hash_table->spinlock);
    {
        // Free all entries
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
        hash_table->version++;

        // Only try to resize if current size is larger than default
        if (hash_table->table_size > HASH_TABLE_DEFAULT_SIZE) {
            HashtableEntry **old_table = hash_table->table;
            s64 old_size = hash_table->table_size;

            // Try to allocate new smaller table
            if (hash_table_allocate_table(hash_table, HASH_TABLE_DEFAULT_SIZE)) {
                // If successful, free old table
                jvm_free(old_table);
            } else {
                // If allocation fails, restore old table but clear all entries
                hash_table->table = old_table;
                hash_table->table_size = old_size;
                memset(hash_table->table, 0, old_size * sizeof(HashtableEntry *));
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
    if (!hash_table) return 0;

    int success = 0;
    spin_lock(&hash_table->spinlock);
    {
        // Check load factor and resize if needed
        if ((hash_table->entries * 3) >= (hash_table->table_size * 2)) {
            if (!hashtable_resize(hash_table, hash_table->table_size * 2)) {
                spin_unlock(&hash_table->spinlock);
                return 0;
            }
        }

        s64 index = hash_table->hash_func(key) % hash_table->table_size;
        HashtableEntry *rover = hash_table->table[index];

        // Try to update existing entry
        while (rover != NULL) {
            if (hash_table->equal_func(rover->key, key) != 0) {
                if (hash_table->value_free_func) {
                    hash_table->value_free_func(rover->value);
                }
                if (hash_table->key_free_func) {
                    hash_table->key_free_func(rover->key);
                }
                rover->key = key;
                rover->value = value;
                success = 1;
                break;
            }
            rover = rover->next;
        }

        // Add new entry if not found
        if (!success) {
            HashtableEntry *newentry = _hashtable_get_entry(hash_table);
            if (newentry) {
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
    s64 index;
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
    s64 index;
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

s64 hashtable_num_entries(Hashtable *hash_table) {
    if (!hash_table) return 0;
    s64 count;
    spin_lock(&hash_table->spinlock);
    count = hash_table->entries;
    spin_unlock(&hash_table->spinlock);
    return count;
}

void hashtable_iterate(Hashtable *hash_table, HashtableIterator *iterator) {
    if (!hash_table || !iterator) return;

    spin_lock(&hash_table->spinlock);
    iterator->hash_table = hash_table;
    iterator->next_entry = NULL;
    iterator->version = hash_table->version;  // Store current version

    s64 chain;
    for (chain = 0; chain < hash_table->table_size; ++chain) {
        if (hash_table->table[chain] != NULL) {
            iterator->next_entry = hash_table->table[chain];
            iterator->next_chain = chain;
            break;
        }
    }
    spin_unlock(&hash_table->spinlock);
}

int hashtable_iter_has_more(HashtableIterator *iterator) {
    if (!iterator || !iterator->hash_table) return 0;

    spin_lock(&iterator->hash_table->spinlock);
    if (iterator->version != iterator->hash_table->version) {
        spin_unlock(&iterator->hash_table->spinlock);
        return 0;
    }
    int has_more = (iterator->next_entry != NULL);
    spin_unlock(&iterator->hash_table->spinlock);
    return has_more;
}

HashtableEntry *hashtable_iter_next_entry(HashtableIterator *iterator) {
    if (!iterator || !iterator->hash_table) return NULL;

    spin_lock(&iterator->hash_table->spinlock);
    if (iterator->version != iterator->hash_table->version) {
        // Concurrent modification detected
        spin_unlock(&iterator->hash_table->spinlock);
        return NULL;
    }

    HashtableEntry *current = iterator->next_entry;
    if (!current) {
        spin_unlock(&iterator->hash_table->spinlock);
        return NULL;
    }

    // Update iterator state
    if (current->next) {
        iterator->next_entry = current->next;
    } else {
        s64 chain = iterator->next_chain + 1;
        iterator->next_entry = NULL;
        while (chain < iterator->hash_table->table_size) {
            if (iterator->hash_table->table[chain]) {
                iterator->next_entry = iterator->hash_table->table[chain];
                break;
            }
            ++chain;
        }
        iterator->next_chain = chain;
    }

    spin_unlock(&iterator->hash_table->spinlock);
    return current;
}

HashtableEntry *hashtable_iter_remove(HashtableIterator *iterator) {
    if (!iterator || !iterator->hash_table) return HASH_NULL;

    spin_lock(&iterator->hash_table->spinlock);
    if (iterator->version != iterator->hash_table->version) {
        // Concurrent modification detected
        spin_unlock(&iterator->hash_table->spinlock);
        return HASH_NULL;
    }

    HashtableEntry *current = iterator->next_entry;
    if (!current) {
        spin_unlock(&iterator->hash_table->spinlock);
        return HASH_NULL;
    }

    --iterator->hash_table->entries;

    // Update iterator state
    if (current->next) {
        iterator->next_entry = current->next;
    } else {
        s64 chain = iterator->next_chain + 1;
        iterator->next_entry = NULL;
        while (chain < iterator->hash_table->table_size) {
            if (iterator->hash_table->table[chain]) {
                iterator->next_entry = iterator->hash_table->table[chain];
                break;
            }
            ++chain;
        }
        iterator->next_chain = chain;
    }

    spin_unlock(&iterator->hash_table->spinlock);
    return current;
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
            if (hti.version != hash_table->version) {
                // Concurrent modification detected
                break;
            }
            func(entry->key, entry->value, para);
        }
    }
    spin_unlock(&hash_table->spinlock);
}

int hashtable_resize(Hashtable *hash_table, s64 size) {
    if (!hash_table || !size || size < HASH_TABLE_DEFAULT_SIZE) return 0;

    HashtableEntry **new_table = jvm_calloc((unsigned int) size * sizeof(HashtableEntry *));
    if (!new_table) return 0;

    HashtableEntry **old_table = hash_table->table;
    s64 old_table_size = hash_table->table_size;

    // Rehash all entries
    s64 i;
    for (i = 0; i < old_table_size; ++i) {
        HashtableEntry *rover = old_table[i];
        while (rover != NULL) {
            HashtableEntry *next = rover->next;
            s64 index = hash_table->hash_func(rover->key) % size;
            rover->next = new_table[index];
            new_table[index] = rover;
            rover = next;
        }
    }

    // Update hash table state
    hash_table->table = new_table;
    hash_table->table_size = size;
    hash_table->version++;
    jvm_free(old_table);

    return 1;
}

