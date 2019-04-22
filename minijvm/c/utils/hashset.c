//
// Created by gust on 2017/8/30.
//
#include <stdlib.h>
#include <stdio.h>
#include "math.h"
#include "d_type.h"
#include "hashset.h"


HashsetEntry *hashset_find_entry(Hashset *set, HashsetKey key);

static s32 HASH_SET_DEFAULT_SIZE = 4 * 1024;
static s32 HASH_SET_POOL_SIZE = 1024 * 1;


HashsetEntry *_hashset_get_entry(Hashset *set) {
    if (set->entry_pool->length) {
        return arraylist_pop_back_unsafe(set->entry_pool);
    } else {
        return jvm_calloc(sizeof(HashsetEntry));
    }
}

static void _hashset_free_entry(Hashset *set, HashsetEntry *entry) {
    if (set->entry_pool->length < HASH_SET_POOL_SIZE)
        arraylist_push_back_unsafe(set->entry_pool, entry);
    else
        jvm_free(entry);
}

void _hashset_clear_pool(Hashset *set) {
    s32 i;
    for (i = 0; i < set->entry_pool->length; i++) {
        ArrayListValue val = arraylist_get_value(set->entry_pool, i);
        jvm_free(val);
    }
}

unsigned int hashset_allocate_table(Hashset *set, unsigned int size) {
    if (size) {
        set->table = jvm_calloc(size *
                                sizeof(HashsetEntry *));
        if (set->table)set->table_size = size;
    }

    return set->table != NULL;
}


u64 _DEFAULT_HASH_FUNC(HashsetKey kmer) {
    return ((u64) (intptr_t) kmer) >> 4;
}

int _DEFAULT_HASH_EQUALS_FUNC(HashsetKey value1, HashsetKey value2) {
    return (value1 == value2);
}


Hashset *hashset_create() {
    Hashset *set;

    set = (Hashset *) jvm_calloc(sizeof(Hashset));

    if (set == NULL) {
        return NULL;
    }

    set->entries = 0;

    if (!hashset_allocate_table(set, HASH_SET_DEFAULT_SIZE)) {
        jvm_free(set);

        return NULL;
    }
    set->entry_pool = arraylist_create(HASH_SET_POOL_SIZE);
    spin_init(&set->lock, 0);
    return set;
}

void hashset_destory(Hashset *set) {
    if (!set)return;
    HashsetEntry *rover;
    HashsetEntry *next;
    u64 i;

    for (i = 0; i < set->table_size; ++i) {
        rover = set->table[i];
        while (rover != NULL) {
            next = rover->next;
            _hashset_free_entry(set, rover);
            rover = next;
        }
    }
    _hashset_clear_pool(set);
    arraylist_destory(set->entry_pool);
    jvm_free(set->table);
    jvm_free(set);
}


void hashset_clear(Hashset *set) {
    HashsetEntry *rover;
    HashsetEntry *next;
    unsigned int i;

    for (i = 0; i < set->table_size; ++i) {
        rover = set->table[i];
        while (rover != NULL) {
            next = rover->next;
            _hashset_free_entry(set, rover);
            rover = next;
        }
        set->table[i] = NULL;
    }
    set->entries = 0;
    _hashset_clear_pool(set);

    if (set->table_size > HASH_SET_DEFAULT_SIZE) {
        jvm_free(set->table);
        set->table = NULL;
        set->table_size = 0;
        if (!hashset_allocate_table(set, HASH_SET_DEFAULT_SIZE)) {
            arraylist_destory(set->entry_pool);
            jvm_free(set);
        }
    }
}


int hashset_put(Hashset *set, HashsetKey key) {
    HashsetEntry *rover;
    HashsetEntry *newentry;
    u64 index;
    int success = 0;


    if ((set->entries << 1) >= set->table_size) {
        hashset_resize(set, set->table_size << 1);
    }
    spin_lock(&set->lock);
    {
        index = _DEFAULT_HASH_FUNC(key) % set->table_size;

        rover = set->table[index];

        while (rover != NULL) {
            if (_DEFAULT_HASH_EQUALS_FUNC(rover->key, key) != 0) {
                success = 2;
                break;
            }
            rover = rover->next;
        }

        if (!success) {
            newentry = _hashset_get_entry(set);
            if (newentry != NULL) {
                newentry->key = key;
                newentry->next = set->table[index];
                set->table[index] = newentry;
                ++set->entries;
                success = 1;
            }
        }
    }
    spin_unlock(&set->lock);
    return success;
}


HashsetKey hashset_get(Hashset *set, HashsetKey key) {
    HashsetKey *ret = HASH_NULL;
    spin_lock(&set->lock);
    {
        HashsetEntry *rover = hashset_find_entry(set, key);
        if (rover)ret = rover->key;
    }
    spin_unlock(&set->lock);
    return ret;
}

