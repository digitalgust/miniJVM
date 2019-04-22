//
// Created by gust on 2017/8/30.
//

#ifndef MINI_JVM_hashset_H
#define MINI_JVM_hashset_H

#include "arraylist.h"

#ifdef __cplusplus
extern "C" {
#endif


/**
 * A hash table structure.
 */

typedef struct _Hashset Hashset;

/**
 * Structure used to iterate over a hash table.
 */

typedef struct _HashsetIterator HashsetIterator;

/**
 * Internal structure representing an entry in a hash table.
 */

typedef struct _HashsetEntry HashsetEntry;

/**
 * A key to look up a value in a @ref Hashset.
 */

typedef void *HashsetKey;


/**
 * Definition of a @ref HashsetIterator.
 */

struct _HashsetIterator {
    Hashset *set;
//    HashsetEntry *prev_entry;
    HashsetEntry *curr_entry;
    HashsetEntry *next_entry;
    u64 curr_chain;
    u64 next_chain;
};

/**
 * A null @ref HashsetValue.
 */
#ifndef HASH_NULL
#define HASH_NULL ((void *) 0)
#endif


struct _HashsetEntry {
    HashsetKey key;
    HashsetEntry *next;
};

struct _Hashset {
    HashsetEntry **table;
    u64 table_size;
    u64 entries;
    spinlock_t lock;
    ArrayList *entry_pool;
};

/**
 * Create a new hash table.
 *
 * @return                     A new hash table structure, or NULL if it
 *                             was not possible to allocate the new hash
 *                             table.
 */

Hashset *hashset_create(void);
//
//Hashset *hashset_create(HashtableHashFunc hash_func,
//                            HashtableEqualFunc equal_func);

/**
 * Destroy a hash table.
 *
 * @param hash_table           The hash table to destroy.
 */

void hashset_destory(Hashset *hash_table);


void hashset_clear(Hashset *hash_table);
/**
 * Register functions used to free the key and value when an entry is
 * removed from a hash table.
 *
 * @param hash_table           The hash table.
 * @param key_free_func        Function used to free keys.
 * @param value_free_func      Function used to free values.
 */

//void hashset_register_free_functions(Hashset *hash_table,
//                                       HashtableKeyFreeFunc key_free_func);

/**
 * Insert a value into a hash table, overwriting any existing entry
 * using the same key.
 *
 * @param hash_table           The hash table.
 * @param key                  The key for the new value.
 * @return                     Non-zero if the value was added successfully,
 *                             or zero if it was not possible to allocate
 *                             memory for the new entry.
 */

int hashset_put(Hashset *hash_table,
                HashsetKey key);

/**
 * Look up a value in a hash table by key.
 *
 * @param hash_table          The hash table.
 * @param key                 The key of the value to look up.
 * @return                    The value, or @ref HASH_TABLE_NULL if there
 *                            is no value with that key in the hash table.
 */

HashsetKey hashset_get(Hashset *hash_table,
                       HashsetKey key);



/**
 * Remove a value from a hash table.
 *
 * @param hash_table          The hash table.
 * @param key                 The key of the value to remove.
 * @return                    Non-zero if a key was removed, or zero if the
 *                            specified key was not found in the hash table.
 */

int hashset_remove(Hashset *hash_table, HashsetKey key, int resize);

/**
 * Retrieve the number of entries in a hash table.
 *
 * @param hash_table          The hash table.
 * @return                    The number of entries in the hash table.
 */

u64 hashset_num_entries(Hashset *hash_table);

/**
 * Initialise a @ref HashsetIterator to iterate over a hash table.
 *
 * @param hash_table          The hash table.
 * @param iter                Pointer to an iterator structure to
 *                            initialise.
 */

void hashset_iterate(Hashset *hash_table, HashsetIterator *iter);

/**
 * Determine if there are more keys in the hash table to iterate
 * over.
 *
 * @param iterator            The hash table iterator.
 * @return                    Zero if there are no more values to iterate
 *                            over, non-zero if there are more values to
 *                            iterate over.
 */

int hashset_iter_has_more(HashsetIterator *iterator);


/**
 * (EXTENSION!)
 *
 * Using a hash table iterator, retrieve the next key.
 *
 * @param iterator            The hash table iterator.
 * @return                    The next key from the hash table, or
 *                            @ref HASH_TABLE_NULL if there are no more
 *                            keys to iterate over.
 */

HashsetKey hashset_iter_next_key(HashsetIterator *iterator);

HashsetKey hashset_iter_remove(HashsetIterator *iterator);

int hashset_resize(Hashset *hash_table, u64 size);

u64 hashset_count(Hashset *set);

#ifdef __cplusplus
}
#endif


#endif //MINI_JVM_hashset_H
