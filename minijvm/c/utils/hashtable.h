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

/**
 * @file hash-table.h
 *
 * @brief Hash table.
 *
 * A hash table stores a set of values which can be addressed by a 
 * key.  Given the key, the corresponding value can be looked up
 * quickly.
 *
 * To create a hash table, use @ref hash_table_new.  To destroy a 
 * hash table, use @ref hash_table_free.
 *
 * To insert a value into a hash table, use @ref hash_table_insert.
 *
 * To remove a value from a hash table, use @ref hash_table_remove.
 *
 * To look up a value by its key, use @ref hash_table_lookup.
 *
 * To iterate over all values in a hash table, use 
 * @ref hash_table_iterate to initialise a @ref HashtableIterator
 * structure.  Each value can then be read in turn using 
 * @ref hash_table_iter_next and @ref hash_table_iter_has_more.
 */

#ifndef ALGORITHM_HASH_TABLE_H
#define ALGORITHM_HASH_TABLE_H

#ifdef __cplusplus
extern "C" {
#endif

#include "d_type.h"
#include "spinlock.h"
#include "arraylist.h"

/**
 * A hash table structure.
 */

typedef struct _Hashtable Hashtable;

typedef Hashtable *hmap_t;
/**
 * Structure used to iterate over a hash table.
 */

typedef struct _HashtableIterator HashtableIterator;

/**
 * Internal structure representing an entry in a hash table.
 */

typedef struct _HashtableEntry HashtableEntry;

/**
 * A key to look up a value in a @ref Hashtable.
 */

typedef void *HashtableKey;

/**
 * A value stored in a @ref Hashtable.
 */

typedef void *HashtableValue;

/**
 * Definition of a @ref HashtableIterator.
 */

struct _HashtableIterator {
    Hashtable *hash_table;
    HashtableEntry *next_entry;
    u64 next_chain;
};

/**
 * A null @ref HashtableValue.
 */
#ifndef HASH_NULL
#define HASH_NULL ((void *) 0)
#endif

/**
 * Hash function used to generate hash values for keys used in a hash
 * table.
 *
 * @param value  The value to generate a hash value for.
 * @return       The hash value.
 */

typedef u64 (*HashtableHashFunc)(HashtableKey value);

/**
 * Function used to compare two keys for equality.
 *
 * @return   Non-zero if the two keys are equal, zero if the keys are
 *           not equal.
 */

typedef int (*HashtableEqualFunc)(HashtableKey value1, HashtableKey value2);

/**
 * Type of function used to free keys when entries are removed from a
 * hash table.
 */

typedef void (*HashtableKeyFreeFunc)(HashtableKey value);

/**
 * Type of function used to free values when entries are removed from a
 * hash table.
 */

typedef void (*HashtableValueFreeFunc)(HashtableValue value);


typedef void (*HashtableIteratorFunc)(HashtableKey key, HashtableValue value, void *para);


struct _HashtableEntry {
    HashtableKey key;
    HashtableValue value;
    HashtableEntry *next;
};

struct _Hashtable {
    HashtableEntry **table;
    u64 table_size;
    HashtableHashFunc hash_func;
    HashtableEqualFunc equal_func;
    HashtableKeyFreeFunc key_free_func;
    HashtableValueFreeFunc value_free_func;
    u64 entries;
    spinlock_t spinlock;
};

/**
 * Create a new hash table.
 *
 * @param hash_func            Function used to generate hash keys for the
 *                             keys used in the table.
 * @param equal_func           Function used to test keys used in the table
 *                             for equality.
 * @return                     A new hash table structure, or NULL if it
 *                             was not possible to allocate the new hash
 *                             table.
 */

Hashtable *hashtable_create(HashtableHashFunc hash_func,
                            HashtableEqualFunc equal_func);


u64 DEFAULT_HASH_FUNC(HashtableKey kmer);

int DEFAULT_HASH_EQUALS_FUNC(HashtableValue value1, HashtableValue value2);

/**
 * Destroy a hash table.
 *
 * @param hash_table           The hash table to destroy.
 */

void hashtable_destory(Hashtable *hash_table);

void hashtable_clear(Hashtable *hash_table);

/**
 * Register functions used to free the key and value when an entry is
 * removed from a hash table.
 *
 * @param hash_table           The hash table.
 * @param key_free_func        Function used to free keys.
 * @param value_free_func      Function used to free values.
 */

void hashtable_register_free_functions(Hashtable *hash_table,
                                       HashtableKeyFreeFunc key_free_func,
                                       HashtableValueFreeFunc value_free_func);

/**
 * Insert a value into a hash table, overwriting any existing entry
 * using the same key.
 *
 * @param hash_table           The hash table.
 * @param key                  The key for the new value.
 * @param value                The value to insert.
 * @return                     Non-zero if the value was added successfully,
 *                             or zero if it was not possible to allocate
 *                             memory for the new entry.
 */

int hashtable_put(Hashtable *hash_table,
                  HashtableKey key,
                  HashtableValue value);

/**
 * Look up a value in a hash table by key.
 *
 * @param hash_table          The hash table.
 * @param key                 The key of the value to look up.
 * @return                    The value, or @ref HASH_TABLE_NULL if there
 *                            is no value with that key in the hash table.
 */

HashtableValue hashtable_get(Hashtable *hash_table,
                             HashtableKey key);

/**
 * Remove a value from a hash table.
 *
 * @param hash_table          The hash table.
 * @param key                 The key of the value to remove.
 * @return                    Non-zero if a key was removed, or zero if the
 *                            specified key was not found in the hash table.
 */

int hashtable_remove(Hashtable *hash_table, HashtableKey key, int resize);

/**
 * Retrieve the number of entries in a hash table.
 *
 * @param hash_table          The hash table.
 * @return                    The number of entries in the hash table.
 */

u64 hashtable_num_entries(Hashtable *hash_table);

/**
 * Initialise a @ref HashtableIterator to iterate over a hash table.
 *
 * @param hash_table          The hash table.
 * @param iter                Pointer to an iterator structure to
 *                            initialise.
 */

void hashtable_iterate(Hashtable *hash_table, HashtableIterator *iter);

/**
 * Determine if there are more keys in the hash table to iterate
 * over.
 *
 * @param iterator            The hash table iterator.
 * @return                    Zero if there are no more values to iterate
 *                            over, non-zero if there are more values to
 *                            iterate over.
 */

int hashtable_iter_has_more(HashtableIterator *iterator);

/**
 * Using a hash table iterator, retrieve the next key.
 *
 * @param iterator            The hash table iterator.
 * @return                    The next key from the hash table, or
 *                            @ref HASH_TABLE_NULL if there are no more
 *                            keys to iterate over.
 */

HashtableValue hashtable_iter_next_value(HashtableIterator *iterator);

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

HashtableKey hashtable_iter_next_key(HashtableIterator *iterator);

void hashtable_iter_safe(Hashtable *hash_table, HashtableIteratorFunc func, void *para);

int hashtable_resize(Hashtable *hash_table, u64 size);

#ifdef __cplusplus
}
#endif

#endif /* #ifndef ALGORITHM_HASH_TABLE_H */

