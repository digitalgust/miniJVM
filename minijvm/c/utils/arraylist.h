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
 * @file arraylist.h
 *
 * @brief Automatically resizing array 
 *
 * ArrayLists are arrays of pointers which automatically increase in 
 * size.
 *
 * To create an ArrayList, use @ref arraylist_new.
 * To destroy an ArrayList, use @ref arraylist_free.
 *
 * To add a value to an ArrayList, use @ref arraylist_prepend, 
 * @ref arraylist_append, or @ref arraylist_insert.
 *
 * To remove a value from an ArrayList, use @ref arraylist_remove
 * or @ref arraylist_remove_range.
 */

#ifndef ALGORITHM_ARRAYLIST_H
#define ALGORITHM_ARRAYLIST_H

#include "spinlock.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * A value to be stored in an @ref ArrayList.
 */

typedef void *ArrayListValue;

/**
 * An ArrayList structure.  New ArrayLists can be created using the 
 * arraylist_new function.
 *
 * @see arraylist_new 
 */

typedef struct _ArrayList ArrayList;

/**
 * Definition of an @ref ArrayList.
 */

struct _ArrayList {

    /** Entries in the array */

    ArrayListValue *data;

    /** Length of the array */

    int length;

    /** Private data and should not be accessed */

    int _alloced;
    spinlock_t spinlock;
};


typedef void (*ArrayListIteratorFunc)(ArrayListValue value, void *para);

/**
 * Compare two values in an arraylist to determine if they are equal.
 *
 * @return Non-zero if the values are not equal, zero if they are equal.
 */

typedef int (*ArrayListEqualFunc)(ArrayListValue value1, ArrayListValue value2);

int DEFAULT_ARRAYLIST_EQUALS_FUNC(ArrayListValue value1, ArrayListValue value2);

/**
 * Compare two values in an arraylist.  Used by @ref arraylist_sort
 * when sorting values.
 *
 * @param value1              The first value.
 * @param value2              The second value.
 * @return                    A negative number if value1 should be sorted
 *                            before value2, a positive number if value2 should
 *                            be sorted before value1, zero if the two values
 *                            are equal.
 */

typedef int (*ArrayListCompareFunc)(ArrayListValue value1,
                                    ArrayListValue value2);

/**
 * Allocate a new ArrayList for use.
 *
 * @param length         Hint to the initialise function as to the amount
 *                       of memory to allocate initially to the ArrayList.
 * @return               A new arraylist, or NULL if it was not possible
 *                       to allocate the memory.
 * @see arraylist_free
 */

ArrayList *arraylist_create(int length);

/**
 * Destroy an ArrayList and free back the memory it uses.
 *
 * @param arraylist      The ArrayList to free.
 */

void arraylist_destory(ArrayList *arraylist);

/**
 * Append a value to the end of an ArrayList.
 *
 * @param arraylist      The ArrayList.
 * @param data           The value to append.
 * @return               Non-zero if the request was successful, zero
 *                       if it was not possible to allocate more memory
 *                       for the new entry.
 */
int arraylist_push_back_unsafe(ArrayList *arraylist, ArrayListValue data);

int arraylist_push_back(ArrayList *arraylist, ArrayListValue data);


int arraylist_remove(ArrayList *arraylist, ArrayListValue data);

int arraylist_remove_unsafe(ArrayList *arraylist, ArrayListValue data);

/**
 * Prepend a value to the beginning of an ArrayList.
 *
 * @param arraylist      The ArrayList.
 * @param data           The value to prepend.
 * @return               Non-zero if the request was successful, zero
 *                       if it was not possible to allocate more memory
 *                       for the new entry.
 */
int arraylist_push_front_unsafe(ArrayList *arraylist, ArrayListValue data);

int arraylist_push_front(ArrayList *arraylist, ArrayListValue data);

/**
 * Remove the entry at the specified location in an ArrayList.
 *
 * @param arraylist      The ArrayList.
 * @param index          The index of the entry to remove.
 */

void arraylist_remove_at(ArrayList *arraylist, int index);


/**
 * Insert a value at the specified index in an ArrayList.
 * The index where the new value can be inserted is limited by the 
 * size of the ArrayList.
 *
 * @param arraylist      The ArrayList.
 * @param index          The index at which to insert the value.
 * @param data           The value.
 * @return               Returns zero if unsuccessful, else non-zero 
 *                       if successful (due to an invalid index or 
 *                       if it was impossible to allocate more memory).
 */

int arraylist_insert(ArrayList *arraylist, int index, ArrayListValue data);


/**
 * default int compare
 * @param a valuea
 * @param b valueb
 * @return equals
 */
int arraylist_compare_ptr(ArrayListValue a, ArrayListValue b);

/**
 * Find the index of a particular value in an ArrayList.
 *
 * @param arraylist      The ArrayList to search.
 * @param equals       Callback function to be invoked to compare
 *                       values in the list with the value to be
 *                       searched for.
 * @param data           The value to search for.
 * @return               The index of the value if found, or -1 if not found.
 */

int arraylist_index_of(ArrayList *arraylist, ArrayListEqualFunc equals, ArrayListValue data);

ArrayListValue arraylist_get_value_unsafe(ArrayList *arraylist, int index);

ArrayListValue arraylist_get_value(ArrayList *arraylist, int index);

ArrayListValue arraylist_pop_back_unsafe(ArrayList *arraylist);

ArrayListValue arraylist_pop_back(ArrayList *arraylist);

ArrayListValue arraylist_pop_front_unsafe(ArrayList *arraylist);

ArrayListValue arraylist_pop_front(ArrayList *arraylist);

ArrayListValue arraylist_peek_front(ArrayList *arraylist);

ArrayListValue arraylist_peek_back(ArrayList *arraylist);

/**
 * Remove all entries from an ArrayList.
 *
 * @param arraylist      The ArrayList.
 */

void arraylist_clear(ArrayList *arraylist);

/** 
 * Sort the values in an ArrayList.
 *
 * @param arraylist      The ArrayList.
 * @param compare_func   Function used to compare values in sorting.
 */

void arraylist_sort(ArrayList *arraylist, ArrayListCompareFunc compare_func);


void arraylist_iter_safe(ArrayList *arraylist, ArrayListIteratorFunc func, void *para);

#ifdef __cplusplus
}
#endif

#endif /* #ifndef ALGORITHM_ARRAYLIST_H */