HashsetEntry *hashset_find_entry(Hashset *set, HashsetKey key) {
    HashsetEntry *rover;
    u64 index;
    index = _DEFAULT_HASH_FUNC(key) % set->table_size;
    rover = set->table[index];

    while (rover != NULL) {
        if (_DEFAULT_HASH_EQUALS_FUNC(key, rover->key) != 0) {
            return rover;
        }
        rover = rover->next;
    }
    return HASH_NULL;
}

int hashset_remove(Hashset *set, HashsetKey key, int resize) {
    HashsetEntry *rover;
    HashsetEntry *pre;
    HashsetEntry *next;
    u64 index;
    unsigned int result;


    if (resize && (set->entries << 3) < set->table_size) {
        hashset_resize(set, set->table_size >> 1);
    }
    spin_lock(&set->lock);
    {
        index = _DEFAULT_HASH_FUNC(key) % set->table_size;

        result = 0;
        rover = set->table[index];
        pre = rover;

        while (rover != NULL) {
            next = rover->next;
            if (_DEFAULT_HASH_EQUALS_FUNC(key, rover->key) != 0) {
                if (pre == rover)set->table[index] = next;
                else pre->next = next;

                _hashset_free_entry(set, rover);
                --set->entries;
                result = 1;
                break;
            }
            pre = rover;
            rover = next;
        }
    }
    spin_unlock(&set->lock);
    return result;
}

u64 hashset_num_entries(Hashset *set) {
    return set->entries;
}

void hashset_iterate(Hashset *set, HashsetIterator *iterator) {
    u64 chain;

    iterator->set = set;
    iterator->next_entry = NULL;
    for (chain = 0; chain < set->table_size; ++chain) {

        if (set->table[chain] != NULL) {
            iterator->next_entry = set->table[chain];
            iterator->curr_chain = iterator->next_chain = chain;
            break;
        }
    }
}

int hashset_iter_has_more(HashsetIterator *iterator) {
    return iterator->next_entry != NULL;
}


HashsetEntry *hashset_iter_next_entry(HashsetIterator *iterator) {
    Hashset *set;
    u64 chain;

    iterator->curr_entry = iterator->next_entry;
    iterator->curr_chain = iterator->next_chain;

    set = iterator->set;
    if (iterator->next_entry == NULL) {
        return HASH_NULL;
    }

    if (iterator->curr_entry->next != NULL) {
        iterator->next_entry = iterator->curr_entry->next;
    } else {
        chain = iterator->next_chain + 1;
        iterator->next_entry = NULL;
        while (chain < set->table_size) {
            if (set->table[chain] != NULL) {
                iterator->next_entry = set->table[chain];
                break;
            }
            ++chain;
        }
        iterator->next_chain = chain;
    }
    return iterator->curr_entry;
}

HashsetKey hashset_iter_next_key(HashsetIterator *iterator) {
    HashsetEntry *current_entry = hashset_iter_next_entry(iterator);
    if (current_entry)return current_entry->key;
    return HASH_NULL;
}

HashsetKey hashset_iter_remove(HashsetIterator *iterator) {
    HashsetKey key = HASH_NULL;
    if (iterator->curr_entry) {
        Hashset *set = iterator->set;
        spin_lock(&set->lock);
        {
            HashsetEntry *prev = NULL;
            HashsetEntry *rover = set->table[iterator->curr_chain];
            if (rover == iterator->curr_entry) {
                set->table[iterator->curr_chain] = iterator->curr_entry->next;
            } else {
                while (rover != iterator->curr_entry) {
                    prev = rover;
                    rover = rover->next;
                }
                prev->next = rover->next;
            }
            key = iterator->curr_entry->key;
            _hashset_free_entry(set, iterator->curr_entry);
            iterator->curr_entry = NULL;
            --set->entries;
        }
        spin_unlock(&set->lock);

    }
    return key;
}

int hashset_resize(Hashset *set, u64 size) {
    HashsetEntry **old_table;
    u64 old_table_size;
    HashsetEntry *rover;
    HashsetEntry *next;
    u64 index;
    unsigned int i;

    spin_lock_count(&set->lock, 1);
    {
        if (size != 0 && size != set->table_size) {
            old_table = set->table;
            old_table_size = set->table_size;

            if (!hashset_allocate_table(set, (unsigned int) size)) {
                printf("CRITICAL: FAILED TO ALLOCATE HASH TABLE!\n");
                set->table = old_table;
                set->table_size = old_table_size;
                old_table = NULL;

            } else {
                for (i = 0; i < old_table_size; ++i) {
                    rover = old_table[i];

                    while (rover != NULL) {
                        next = rover->next;
                        index = _DEFAULT_HASH_FUNC(rover->key) % set->table_size;
                        rover->next = set->table[index];
                        set->table[index] = rover;
                        rover = next;
                    }
                }
            }
            if (old_table)jvm_free(old_table);
        }
    }
    spin_unlock(&set->lock);

    return 1;
}


u64 hashset_count(Hashset *set) {
    HashsetEntry *rover;
    u64 i, count;
    count = 0;

    for (i = 0; i < set->table_size; i++) {
        rover = set->table[i];
        while (rover != NULL) {
            count++;
            rover = rover->next;
        }
    }

    return count;
}
