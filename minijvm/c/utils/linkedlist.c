//
// Created by gust on 2017/10/23.
//


#include <stdlib.h>

#include "d_type.h"
#include "linkedlist.h"

/* A doubly-linked list */

struct _ListEntry {
    LinkedListValue data;
    LinkedListEntry *prev;
    LinkedListEntry *next;
};

LinkedList *linkedlist_create() {
    LinkedList *list = jvm_calloc(sizeof(LinkedList));
    list->mNode = jvm_calloc(sizeof(LinkedListEntry));
    spin_init(&list->spinlock, 0);
    return list;
}

void linkedlist_destory(LinkedList *list) {
    _linkedlist_free(list->mNode);
    spin_destroy(&list->spinlock);
    jvm_free(list);
}

void _linkedlist_free(LinkedListEntry *list) {
    LinkedListEntry *entry;

    entry = list;

    while (entry != NULL) {
        LinkedListEntry *next;

        next = entry->next;

        jvm_free(entry);

        entry = next;
    }
}

LinkedListEntry *linkedlist_push_front(LinkedList *list, LinkedListValue data) {
    LinkedListEntry *mNode = list->mNode;
    LinkedListEntry *newentry;

    if (list == NULL) {
        return NULL;
    }
    newentry = jvm_calloc(sizeof(LinkedListEntry));
    if (newentry == NULL) {
        return NULL;
    }
    spin_lock(&list->spinlock);
    {
        newentry->data = data;
        list->length++;
        //
        if (mNode->next == NULL) {  //make a loop
            mNode->prev = mNode->next = newentry;
            newentry->prev = newentry->next = mNode;
        } else {
            LinkedListEntry *oldnext = mNode->next;
            if (oldnext != NULL) {
                oldnext->prev = newentry;
            }
            newentry->prev = mNode;
            newentry->next = oldnext;
            mNode->next = newentry;
        }
    }
    spin_unlock(&list->spinlock);
    return newentry;
}

LinkedListEntry *linkedlist_push_end(LinkedList *list, LinkedListValue data) {

    LinkedListEntry *mNode = list->mNode;
    LinkedListEntry *newentry;

    if (list == NULL) {
        return NULL;
    }
    newentry = jvm_calloc(sizeof(LinkedListEntry));
    if (newentry == NULL) {
        return NULL;
    }
    spin_lock(&list->spinlock);
    {
        newentry->data = data;
        list->length++;
        //
        if (mNode->prev == NULL) {  //make a loop
            mNode->prev = mNode->next = newentry;
            newentry->prev = newentry->next = mNode;
        } else {
            LinkedListEntry *oldprev = mNode->prev;

            if (oldprev) {
                oldprev->next = newentry;
            }
            mNode->prev = newentry;
            newentry->next = mNode;
            newentry->prev = oldprev;
        }
    }
    spin_unlock(&list->spinlock);
    return newentry;
}

LinkedListValue linkedlist_pop_front(LinkedList *list) {
    if (list == NULL) {
        return NULL;
    }
    LinkedListValue val = NULL;
    LinkedListEntry *mNode;
    LinkedListEntry *oldnext;

    spin_lock(&list->spinlock);
    {
        mNode = list->mNode;
        oldnext = mNode->next;
        if (oldnext) {
            mNode->next = oldnext->next;
            oldnext->prev = oldnext->next = NULL;
            val = oldnext->data;

            list->length--;
        }
        if (mNode->next) {
            mNode->next->prev = mNode;
        }
        if (mNode->prev == mNode) {  // remove loop
            mNode->next = mNode->prev = NULL;
        }
    }
    spin_unlock(&list->spinlock);
    if (oldnext) {
        jvm_free(oldnext);
    }
    return val;
}

LinkedListValue linkedlist_pop_end(LinkedList *list) {
    if (list == NULL) {
        return NULL;
    }
    LinkedListValue val = NULL;
    LinkedListEntry *oldprev;
    LinkedListEntry *mNode;

    spin_lock(&list->spinlock);
    {
        mNode = list->mNode;
        oldprev = mNode->prev;
        if (oldprev) {
            mNode->prev = oldprev->prev;
            oldprev->prev = oldprev->next = NULL;
            val = oldprev->data;
            list->length--;
        }
        if (mNode->prev) {
            mNode->prev->next = mNode;
        }
        if (mNode->next == mNode) {
            mNode->next = mNode->prev = NULL;
        }
    }
    spin_unlock(&list->spinlock);

    if (oldprev) {
        jvm_free(oldprev);
    }
    return val;
}

LinkedListValue linkedlist_data(LinkedListEntry *listentry) {
    if (listentry == NULL) {
        return LINKEDLIST_NULL;
    }

    return listentry->data;
}

void linkedlist_set_data(LinkedListEntry *listentry, LinkedListValue value) {
    if (listentry != NULL) {
        listentry->data = value;
    }
}

LinkedListEntry *linkedlist_header(LinkedList *list) {
    if (list) {
        return list->mNode->next;
    }
    return NULL;
}

LinkedListEntry *linkedlist_tail(LinkedList *list) {
    if (list) {
        return list->mNode->prev;
    }
    return NULL;
}

LinkedListEntry *linkedlist_prev(LinkedList *list, LinkedListEntry *entry) {
    if (entry == NULL) {
        return NULL;
    }
    if (entry->prev == list->mNode)return NULL;
    return entry->prev;
}


LinkedListEntry *linkedlist_next(LinkedList *list, LinkedListEntry *entry) {
    if (entry == NULL) {
        return NULL;
    }
    if (entry->next == list->mNode)return NULL;
    return entry->next;
}


void linkedlist_remove(LinkedList *list, LinkedListEntry *entry) {
    if (entry == NULL) {
        return;
    }

    if (entry->next == list->mNode && entry->prev == list->mNode) {
        list->mNode->prev = list->mNode->next = NULL;
    } else {
        entry->prev->next = entry->next;
        entry->next->prev = entry->prev;
    }
    jvm_free(entry);
    list->length--;
}


void linkedlist_iter_safe(LinkedList *list, LinkedListIteratorFunc func, void *para) {
    spin_lock(&list->spinlock);
    LinkedListEntry *entry = linkedlist_header(list);
    while (entry) {
        LinkedListEntry *tmp = entry;
        entry = linkedlist_next(list, entry);
        func(list, tmp, para);
    }
    spin_unlock(&list->spinlock);
}