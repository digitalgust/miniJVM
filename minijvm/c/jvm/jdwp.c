//
// Created by gust on 2017/9/20.
//
#include <string.h>
#include "jdwp.h"
#include "jvm_util.h"
#include "garbage.h"

#include "../utils/https/mbedtls/include/mbedtls/net_sockets.h"

struct _JdwpServer {
    MiniJVM *jvm;
    c8 *ip;
    s32 port;
    thrd_t pt_listener;
    thrd_t pt_dispacher;
    mbedtls_net_context srvsock;
    ArrayList *clients;
    ArrayList *event_packets;
    Pairlist *event_sets;
    mtx_t event_sets_lock;

    struct NetWorkLock {
        cnd_t thread_cond;
        mtx_t mutex_lock; //互斥锁
    } netlock;

    Runtime *runtime_jdwp;


    s32 jdwp_eventset_requestid;
    s32 jdwp_eventset_commandid;

    volatile u8 exit;
    u8 mode;
    u8 thread_sync_ignore; //for jdwp invokemethod , the flag indicate that method need not synchronized ,because all of java thread were suspended.
    volatile u8 invoking; //jdwp invoke in flight: other threads run, swallow bp/step events
};

struct _JdwpClient {
    MiniJVM *jvm;
    JdwpServer *jdwpserver;
    mbedtls_net_context sockfd;
    u8 closed;
    u8 close_after_flush; //Dispose must send its reply before disconnecting
    u8 conn_first;
    JdwpPacket *rcvp; // Used for non-blocking reception, writing bytes into the same packet multiple times
    ArrayList *sendq; //per-client outbound queue: replies unicast here, events broadcast into every queue
    JdwpPacket *sending; //dispatcher-owned partial write; never requeue a packet prefix
    s32 send_offset;
    s64 send_deadline;
    u8 reported_backpressure;
    Hashset *temp_obj_holder;
};


s32 jdwp_send_packets(JdwpClient *client);

void event_on_debug_step(JdwpServer *jdwpserver, Runtime *step_runtime);

EventSet *jdwp_eventset_create(JdwpServer *jdwpserver, JdwpClient *client, JdwpPacket *req);

void jdwp_eventset_destroy(EventSet *set);

void jdwp_eventset_remove_on_client_close(JdwpServer *jdwpserver, JdwpClient *client);

void jdwppacket_destroy(JdwpPacket *packet);

JdwpClient *jdwp_client_create(JdwpServer *jdwpserver);

void jdwp_client_destroy(JdwpServer *jdwpserver, JdwpClient *client);

void resume_all_thread(MiniJVM *jvm);

void thread_stop_all(MiniJVM *jvm);

void suspend_all_thread(MiniJVM *jvm);

s32 is_class_exists(MiniJVM *jvm, JClass *clazz);

static Instance *jdwp_threadgroup_of(MiniJVM *jvm, Runtime *r, Instance *jthread);

static Instance *jdwp_threadgroup_parent(MiniJVM *jvm, Runtime *r, Instance *group);

static void jdwp_threadgroup_name(MiniJVM *jvm, Runtime *r, Instance *group, Utf8String *out);

static s32 jdwp_map_thread_status(u8 status);

static s16 jdwp_validate_suspended_thread(JdwpServer *jdwpserver, Instance *jthread, Runtime **thread_runtime);

static s16 jdwp_validate_suspended_frame(JdwpServer *jdwpserver, Instance *jthread, Runtime *frame);

static FieldInfo *jdwp_find_field_id(JClass *clazz, FieldInfo *field_id);

static MethodInfo *jdwp_find_method_id(JClass *clazz, MethodInfo *method_id);

s32 jdwp_thread_dispacher(void *para);

//==================================================    server    ==================================================

Runtime *find_jthread_from_threadlist(MiniJVM *jvm, Instance *jthread);

void netlock_init(JdwpServer *jdwpserver) {
    cnd_init(&jdwpserver->netlock.thread_cond);
    mtx_init(&jdwpserver->netlock.mutex_lock, mtx_recursive | mtx_timed);
}

void netlock_destroy(JdwpServer *jdwpserver) {
    cnd_destroy(&jdwpserver->netlock.thread_cond);
    mtx_destroy(&jdwpserver->netlock.mutex_lock);
}

void netlock_lock(JdwpServer *jdwpserver) {
    mtx_lock(&jdwpserver->netlock.mutex_lock);
}

void netlock_unlock(JdwpServer *jdwpserver) {
    mtx_unlock(&jdwpserver->netlock.mutex_lock);
}

void netlock_wait(JdwpServer *jdwpserver) {
    cnd_wait(&jdwpserver->netlock.thread_cond, &jdwpserver->netlock.mutex_lock);
}

void netlock_wait_time(JdwpServer *jdwpserver, s64 ms) {
    struct timespec t;
    timespec_get(&t, TIME_UTC);
    t.tv_sec += ms / 1000;
    t.tv_nsec += (ms % 1000) * 1000000;
    s32 ret = cnd_timedwait(&jdwpserver->netlock.thread_cond, &jdwpserver->netlock.mutex_lock, &t);
}

void netlock_notify(JdwpServer *jdwpserver) {
    cnd_signal(&jdwpserver->netlock.thread_cond);
}

void netlock_notify_all(JdwpServer *jdwpserver) {
    cnd_broadcast(&jdwpserver->netlock.thread_cond);
}

void jdwp_put_client(ArrayList *clients, JdwpClient *client) {
    arraylist_push_back(clients, client);
}

s32 jdwp_client_count(JdwpServer *jdwpserver) {
    if (!jdwpserver)return 0;
    return jdwpserver->clients->length;
}

s32 jdwp_thread_listener(void *para) {
    JdwpServer *jdwpserver = (JdwpServer *) para;
    mbedtls_net_init(&jdwpserver->srvsock);
    Utf8String *uport = utf8_create();
    utf8_append_s64(uport, jdwpserver->port, 10);
    mbedtls_net_bind(&jdwpserver->srvsock, jdwpserver->ip, utf8_cstr(uport), MBEDTLS_NET_PROTO_TCP);
    utf8_destroy(uport);
    jdwpserver->mode |= JDWP_MODE_LISTEN;

    while (!jdwpserver->exit) {
        JdwpClient *client = jdwp_client_create(jdwpserver);
        s32 ret = mbedtls_net_accept(&jdwpserver->srvsock, &client->sockfd, NULL, 0, NULL);
        if (ret < 0) {
            jdwp_client_destroy(jdwpserver, client);
            jdwpserver->exit = 1;
            break;
        }
        jvm_printf("[JDWP]accepetd client\n");
        mbedtls_net_set_nonblock(&client->sockfd);
        //publish under the same lock broadcasters iterate with: an unlocked
        //push_back racing a locked iteration can walk a reallocating array
        netlock_lock(jdwpserver);
        {
            jdwp_put_client(jdwpserver->clients, client);
            netlock_notify(jdwpserver);
        }
        netlock_unlock(jdwpserver);
    }
    jdwpserver->mode &= ~JDWP_MODE_LISTEN;
    return 0;
}

s32 jdwp_thread_dispacher(void *para) {
    JdwpServer *jdwpserver = (JdwpServer *) para;
    jdwpserver->mode |= JDWP_MODE_DISPATCH;
    s64 last_time = 0;
    s32 wait = 100;
    s32 i;
    while (!jdwpserver->exit) {
        if (current_timestamp() - last_time > 0) {
            netlock_lock(jdwpserver);
            {
                netlock_wait_time(jdwpserver, 100);
            }
            netlock_unlock(jdwpserver);
        }
        for (i = 0; i < jdwpserver->clients->length; i++) {
            JdwpClient *client = arraylist_get_value(jdwpserver->clients, i);
            s32 received = jdwp_client_process(jdwpserver, client);
            s32 sent = jdwp_send_packets(client);
            if (client->closed) {
                //Detach the client from the list under the SAME lock event
                //broadcasters hold, so no thread delivering packets can keep
                //using a freed client/sendq. The eventset cleanup and the
                //free itself run outside the lock: jdwp_eventset_* takes
                //event_sets_lock and the lock order in the event path is
                //event_sets_lock -> netlock, so taking netlock -> event_sets
                //here would deadlock.
                netlock_lock(jdwpserver);
                arraylist_remove(jdwpserver->clients, client);
                netlock_unlock(jdwpserver);
                jdwp_eventset_remove_on_client_close(jdwpserver, client);
                jdwp_client_destroy(jdwpserver, client);
                i--;
                continue;
            }
            if (received > 0 || sent > 0) {
                last_time = current_timestamp() + 3000; //3s不等
            } else {
                last_time = current_timestamp();
            }
        }
    }
    jdwpserver->mode &= ~JDWP_MODE_DISPATCH;
    return 0;
}

/**
 * =========================  jdwpserver   ===========================
 */

s32 jdwp_start_server(MiniJVM *jvm) {
    if (!jvm->jdwp_enable)return 0;
    JdwpServer *jdwpserver = jvm_calloc(sizeof(JdwpServer));
    jdwpserver->jvm = jvm;
    jdwpserver->ip = "0.0.0.0"; //bind to all ip
    jdwpserver->port = jvm->jdwp_port;
    jdwpserver->exit = 0;
    jdwpserver->clients = arraylist_create(0);
    jdwpserver->event_packets = arraylist_create(0);
    jdwpserver->event_sets = pairlist_create(32);
    jdwpserver->runtime_jdwp = runtime_create(jvm);
    jdwpserver->runtime_jdwp->thrd_info->type = THREAD_TYPE_JDWP;
    jdwpserver->jdwp_eventset_requestid = 1; //request id 0 is reserved ("no request") in JDWP
    jdwpserver->invoking = 0;
    mtx_init(&jdwpserver->event_sets_lock, mtx_recursive | mtx_timed);
    netlock_init(jdwpserver);
    jvm->jdwpserver = jdwpserver;

    thrd_create(&jdwpserver->pt_listener, jdwp_thread_listener, jdwpserver);
    thrd_create(&jdwpserver->pt_dispacher, jdwp_thread_dispacher, jdwpserver);

    return 0;
}

s32 jdwp_stop_server(MiniJVM *jvm) {
    if (!jvm->jdwp_enable)return 0;
    JdwpServer *jdwpserver = jvm->jdwpserver;
    //notify attached debuggers before tearing the connection down,
    //otherwise jdb reports a raw disconnect instead of VM exit
    if (jdwpserver->clients->length) {
        event_on_vmdeath(jdwpserver);
        threadSleep(150); //let the dispatcher flush the packet
    }
    jdwpserver->exit = 1;
    mbedtls_net_free(&jdwpserver->srvsock);
    while (jdwpserver->mode != 0) {
        threadSleep(10);
    }
    s32 i;
    //
    for (i = 0; i < jdwpserver->clients->length; i++) {
        JdwpClient *client = arraylist_get_value(jdwpserver->clients, i);
        jdwp_client_destroy(jdwpserver, client);
    }
    arraylist_destroy(jdwpserver->clients);
    //
    spin_lock(&jdwpserver->event_packets->spinlock);
    for (i = 0; i < jdwpserver->event_packets->length; i++) {
        JdwpPacket *packet = arraylist_get_value_unsafe(jdwpserver->event_packets, i);
        jdwppacket_destroy(packet);
    }
    spin_unlock(&jdwpserver->event_packets->spinlock);
    arraylist_destroy(jdwpserver->event_packets);
    //
    mtx_lock(&jdwpserver->event_sets_lock);
    Pair *pair = (Pair *) jdwpserver->event_sets->ptr;
    Pair *end = pair + jdwpserver->event_sets->count;
    for (; pair < end; pair++) {
        EventSet *set = (EventSet *) pair->right;
        jdwp_eventset_destroy(set);
    }
    mtx_unlock(&jdwpserver->event_sets_lock);

    mtx_destroy(&jdwpserver->event_sets_lock);
    pairlist_destroy(jdwpserver->event_sets);
    netlock_destroy(jdwpserver);

    //
    runtime_destroy(jdwpserver->runtime_jdwp);
    //
    thrd_detach(jdwpserver->pt_listener);
    thrd_detach(jdwpserver->pt_dispacher);
    jvm_free(jdwpserver);
    jvm->jdwpserver = NULL;
    jvm->jdwp_enable = 0;
    return 0;
}


JdwpClient *jdwp_client_create(JdwpServer *jdwpserver) {
    JdwpClient *client = jvm_calloc(sizeof(JdwpClient));
    client->jvm = jdwpserver->jvm;
    client->jdwpserver = jdwpserver;
    client->closed = 0;
    client->conn_first = 1;
    mbedtls_net_init(&client->sockfd);
    client->rcvp = NULL;
    client->sendq = arraylist_create(8);
    client->temp_obj_holder = hashset_create();
    return client;
}

void jdwp_client_destroy(JdwpServer *jdwpserver, JdwpClient *client) {
    mbedtls_net_free(&client->sockfd);
    if (client->sending) jdwppacket_destroy(client->sending);
    if (client->rcvp) {
        jdwppacket_destroy(client->rcvp);
    }
    if (client->sendq) {
        s32 i;
        for (i = 0; i < client->sendq->length; i++) {
            jdwppacket_destroy(arraylist_get_value_unsafe(client->sendq, i));
        }
        arraylist_destroy(client->sendq);
        client->sendq = NULL;
    }
    //release all hold object
    HashsetIterator hi;
    hashset_iterate(client->temp_obj_holder, &hi);
    while (hashset_iter_has_more(&hi)) {
        HashsetKey k = hashset_iter_next_key(&hi);
        gc_obj_release(client->jdwpserver->jvm->collector, k);
    }
    hashset_destroy(client->temp_obj_holder);
    client->temp_obj_holder = NULL;

    jdwp_eventset_remove_on_client_close(jdwpserver, client);

    jvm_free(client);
}

void jdwp_client_hold_obj(JdwpClient *client, __refer obj) {
    hashset_put(client->temp_obj_holder, obj);
    gc_obj_hold(client->jvm->collector, obj);
}

void jdwp_client_release_obj(JdwpClient *client, __refer obj) {
    hashset_remove(client->temp_obj_holder, obj, 1);
    gc_obj_release(client->jvm->collector, obj);
}

//==================================================    packet    ==================================================


JdwpPacket *jdwppacket_create() {
    JdwpPacket *packet = jvm_calloc(sizeof(JdwpPacket));
    packet->alloc = 32;
    packet->readPos = 11;
    packet->writePos = 11;
    packet->data = jvm_calloc(packet->alloc);
    return packet;
}

JdwpPacket *jdwppacket_create_data(c8 *data, s32 len) {
    JdwpPacket *packet = jvm_calloc(sizeof(JdwpPacket));
    packet->data = data;
    packet->alloc = len;
    return packet;
}

void jdwppacket_destroy(JdwpPacket *packet) {
    jvm_free(packet->data);
    jvm_free(packet);
}

s8 jdwppacket_read_byte(JdwpPacket *packet) {
    return packet->data[packet->readPos++];
}

s16 jdwppacket_read_short(JdwpPacket *packet) {
    s16 s = (short) (((packet->data[packet->readPos + 1] & 0xFF) << 0) +
                     ((packet->data[packet->readPos + 0] & 0xFF) << 8));
    packet->readPos += 2;
    return s;
}


s32 jdwppacket_read_int(JdwpPacket *packet) {
    s32 i = ((packet->data[packet->readPos + 3] & 0xFF) << 0)
            + ((packet->data[packet->readPos + 2] & 0xFF) << 8)
            + ((packet->data[packet->readPos + 1] & 0xFF) << 16)
            + ((packet->data[packet->readPos + 0] & 0xFF) << 24);
    packet->readPos += 4;
    return i;
}


s64 jdwppacket_read_long(JdwpPacket *packet) {
    s64 l = (((s64) packet->data[packet->readPos + 7] & 0xFFL) << 0)
            + (((s64) packet->data[packet->readPos + 6] & 0xFFL) << 8)
            + (((s64) packet->data[packet->readPos + 5] & 0xFFL) << 16)
            + (((s64) packet->data[packet->readPos + 4] & 0xFFL) << 24)
            + (((s64) packet->data[packet->readPos + 3] & 0xFFL) << 32)
            + (((s64) packet->data[packet->readPos + 2] & 0xFFL) << 40)
            + (((s64) packet->data[packet->readPos + 1] & 0xFFL) << 48)
            + (((s64) packet->data[packet->readPos + 0] & 0xFFL) << 56);
    packet->readPos += 8;
    return l;
}


Utf8String *jdwppacket_read_utf(JdwpPacket *packet) {
    Utf8String *ustr = utf8_create();
    s32 len = jdwppacket_read_int(packet);
    s32 i;
    for (i = 0; i < len; i++) {
        utf8_pushback(ustr, jdwppacket_read_byte(packet));
    }
    return ustr;
}

__refer jdwppacket_read_refer(JdwpPacket *packet) {
    if (sizeof(__refer) > 4) {
        return (__refer) (intptr_t) jdwppacket_read_long(packet);
    } else {
        return (__refer) (intptr_t) jdwppacket_read_int(packet);
    }
}

void jdwppacket_ensureCapacity(JdwpPacket *packet, s32 length) {
    s32 newcount = packet->writePos + length;
    if (newcount >= packet->alloc) {
        newcount = newcount > packet->alloc << 1 ? newcount : packet->alloc << 1;
        c8 *tmp = jvm_calloc(newcount);
        memcpy(tmp, packet->data, packet->alloc);
        jvm_free(packet->data);
        packet->data = tmp;
        packet->alloc = newcount;
    }
}

void jdwppacket_write_byte(JdwpPacket *packet, s8 val) {
    jdwppacket_ensureCapacity(packet, 1);
    packet->data[packet->writePos] = val;
    packet->writePos++;
}

void jdwppacket_write_short(JdwpPacket *packet, s16 val) {
    jdwppacket_ensureCapacity(packet, 2);
    packet->data[packet->writePos + 1] = (u8) (val >> 0);
    packet->data[packet->writePos + 0] = (u8) (val >> 8);
    packet->writePos += 2;
}


void jdwppacket_write_int(JdwpPacket *packet, s32 val) {
    jdwppacket_ensureCapacity(packet, 4);
    packet->data[packet->writePos + 3] = (u8) (val >> 0);
    packet->data[packet->writePos + 2] = (u8) (val >> 8);
    packet->data[packet->writePos + 1] = (u8) (val >> 16);
    packet->data[packet->writePos + 0] = (u8) (val >> 24);
    packet->writePos += 4;
}

void jdwppacket_write_long(JdwpPacket *packet, s64 val) {
    jdwppacket_ensureCapacity(packet, 8);
    packet->data[packet->writePos + 7] = (u8) (val >> 0);
    packet->data[packet->writePos + 6] = (u8) (val >> 8);
    packet->data[packet->writePos + 5] = (u8) (val >> 16);
    packet->data[packet->writePos + 4] = (u8) (val >> 24);
    packet->data[packet->writePos + 3] = (u8) (val >> 32);
    packet->data[packet->writePos + 2] = (u8) (val >> 40);
    packet->data[packet->writePos + 1] = (u8) (val >> 48);
    packet->data[packet->writePos + 0] = (u8) (val >> 56);
    packet->writePos += 8;
}

void jdwppacket_write_buf(JdwpPacket *packet, c8 const *val, s32 len) {
    jdwppacket_ensureCapacity(packet, len);
    memcpy(packet->data + packet->writePos, val, len);
    packet->writePos += len;
}

void jdwppacket_write_utf(JdwpPacket *packet, Utf8String *val) {
    jdwppacket_write_int(packet, val->length);
    jdwppacket_write_buf(packet, utf8_cstr(val), val->length);
}

void jdwppacket_write_refer(JdwpPacket *packet, __refer val) {
    if (sizeof(__refer) > 4) {
        jdwppacket_write_long(packet, (s64) (intptr_t) val);
    } else {
        jdwppacket_write_int(packet, (s32) (intptr_t) val);
    }
}

s32 jdwppacket_getbypos(JdwpPacket *packet, s32 pos, s32 n) {
    s32 len = 0;
    s32 last = pos + n;
    s32 i;
    for (i = pos; i < last; i++) {
        len <<= 8;
        len += (u8) packet->data[i];
    }
    return len;
}

s16 jdwppacket_get_cmd_err(JdwpPacket *packet) {
    return (s16) jdwppacket_getbypos(packet, 9, 2);
}

u8 jdwppacket_get_flag(JdwpPacket *packet) {
    return (u8) jdwppacket_getbypos(packet, 8, 1);
}

s32 jdwppacket_get_id(JdwpPacket *packet) {
    return jdwppacket_getbypos(packet, 4, 4);
}

s32 jdwppacket_get_length(JdwpPacket *packet) {
    return jdwppacket_getbypos(packet, 0, 4);
}

void jdwppacket_set_flag(JdwpPacket *packet, u8 flag) {
    s32 pos = 8;
    packet->data[pos++] = flag;
}

void jdwppacket_set_id(JdwpPacket *packet, s32 id) {
    s32 pos = 4;
    packet->data[pos++] = (u8) (id >> 24);
    packet->data[pos++] = (u8) (id >> 16);
    packet->data[pos++] = (u8) (id >> 8);
    packet->data[pos++] = (u8) (id);
}

void jdwppacket_set_length(JdwpPacket *packet, s32 len) {
    s32 pos = 0;
    packet->data[pos++] = (u8) (len >> 24);
    packet->data[pos++] = (u8) (len >> 16);
    packet->data[pos++] = (u8) (len >> 8);
    packet->data[pos++] = (u8) (len);
}

void jdwppacket_set_cmd(JdwpPacket *packet, u16 cmd) {
    s32 pos = 9;
    packet->data[pos++] = (c8) (cmd >> 8);
    packet->data[pos++] = (c8) (cmd >> 0);
}

void jdwppacket_set_err(JdwpPacket *packet, u16 err) {
    s32 pos = 9;
    packet->data[pos++] = (c8) (err >> 8);
    packet->data[pos++] = (c8) (err >> 0);
}

//Blocking full read/write with an overall deadline. Used for the handshake
//AND for regular reply/event delivery: a debugger that stops reading must
//not wedge the dispatcher forever (VM exit stays possible via the exit
//flag check below). mbedtls contract: negative = error, WANT_* = nothing
//right now, 0 from recv = orderly shutdown by the peer.
#define JDWP_IO_TIMEOUT_MS 10000

s32 jdwp_read_fully(JdwpClient *client, c8 *buf, s32 need) {
    s32 got = 0, len = 0;
    s64 deadline = current_timestamp() + JDWP_IO_TIMEOUT_MS;
    while (got < need) {
        len = mbedtls_net_recv(&client->sockfd, (u8 *) buf + got, need - got);
        if (len == MBEDTLS_ERR_SSL_WANT_READ) {
            if (current_timestamp() > deadline
                || (client->jdwpserver && client->jdwpserver->exit)) {
                jvm_printf("[JDWP]read timeout/exit\n");
                client->closed = 1;
                return -1;
            }
            threadSleep(1); //no busy burn while the debugger is slow
            continue;
        }
        if (len == 0) {
            //peer closed the connection (half-sent handshake included)
            client->closed = 1;
            return -1;
        }
        if (len < 0) {
            //mbedtls returns many negative codes (CONN_RESET, PEER_CLOSED...),
            //not just -1: anything negative ends the read
            jvm_printf("[JDWP]read error %x\n", len);
            client->closed = 1;
            return -1;
        }
        got += len;
    }
    return got;
}

s32 jdwp_write_fully(JdwpClient *client, c8 *buf, s32 need) {
    s32 sent = 0, len = 0;
    s64 deadline = current_timestamp() + JDWP_IO_TIMEOUT_MS;
    while (sent < need) {
        len = mbedtls_net_send(&client->sockfd, (const u8 *) buf + sent, need - sent);
        if (len == MBEDTLS_ERR_SSL_WANT_WRITE) {
            if (current_timestamp() > deadline
                || (client->jdwpserver && client->jdwpserver->exit)) {
                //a client that stopped reading (full socket buffer) must not
                //wedge the dispatcher: bail so shutdown can proceed
                client->closed = 1;
                return -1;
            }
            threadSleep(1);
            continue;
        }
        if (len < 0) {
            //the old loop only broke on len == -1 exactly; every OTHER
            //mbedtls negative error (e.g. CONN_RESET when the debugger
            //dropped the socket) spun forever - the dispatcher burned a
            //core sending to a dead client and never cleared its mode bit
            client->closed = 1;
            return -1;
        }
        sent += len;
    }
    return sent;
}

JdwpPacket *jdwp_readpacket(JdwpClient *client) {
    if (!client->conn_first) {
        if (!client->rcvp) {
            // the previous packet has been fully received
            client->rcvp = jdwppacket_create();
            client->rcvp->_req_len = 4;
            client->rcvp->_rcv_len = 0;
            client->rcvp->_4len = 1; // indicates that the first part received is the length information
        }
        // The previous packet was received partially. There are two scenarios: first, receive 4 bytes, then receive the remaining part.
        if (client->rcvp) {
            if (client->rcvp->_4len) {
                s32 len = mbedtls_net_recv(&client->sockfd, (u8 *) client->rcvp->data + client->rcvp->_rcv_len,
                                           client->rcvp->_req_len - client->rcvp->_rcv_len);
                if (len == MBEDTLS_ERR_SSL_WANT_READ)len = 0; //poll: no data yet
                else if (len == 0)client->closed = 1; //peer gone, not "no data"
                else if (len < 0)client->closed = 1;
                client->rcvp->_rcv_len += len;
                if (client->rcvp->_rcv_len == client->rcvp->_req_len) {
                    client->rcvp->_4len = 0;
                    client->rcvp->_req_len = jdwppacket_get_length(client->rcvp) - 4; // on the next entry, directly receive the packet body
                    client->rcvp->_rcv_len = 0;
                    jdwppacket_ensureCapacity(client->rcvp, client->rcvp->_req_len);
                }
            } else {
                s32 len = mbedtls_net_recv(&client->sockfd, (u8 *) client->rcvp->data + 4 + client->rcvp->_rcv_len,
                                           client->rcvp->_req_len - client->rcvp->_rcv_len);
                if (len == MBEDTLS_ERR_SSL_WANT_READ)len = 0; //poll: no data yet
                else if (len == 0)client->closed = 1; //peer gone, not "no data"
                else if (len < 0)client->closed = 1;
                client->rcvp->_rcv_len += len;
                if (client->rcvp->_rcv_len == client->rcvp->_req_len) {
                    JdwpPacket *p = client->rcvp;
                    client->rcvp = NULL;
                    return p;
                }
            }
        }
    } else {
        // first connection
        c8 buf[14];
        s32 len = jdwp_read_fully(client, (c8 *) &buf, 14);
        if (len == -1) {
            client->closed = 1;
            return NULL; //do not compare an unfilled buffer
        }
        len = (s32) strlen(JDWP_HANDSHAKE);
        s32 i;
        for (i = 0; i < len; i++) {
            if (JDWP_HANDSHAKE[i] != buf[i]) {
                client->closed = 1;
                return NULL;
            }
        }
        len = jdwp_write_fully(client, (c8 *) JDWP_HANDSHAKE, 14);
        if (len < 0) {
            client->closed = 1;
            return NULL;
        }
        client->conn_first = 0;
    }
    return NULL;
}


s32 jdwp_writepacket(JdwpClient *client, JdwpPacket *packet) {
    jdwppacket_set_length(packet, packet->writePos);
    s32 len = jdwp_write_fully(client, packet->data, packet->writePos);
    jdwppacket_destroy(packet);
    if (len < 0) {
        client->closed = 1;
        return 1;
    }
    return 0;
}

//==================================================    toolkit    ==================================================

void suspend_all_thread(MiniJVM *jvm) {
    spin_lock(&jvm->thread_list->spinlock);
    {
        for (s32 i = 0; i < jvm->thread_list->length; i++) {
            Runtime *t = arraylist_get_value_unsafe(jvm->thread_list, i);
            if (t)jthread_suspend(t);
            //jvm_printf("[JDWP]VirtualMachine_Suspend: %lld\n" + (s64) (intptr_t) t);
        }
    }
    spin_unlock(&jvm->thread_list->spinlock);
}

void resume_all_thread(MiniJVM *jvm) {
    spin_lock(&jvm->thread_list->spinlock);
    {
        for (s32 i = 0; i < jvm->thread_list->length; i++) {
            Runtime *t = arraylist_get_value_unsafe(jvm->thread_list, i);
            if (t)jthread_resume(t);
            //jvm_printf("[JDWP]VirtualMachine_Suspend: %lld\n" + (s64) (intptr_t) t);
        }
    }
    spin_unlock(&jvm->thread_list->spinlock);
}

void signatureToName(Utf8String *signature) {
    if (utf8_char_at(signature, 0) == 'L') {
        utf8_substring(signature, 1, signature->length - 1);
    }
}

void nameToSignature(Utf8String *name) {
    if (utf8_char_at(name, 0) != '[') {
        utf8_insert(name, 0, 'L');
        utf8_append_c(name, ";");
    }
}

u8 getClassStatus(JClass *clazz) {
    return JDWP_CLASS_STATUS_INITIALIZED | JDWP_CLASS_STATUS_PREPARED | JDWP_CLASS_STATUS_VERIFIED;
}

CodeAttribute *getCodeAttribute(MethodInfo *method) {
    return method->converted_code;
}


s32 getClassType(JClass *clazz) {
    if (clazz->mb.arr_type_index) {
        return JDWP_TYPETAG_ARRAY;
    } else if (clazz->cff.access_flags & ACC_INTERFACE) {
        return JDWP_TYPETAG_INTERFACE;
    } else {
        return JDWP_TYPETAG_CLASS;
    }
}


c8 getSimpleTag(u8 type) {
    c8 bytes = '0';
    switch (type) {
        case JDWP_TAG_BYTE:
        case JDWP_TAG_BOOLEAN:
            bytes = '1';
            break;
        case JDWP_TAG_SHORT:
        case JDWP_TAG_CHAR:
            bytes = '2';
            break;
        case JDWP_TAG_INT:
        case JDWP_TAG_FLOAT:
            bytes = '4';
            break;
        case JDWP_TAG_LONG:
        case JDWP_TAG_DOUBLE:
            bytes = '8';
            break;
        case JDWP_TAG_ARRAY:
        case JDWP_TAG_OBJECT:
        case JDWP_TAG_STRING:
        case JDWP_TAG_THREAD:
        case JDWP_TAG_THREAD_GROUP:
        case JDWP_TAG_CLASS_LOADER:
        case JDWP_TAG_CLASS_OBJECT:
            bytes = 'R';
            break;
        case JDWP_TAG_VOID:
            bytes = '0';
            break;
    }
    return bytes;
}

c8 getInstanceOfClassTag(Instance *ins) {
    if (!ins)return JDWP_TAG_OBJECT;
    if (ins->mb.type == MEM_TYPE_CLASS)return JDWP_TAG_CLASS_OBJECT;
    JClass *clazz = ins->mb.clazz;
    if (clazz->mb.arr_type_index)return JDWP_TAG_ARRAY;
    if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_THREAD))return JDWP_TAG_THREAD;
    if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_STRING))return JDWP_TAG_STRING;
    return JDWP_TAG_OBJECT;
}

c8 getJdwpTag(Utf8String *ustr) {
    if (utf8_equals_c(ustr, STR_INS_JAVA_LANG_STRING)) {
        return JDWP_TAG_STRING;
    } else if (utf8_equals_c(ustr, STR_INS_JAVA_LANG_CLASS)) {
        return JDWP_TAG_CLASS_OBJECT;
    } else if (utf8_equals_c(ustr, STR_INS_JAVA_LANG_THREAD)) {
        return JDWP_TAG_THREAD;
    }
    return utf8_char_at(ustr, 0);
}

void writeValueType(JdwpPacket *res, ValueType *vt) {
    jdwppacket_write_byte(res, vt->type);
    switch (getSimpleTag(vt->type)) {
        case '1':
            jdwppacket_write_byte(res, (s8) vt->value);
            break;
        case '2':
            jdwppacket_write_short(res, (s16) vt->value);
            break;
        case '4':
            jdwppacket_write_int(res, (s32) vt->value);
            break;
        case '8':
            jdwppacket_write_long(res, vt->value);
            break;
        case 'R':
            jdwppacket_write_refer(res, (__refer) (intptr_t) vt->value);
            break;
    }
}

//SetValues commands (ClassType 3.2 / ObjectReference 9.3) carry UNTAGGED
//values on the wire: the field descriptor pre-sets the type, only the raw
//bytes follow. Reading a tag byte here (like tagged values) misparses them.
void readValueType_untagged(JdwpPacket *req, ValueType *vt) {
    switch (getSimpleTag(vt->type)) {
        case '1':
            vt->value = jdwppacket_read_byte(req);
            break;
        case '2':
            vt->value = jdwppacket_read_short(req);
            break;
        case '4':
            vt->value = jdwppacket_read_int(req);
            break;
        case '8':
            vt->value = jdwppacket_read_long(req);
            break;
        case 'R':
            vt->value = (s64) (intptr_t) jdwppacket_read_refer(req);
            break;
        default:
            vt->value = 0;
            break;
    }
}

void readValueType(JdwpPacket *req, ValueType *vt) {
    vt->type = jdwppacket_read_byte(req);
    switch (getSimpleTag(vt->type)) {
        case '1':
            vt->value = jdwppacket_read_byte(req);
            break;
        case '2':
            vt->value = jdwppacket_read_short(req);
            break;
        case '4':
            vt->value = jdwppacket_read_int(req);
            break;
        case '8':
            vt->value = jdwppacket_read_long(req);
            break;
        case 'R':
            vt->value = (s64) (intptr_t) jdwppacket_read_refer(req);
            break;
    }
}

void writeLocation(JdwpPacket *res, Location *loc) {
    jdwppacket_write_byte(res, loc->typeTag);
    jdwppacket_write_refer(res, loc->classID);
    jdwppacket_write_refer(res, loc->methodID);
    jdwppacket_write_long(res, loc->execIndex);
}

void readLocation(JdwpPacket *req, Location *loc) {
    loc->typeTag = jdwppacket_read_byte(req);
    loc->classID = jdwppacket_read_refer(req);
    loc->methodID = jdwppacket_read_refer(req);
    loc->execIndex = jdwppacket_read_long(req);
}

s64 getPtrValue(u8 type, c8 *ptr) {
    s64 value = 0;
    switch (getSimpleTag(type)) {
        case '1':
            value = getFieldByte(ptr);
            break;
        case '2':
            value = getFieldShort(ptr);
            break;
        case '4':
            value = getFieldInt(ptr);
            break;
        case '8':
            value = getFieldLong(ptr);
            break;
        case 'R':
            value = (s64) (intptr_t) getFieldRefer(ptr);
            break;
    }
    return value;
}

void setPtrValue(u8 type, c8 *ptr, s64 value) {
    switch (getSimpleTag(type)) {
        case '1':
            setFieldByte(ptr, (c8) value);
            break;
        case '2':
            setFieldShort(ptr, (s16) value);
            break;
        case '4':
            setFieldInt(ptr, (s32) value);
            break;
        case '8':
            setFieldLong(ptr, value);
            break;
        case 'R':
            setFieldRefer(ptr, (__refer) (intptr_t) value);
            break;
    }
}


void writeArrayRegion(JdwpPacket *res, Instance *arr, s32 firstIndex, s32 length) {
    c8 arr_type = utf8_char_at(arr->mb.clazz->name, 1);
    jdwppacket_write_byte(res, arr_type);
    jdwppacket_write_int(res, length);
    c8 tag = getSimpleTag(arr_type);
    s32 i;
    // Primitive types do not require a flag, while non-primitive types need to be of ValueType
    for (i = 0; i < length; i++) {
        switch (tag) {
            case '1':
                jdwppacket_write_byte(res, getFieldByte(&jarray_body(arr)[(firstIndex + i)]));
                break;
            case '2':
                jdwppacket_write_short(res, getFieldShort(&jarray_body(arr)[(firstIndex + i) * 2]));
                break;
            case '4':
                jdwppacket_write_int(res, getFieldInt(&jarray_body(arr)[(firstIndex + i) * 4]));
                break;
            case '8':
                jdwppacket_write_long(res, getFieldLong(&jarray_body(arr)[(firstIndex + i) * 8]));
                break;
            case 'R': {
                Instance *elem = getFieldRefer(&jarray_body(arr)[(firstIndex + i) * sizeof(__refer)]);
                if (elem)
                    jdwppacket_write_byte(res, getInstanceOfClassTag(elem));
                else
                    jdwppacket_write_byte(res, 'L');
                jdwppacket_write_refer(res, elem);
                break;
            }
        }
    }
}


void readArrayRegion(JdwpPacket *res, Instance *arr, s32 firstIndex, s32 length) {
    c8 arr_type = utf8_char_at(arr->mb.clazz->name, 1);
    c8 tag = getSimpleTag(arr_type);
    s64 val;
    s32 i;
    // Primitive types do not require a flag, while non-primitive types need to be of ValueType
    for (i = 0; i < length; i++) {
        switch (tag) {
            case '1':
                val = jdwppacket_read_byte(res);
                jarray_set_field(arr, firstIndex + i, val);
                break;
            case '2':
                val = jdwppacket_read_short(res);
                jarray_set_field(arr, firstIndex + i, val);
                break;
            case '4':
                val = jdwppacket_read_int(res);
                jarray_set_field(arr, firstIndex + i, val);
                break;
            case '8':
                val = jdwppacket_read_long(res);
                jarray_set_field(arr, firstIndex + i, val);
                break;
            case 'R': {
                val = (s64) (intptr_t) jdwppacket_read_refer(res);
                jarray_set_field(arr, firstIndex + i, val);
                break;
            }
        }
    }
}

void getClassSignature(JClass *clazz, Utf8String *ustr) {
    if (clazz->mb.arr_type_index) {
        utf8_append(ustr, clazz->name);
    } else {
        utf8_append_c(ustr, "L");
        utf8_append(ustr, clazz->name);
        utf8_append_c(ustr, ";");
    }
}

s32 location_equals(Location *loc1, Location *loc2) {
    if ((!loc1 && loc2) || (!loc2 && loc1))return 0; // one is NULL, the other is not empty
    if (loc1->typeTag == loc2->typeTag
        && loc1->classID == loc2->classID
        && loc1->methodID == loc2->methodID
        && loc1->execIndex == loc2->execIndex
    )
        return 1;
    return 0;
}


void jdwp_print_packet(JdwpPacket *packet) {
    s32 i;
    for (i = 0; i < packet->writePos; i++) {
        if (i % 5 == 0) { jvm_printf("[JDWP]      "); }
        if (i % 10 == 0) { jvm_printf("[JDWP]\n"); }
        jvm_printf("[JDWP] %c[%2x]", packet->data[i] < ' ' || packet->data[i] > '~' ? ' ' : packet->data[i],
                   (u8) packet->data[i]);
    }
    jvm_printf("[JDWP]\n------------------------------\n");
}

void jdwp_check_breakpoint(Runtime *runtime) {
    MethodInfo *method = runtime->method;
    if (runtime->jvm->jdwpserver && runtime->jvm->jdwpserver->invoking)return; //events suppressed during invoke
    //JDWP semantics: resuming from a breakpoint must not re-report the same
    //location. Skip checks at the reported pc until the pc moves on.
    if (runtime->jdwp_bp_skip_pc && runtime->jdwp_bp_skip_pc != runtime->pc) {
        runtime->jdwp_bp_skip_pc = NULL;
    }
    if (!method->breakpoint || !method->converted_code)return;
    if (runtime->jdwp_bp_skip_pc == runtime->pc) {
        return;
    }
    u32 index = (u32) (runtime->pc - runtime->method->converted_code->code);
    if (pairlist_getl(method->breakpoint, index)) {
        runtime->jdwp_bp_skip_pc = runtime->pc;
        event_on_breakpoint(runtime->jvm->jdwpserver, runtime); //
    }
}

void jdwp_check_debug_step(Runtime *runtime) {
    JdwpServer *jdwpserver = runtime->jvm->jdwpserver;
    JdwpStep *step = (runtime->thrd_info->jdwp_step);

    if (jdwpserver && jdwpserver->invoking) return; //events suppressed during invoke
    if (!step->active) return;

    // Only check stepping for the specific target thread
    if (step->target_thread != runtime->thrd_info->jthread) return;

    s32 should_suspend = 0;
    s32 current_depth = getRuntimeDepth(runtime->thrd_info->top_runtime);
    MethodInfo *current_method = runtime->method;
    CodeAttribute *ca = current_method->converted_code;

    switch (step->step_type) {
        case NEXT_TYPE_SINGLE:
            // Single instruction stepping
            if (step->bytecode_count >= 1) {
                should_suspend = 1;
            }
            break;

        case NEXT_TYPE_INTO:
            // Step into: stop when method changes or line changes
            if (current_method != step->start_method) {
                should_suspend = 1;
            } else if (ca) {
                s32 current_line = getLineNumByIndex(ca, (s32) (runtime->pc - ca->code));
                // For native methods, both start_line_no and current_line will be -1
                // We should not trigger step events for native methods unless method changes
                if (current_line != step->start_line_no && current_line > 0 && step->start_line_no > 0) {
                    should_suspend = 1;
                }
            }
            break;

        case NEXT_TYPE_OVER:
            // Step over: stop at same depth with different line, or when returning to upper level
            if (current_depth < step->start_depth) {
                should_suspend = 1; // Returned to upper level
            } else if (current_depth == step->start_depth && ca) {
                s32 current_line = getLineNumByIndex(ca, (s32) (runtime->pc - ca->code));
                // For native methods, both start_line_no and current_line will be -1
                // We should not trigger step events for native methods unless method changes
                if (current_line != step->start_line_no && current_line > 0 && step->start_line_no > 0) {
                    should_suspend = 1; // Same level, different line
                }
            }
            break;

        case NEXT_TYPE_OUT:
            // Step out: stop when call depth decreases
            if (current_depth < step->start_depth) {
                should_suspend = 1;
            }
            break;
    }

    if (should_suspend) {
        // Stop stepping for current thread
        step->active = 0;

        // Send step event first, which will handle suspension based on EventSet policy
        event_on_debug_step(jdwpserver, runtime);
    }
}

Runtime *jdwp_get_runtime(JdwpServer *srv) {
    return srv->runtime_jdwp;
}

void jdwp_check_debug_step_on_return(Runtime *runtime) {
    JdwpStep *step = runtime->thrd_info->jdwp_step;
    if (!step || !step->active)return;
    if (step->target_thread != runtime->thrd_info->jthread)return;
    if (!runtime->method || !runtime->method->converted_code)return; //native frame has no bytecode location
    //called from execute_method_impl when the outermost java frame returns
    //into native (GLFW/JNI callback, main): no further bytecode will run,
    //so a step waiting for a depth decrease must complete here or it is lost
    s32 depth_after_return = getRuntimeDepth(runtime->thrd_info->top_runtime) - 1;
    if (depth_after_return < step->start_depth) {
        step->active = 0;
        event_on_debug_step(runtime->jvm->jdwpserver, runtime);
        //park in the epilogue while the frame is still readable, so the
        //debugger can query frames before the thread vanishes into native
        check_suspend_and_pause(runtime);
    }
}

s32 jdwp_is_invoking(JdwpServer *srv) {
    //true while a debugger invoke is in flight (all java threads frozen,
    //locks skipped, events suppressed)
    if (!srv)return 0;
    return srv->invoking != 0;
}

s32 jdwp_is_ignore_sync(JdwpServer *srv, Runtime *runtime) {
    /* No JDWP server == normal production run: synchronized methods take
     * the FULL lock path. Only an explicit debugger request may bypass
     * method sync. The old `if (!srv) return 1` left Thread.join()'s
     * synchronized wait() on a never-locked monitor. */
    if (!srv || !runtime || !srv->runtime_jdwp)return 0;
    return srv->thread_sync_ignore != 0 &&
           runtime->thrd_info == srv->runtime_jdwp->thrd_info;
}

//==================================================    event suspend helper    ==================================================

/**
 * Apply suspend policy for JDWP events
 * @param jdwpserver JDWP server instance
 * @param suspendPolicy The suspend policy from EventSet
 * @param event_thread The thread that triggered the event (can be NULL)
 */
void jdwp_apply_suspend_policy(JdwpServer *jdwpserver, u8 suspendPolicy, Runtime *event_thread) {
    switch (suspendPolicy) {
        case JDWP_SUSPENDPOLICY_NONE:
            // Do not suspend any threads
            break;
        case JDWP_SUSPENDPOLICY_EVENT_THREAD:
            // Suspend only the thread that triggered the event
            if (event_thread) {
                jthread_suspend(event_thread);
            }
            break;
        case JDWP_SUSPENDPOLICY_ALL:
            // Suspend all threads
            suspend_all_thread(jdwpserver->jvm);
            break;
        default:
            // Default to suspending event thread for unknown policies
            if (event_thread) {
                jthread_suspend(event_thread);
            }
            break;
    }
}

//==================================================    event    ==================================================

//Event composites are BROADCAST: every attached debugger gets its own copy.
//The old code pushed events onto one global queue that the dispatcher
//drained into whichever client it reached first - with two connections
//(IDE + side client) events and replies crossed wires.
static void jdwp_packet_clone_put(JdwpClient *client, JdwpPacket *packet) {
    JdwpPacket *cp = jdwppacket_create();
    //copy the FULL packet incl. its 11-byte header (id/flags/cmd live in
    //data[4..10]): write_buf appends at writePos, so rewind it to 0 first -
    //leaving it at 11 appended the body after a zero header and produced
    //id=0/cmd=0 garbage packets on the wire
    cp->writePos = 0;
    jdwppacket_write_buf(cp, (c8 *) packet->data, packet->writePos);
    arraylist_push_back(client->sendq, cp);
}

void jdwp_packet_put(JdwpServer *jdwpserver, JdwpPacket *packet) {
    netlock_lock(jdwpserver);
    {
        s32 i;
        s32 delivered = 0;
        for (i = 0; i < jdwpserver->clients->length; i++) {
            JdwpClient *client = arraylist_get_value_unsafe(jdwpserver->clients, i);
            if (!client->closed && !client->close_after_flush) {
                jdwp_packet_clone_put(client, packet);
                delivered++;
            }
        }
        if (!delivered) {
            //no client connected: park on the global queue so an (unlikely)
            //later drain does not leak, and drop it right away
            jdwppacket_destroy(packet);
            netlock_unlock(jdwpserver);
            return;
        }
        netlock_notify(jdwpserver);
    }
    netlock_unlock(jdwpserver);
    jdwppacket_destroy(packet);
}

//Request-generated events are UNICAST to the debugger that created the
//request (set->client): broadcasting them would hand foreign requestIds to
//other attached debuggers. Only true lifecycle events (thread start/death,
//vm death) are broadcast via jdwp_packet_put.
void jdwp_event_put_to(JdwpClient *client, JdwpPacket *packet) {
    if (!client || client->closed || client->close_after_flush) {
        jdwppacket_destroy(packet);
        return;
    }
    arraylist_push_back(client->sendq, packet);
    netlock_lock(client->jdwpserver);
    {
        netlock_notify(client->jdwpserver);
    }
    netlock_unlock(client->jdwpserver);
}

//Command replies are UNICAST to the requesting client.
void jdwp_reply_put(JdwpClient *client, JdwpPacket *packet) {
    arraylist_push_back(client->sendq, packet);
    netlock_lock(client->jdwpserver);
    {
        netlock_notify(client->jdwpserver);
    }
    netlock_unlock(client->jdwpserver);
}

JdwpPacket *jdwp_event_packet_get(JdwpServer *jdwpserver) {
    return arraylist_pop_front(jdwpserver->event_packets);
}

void jdwp_eventset_put(JdwpServer *jdwpserver, EventSet *set) {
    mtx_lock(&jdwpserver->event_sets_lock);
    pairlist_put((jdwpserver->event_sets), (__refer) (intptr_t) set->requestId, set);
    mtx_unlock(&jdwpserver->event_sets_lock);
}

void jdwp_eventset_remove(JdwpServer *jdwpserver, s32 id) {
    mtx_lock(&jdwpserver->event_sets_lock);
    pairlist_remove(jdwpserver->event_sets, (__refer) (intptr_t) id);
    mtx_unlock(&jdwpserver->event_sets_lock);
}

EventSet *jdwp_eventset_get(JdwpServer *jdwpserver, s32 id) {
    mtx_lock(&jdwpserver->event_sets_lock);
    EventSet *es = pairlist_get((jdwpserver->event_sets), (__refer) (intptr_t) id);
    mtx_unlock(&jdwpserver->event_sets_lock);
    return es;
}

s32 jdwp_send_packets(JdwpClient *client) {
    s32 count = 0;
    s32 budget = 64 * 1024;
    while (!client->closed && !client->jdwpserver->exit && budget > 0) {
        if (!client->sending) {
            client->sending = arraylist_pop_front(client->sendq);
            if (!client->sending) {
                if (client->close_after_flush) client->closed = 1;
                break;
            }
            jdwppacket_set_length(client->sending, client->sending->writePos);
            client->send_offset = 0;
            client->send_deadline = current_timestamp() + JDWP_IO_TIMEOUT_MS;
        }
        if (current_timestamp() >= client->send_deadline) {
            client->closed = 1;
            break;
        }
        s32 remaining = client->sending->writePos - client->send_offset;
        s32 len = mbedtls_net_send(&client->sockfd,
                                  (const u8 *) client->sending->data + client->send_offset,
                                  remaining < budget ? remaining : budget);
        //Do not wait here: the same dispatcher must service other clients,
        //including the debugger requesting VM.Exit. Keep the exact offset.
        if (len == MBEDTLS_ERR_SSL_WANT_WRITE) {
            if (!client->reported_backpressure && getenv("MINIJVM_JDWP_TRACE_IO")) {
                jvm_printf("[JDWP] WANT_WRITE: yielding dispatcher\n");
            }
            client->reported_backpressure = 1;
            break;
        }
        if (len <= 0) {
            client->closed = 1;
            break;
        }
        client->send_offset += len;
        budget -= len;
        count++;
        if (client->send_offset == client->sending->writePos) {
            jdwppacket_destroy(client->sending);
            client->sending = NULL;
        }
    }
    return count;
}

void event_on_vmstart(JdwpServer *jdwpserver, Instance *jthread, JdwpClient *client) {
    JdwpPacket *req = jdwppacket_create();
    jdwppacket_set_id(req, jdwpserver->jdwp_eventset_commandid++);
    jdwppacket_set_cmd(req, JDWP_CMD_Event_Composite);
    //the suspend policy must reflect the real suspend state of the vm,
    //otherwise the debugger's suspend accounting gets out of sync.
    jdwppacket_write_byte(req, jdwpserver->jvm->jdwp_suspend_on_start ? JDWP_SUSPENDPOLICY_ALL : JDWP_SUSPENDPOLICY_NONE);
    jdwppacket_write_int(req, 1);
    jdwppacket_write_byte(req, JDWP_EVENTKIND_VM_START);
    jdwppacket_write_int(req, 0);
    jdwppacket_write_refer(req, jthread);
    jdwp_event_put_to(client, req);
}

static void send_class_prepare(JdwpServer *jdwpserver, Runtime *runtime, JClass *clazz, EventSet *set, Utf8String *str) {
    JdwpPacket *req = jdwppacket_create();
    jdwppacket_set_id(req, jdwpserver->jdwp_eventset_commandid++);
    jdwppacket_set_cmd(req, JDWP_CMD_Event_Composite);
    jdwppacket_write_byte(req, set->suspendPolicy);
    jdwppacket_write_int(req, 1);
    jdwppacket_write_byte(req, set->eventKind);
    jdwppacket_write_int(req, set->requestId);
    jdwppacket_write_refer(req, runtime ? runtime->thrd_info->jthread : NULL);
    jdwppacket_write_byte(req, getClassType(clazz));
    jdwppacket_write_refer(req, clazz);
    jdwppacket_write_utf(req, str);
    jdwppacket_write_int(req, getClassStatus(clazz));
    jdwp_event_put_to(set->client, req);
    //jvm_printf("[JDWP]class prepare: %s\n", utf8_cstr(str));
}

void event_on_class_prepare(JdwpServer *jdwpserver, Runtime *runtime, JClass *clazz) {
    //post event
    if (jdwpserver) {
        Utf8String *str = utf8_create();
        getClassSignature(clazz, str);


        mtx_lock(&jdwpserver->event_sets_lock);
        Pair *pair = (Pair *) jdwpserver->event_sets->ptr;
        Pair *end = pair + jdwpserver->event_sets->count;
        for (; pair < end; pair++) {
            EventSet *set = (EventSet *) pair->right;
            if (set->eventKind == JDWP_EVENTKIND_CLASS_PREPARE) {
                if (set->modifiers > 0) {
                    s32 i;
                    for (i = 0; i < set->modifiers; i++) {
                        s32 classNameMatch = 0;
                        EventSetMod *mod = &set->mods[i];
                        if (5 == mod->mod_type) {
                            Utf8String *cpattern = set->mods[i].classPattern;
                            s32 starPos = utf8_indexof_c(cpattern, "*");
                            if (starPos < 0) {
                                classNameMatch = utf8_equals(set->mods[i].classPattern, clazz->name);
                            } else {
                                Utf8String *prefix = utf8_create_part(cpattern, 0, starPos);
                                classNameMatch = utf8_indexof(clazz->name, prefix) >= 0;
                            }
                            if (classNameMatch) {
                                //arm the suspend before the packet is queued
                                jdwp_apply_suspend_policy(jdwpserver, set->suspendPolicy, runtime);
                                send_class_prepare(jdwpserver, runtime, clazz, set, str);
                            }
                        }
                    }
                } else {
                    jdwp_apply_suspend_policy(jdwpserver, set->suspendPolicy, runtime);
                    send_class_prepare(jdwpserver, runtime, clazz, set, str);
                }
            }
        }
        utf8_destroy(str);
        mtx_unlock(&jdwpserver->event_sets_lock);
    }
}

void event_on_class_unload(JdwpServer *jdwpserver, JClass *clazz) {
    if (jdwpserver) {
        Utf8String *str = utf8_create();

        getClassSignature(clazz, str);
        mtx_lock(&jdwpserver->event_sets_lock);

        Pair *pair = (Pair *) jdwpserver->event_sets->ptr;
        Pair *end = pair + jdwpserver->event_sets->count;
        for (; pair < end; pair++) {
            EventSet *set = (EventSet *) pair->right;
            if (set->eventKind == JDWP_EVENTKIND_CLASS_UNLOAD) {
                JdwpPacket *req = jdwppacket_create();
                jdwppacket_set_id(req, jdwpserver->jdwp_eventset_commandid++);
                jdwppacket_set_cmd(req, JDWP_CMD_Event_Composite);
                jdwppacket_write_byte(req, set->suspendPolicy);
                jdwppacket_write_int(req, 1);
                jdwppacket_write_byte(req, set->eventKind);
                jdwppacket_write_int(req, set->requestId);
                jdwppacket_write_utf(req, str);
                jdwp_event_put_to(set->client, req);
                //jvm_printf("[JDWP]class unload: %s\n", utf8_cstr(str));
            }
        }
        mtx_unlock(&jdwpserver->event_sets_lock);
        utf8_destroy(str);
    }
}

void event_on_vmdeath(JdwpServer *jdwpserver) {
    JdwpPacket *req = jdwppacket_create();
    jdwppacket_set_id(req, jdwpserver->jdwp_eventset_commandid++);
    jdwppacket_set_cmd(req, JDWP_CMD_Event_Composite);
    jdwppacket_write_byte(req, JDWP_SUSPENDPOLICY_NONE);
    jdwppacket_write_int(req, 1); //event count
    jdwppacket_write_byte(req, JDWP_EVENTKIND_VM_DEATH);
    jdwppacket_write_int(req, 0); //request id
    jdwp_packet_put(jdwpserver, req);
}

void event_on_thread_start(JdwpServer *jdwpserver, Instance *jthread) {
    JdwpPacket *req = jdwppacket_create();
    jdwppacket_set_id(req, jdwpserver->jdwp_eventset_commandid++);
    jdwppacket_set_cmd(req, JDWP_CMD_Event_Composite);
    jdwppacket_write_byte(req, JDWP_SUSPENDPOLICY_NONE);
    jdwppacket_write_int(req, 1); //event count
    jdwppacket_write_byte(req, JDWP_EVENTKIND_THREAD_START);
    jdwppacket_write_int(req, 0); //request id
    jdwppacket_write_refer(req, jthread);
    jdwp_packet_put(jdwpserver, req);
}

void event_on_thread_death(JdwpServer *jdwpserver, Instance *jthread) {
    JdwpPacket *req = jdwppacket_create();
    jdwppacket_set_id(req, jdwpserver->jdwp_eventset_commandid++);
    jdwppacket_set_cmd(req, JDWP_CMD_Event_Composite);
    jdwppacket_write_byte(req, JDWP_SUSPENDPOLICY_NONE);
    jdwppacket_write_int(req, 1); //event count
    jdwppacket_write_byte(req, JDWP_EVENTKIND_THREAD_DEATH);
    jdwppacket_write_int(req, 0); //request id
    jdwppacket_write_refer(req, jthread);
    jdwp_packet_put(jdwpserver, req);
}

void event_on_breakpoint(JdwpServer *jdwpserver, Runtime *breakpoint_runtime) {
    EventInfo ei;
    ei.eventKind = JDWP_EVENTKIND_BREAKPOINT;
    ei.thread = breakpoint_runtime->thrd_info->jthread;
    ei.loc.typeTag = getClassType(breakpoint_runtime->clazz);
    ei.loc.classID = breakpoint_runtime->clazz;
    ei.loc.methodID = breakpoint_runtime->method;
    ei.loc.execIndex = (u64) (intptr_t) breakpoint_runtime->pc - (u64) (intptr_t) breakpoint_runtime->method->converted_code->code;


    mtx_lock(&jdwpserver->event_sets_lock);
    Pair *pair = (Pair *) jdwpserver->event_sets->ptr;
    Pair *end = pair + jdwpserver->event_sets->count;
    for (; pair < end; pair++) {
        EventSet *set = (EventSet *) pair->right;
        s32 i;
        for (i = 0; i < set->modifiers; i++) {
            EventSetMod *mod = &set->mods[i];

            if (7 == mod->mod_type) {
                if (location_equals(&mod->loc, &ei.loc)) {
                    JdwpPacket *req = jdwppacket_create();
                    jdwppacket_set_id(req, jdwpserver->jdwp_eventset_commandid++);
                    jdwppacket_set_cmd(req, JDWP_CMD_Event_Composite);
                    jdwppacket_write_byte(req, set->suspendPolicy);
                    jdwppacket_write_int(req, 1);
                    jdwppacket_write_byte(req, set->eventKind);
                    jdwppacket_write_int(req, set->requestId);
                    jdwppacket_write_refer(req, ei.thread);
                    writeLocation(req, &ei.loc);
                    //arm the suspend BEFORE queueing: the dispatcher may
                    //deliver within microseconds and the debugger would
                    //query a thread whose suspend count is not armed yet
                    jdwp_apply_suspend_policy(jdwpserver, set->suspendPolicy, breakpoint_runtime);
                    jdwp_event_put_to(set->client, req);
                }
            }
        }
    }
    mtx_unlock(&jdwpserver->event_sets_lock);
}

void event_on_debug_step(JdwpServer *jdwpserver, Runtime *step_runtime) {
    EventInfo ei;
    ei.eventKind = JDWP_EVENTKIND_SINGLE_STEP;
    ei.thread = step_runtime->thrd_info->jthread;
    ei.loc.typeTag = getClassType(step_runtime->clazz);
    ei.loc.classID = step_runtime->clazz;
    ei.loc.methodID = step_runtime->method;
    ei.loc.execIndex = (u64) (intptr_t) step_runtime->pc - (u64) (intptr_t) step_runtime->method->converted_code->code;

    mtx_lock(&jdwpserver->event_sets_lock);
    Pair *pair = (Pair *) jdwpserver->event_sets->ptr;
    Pair *end = pair + jdwpserver->event_sets->count;
    for (; pair < end; pair++) {
        EventSet *set = (EventSet *) pair->right;
        s32 i;
        for (i = 0; i < set->modifiers; i++) {
            EventSetMod *mod = &set->mods[i];
            if (10 == mod->mod_type) {
                JdwpPacket *req = jdwppacket_create();
                jdwppacket_set_id(req, jdwpserver->jdwp_eventset_commandid++);
                jdwppacket_set_cmd(req, JDWP_CMD_Event_Composite);
                jdwppacket_write_byte(req, set->suspendPolicy);
                jdwppacket_write_int(req, 1);
                jdwppacket_write_byte(req, set->eventKind);
                jdwppacket_write_int(req, set->requestId);
                jdwppacket_write_refer(req, ei.thread);
                writeLocation(req, &ei.loc);
                //apply the suspend policy BEFORE the packet is queued: the
                //dispatcher may deliver the event within microseconds and the
                //debugger would otherwise query a thread whose suspend count
                //is not armed yet (IncompatibleThreadStateException)
                jdwp_apply_suspend_policy(jdwpserver, set->suspendPolicy, step_runtime);
                jdwp_event_put_to(set->client, req);
            }
        }
    }
    mtx_unlock(&jdwpserver->event_sets_lock);
}

s32 jdwp_set_breakpoint(JdwpServer *jdwpserver, s32 setOrClear, JClass *clazz, MethodInfo *methodInfo, s64 execIndex) {
    if (!is_class_exists(jdwpserver->jvm, clazz)) {
        return JDWP_ERROR_INVALID_CLASS;
    }
    methodInfo = jdwp_find_method_id(clazz, methodInfo);
    if (!methodInfo) {
        return JDWP_ERROR_INVALID_METHODID;
    }
    if (!methodInfo->breakpoint) {
        methodInfo->breakpoint = pairlist_create(4);
    }
    if (methodInfo->converted_code) {
        if (setOrClear) {
            pairlist_putl(methodInfo->breakpoint, (intptr_t) execIndex, 1);
            return JDWP_ERROR_NONE;
        } else {
            pairlist_removel(methodInfo->breakpoint, (intptr_t) execIndex);
            if (methodInfo->breakpoint->count == 0) {
                pairlist_destroy(methodInfo->breakpoint);
                methodInfo->breakpoint = NULL;
            }
            return JDWP_ERROR_NONE;
        }
    }

    return JDWP_ERROR_INVALID_LOCATION;
}


s32 jdwp_set_debug_step(JdwpServer *jdwpserver, s32 setOrClear, Instance *jthread, s32 size, s32 depth) {
    Runtime *r = jthread_get_stackframe_value(jdwpserver->jvm, jthread);
    if (!r) return JDWP_ERROR_INVALID_THREAD;

    Runtime *last = getLastSon(r);
    JdwpStep *step = r->thrd_info->jdwp_step;

    if (setOrClear) {
        step->active = 1;
        step->target_thread = jthread; // Bind to specific thread
        step->start_method = last->method;
        step->start_depth = getRuntimeDepth(r->thrd_info->top_runtime);
        if (step->start_depth > 1) {
            s32 debug = 1;
        }

        // Get current line number
        if (last->method->converted_code) {
            step->start_line_no = getLineNumByIndex(last->method->converted_code,
                                                    (s32) (last->pc - last->method->converted_code->code));
        } else {
            step->start_line_no = -1;
        }

        // Set step type based on depth parameter
        if (depth == JDWP_STEPDEPTH_INTO) {
            step->step_type = NEXT_TYPE_INTO;
        } else if (depth == JDWP_STEPDEPTH_OUT || (last->method && last->method->is_native)) {
            step->step_type = NEXT_TYPE_OUT;
        } else {
            if (size == JDWP_STEPSIZE_LINE) {
                step->step_type = NEXT_TYPE_OVER;
            } else {
                step->step_type = NEXT_TYPE_SINGLE;
            }
        }

        // Reset bytecode counter for single instruction stepping
        step->bytecode_count = 0;
    } else {
        // Clear stepping
        step->active = 0;
        step->target_thread = NULL;
    }

    return JDWP_ERROR_NONE;
}

EventSet *jdwp_eventset_create(JdwpServer *jdwpserver, JdwpClient *client, JdwpPacket *req) {
    EventSet *set = jvm_calloc(sizeof(EventSet));
    set->client = client;
    set->requestId = jdwpserver->jdwp_eventset_requestid++;
    set->eventKind = jdwppacket_read_byte(req);
    set->suspendPolicy = jdwppacket_read_byte(req);
    set->modifiers = jdwppacket_read_int(req);
    set->mods = jvm_calloc(set->modifiers * sizeof(EventSetMod));
    s32 i;
    for (i = 0; i < set->modifiers; i++) {
        EventSetMod *mod = &set->mods[i];
        u8 imod = jdwppacket_read_byte(req);
        mod->mod_type = imod;
        switch (imod) {
            case 1:
                mod->count = jdwppacket_read_int(req);
                break;
            case 2:
                mod->exprID = jdwppacket_read_int(req);
                break;
            case 3:
                mod->thread = jdwppacket_read_refer(req);
                break;
            case 4:
                mod->clazz = jdwppacket_read_refer(req);
                break;
            case 5:
            case 6:
                mod->classPattern = jdwppacket_read_utf(req);
                utf8_replace_c(mod->classPattern, ".", "/");
                break;
            case 7:
                readLocation(req, &mod->loc);
                break;
            case 8:
                mod->exceptionOrNull = jdwppacket_read_refer(req);
                mod->caught = jdwppacket_read_byte(req);
                mod->uncaught = jdwppacket_read_byte(req);
                break;
            case 9:
                mod->declaring = jdwppacket_read_refer(req);
                mod->fieldID = jdwppacket_read_refer(req);
                break;
            case 10:
                mod->thread = jdwppacket_read_refer(req);
                mod->size = jdwppacket_read_int(req);
                mod->depth = jdwppacket_read_int(req);
                break;
            case 11:
                mod->instance = jdwppacket_read_refer(req);
                break;
            case 12:
                mod->sourceNamePattern = jdwppacket_read_utf(req);
                break;
        }
    }
    return set;
}

void jdwp_eventset_destroy(EventSet *set) {
    if (set->mods) {
        s32 i;
        for (i = 0; i < set->modifiers; i++) {
            EventSetMod *mod = &set->mods[i];
            if (mod->sourceNamePattern)utf8_destroy(mod->sourceNamePattern);
            if (mod->classPattern)utf8_destroy(mod->classPattern);
        }
        jvm_free(set->mods);
    }
    jvm_free(set);
}

s16 jdwp_eventset_set(JdwpServer *jdwpserver, EventSet *set) {
    s16 ret = JDWP_ERROR_NONE;
    if (set) {
        switch (set->eventKind) {
            case JDWP_EVENTKIND_VM_DISCONNECTED: {
                break;
            }
            case JDWP_EVENTKIND_VM_START: {
                break;
            }
            case JDWP_EVENTKIND_THREAD_DEATH: {
                break;
            }
            case JDWP_EVENTKIND_SINGLE_STEP: {
                s32 i;
                for (i = 0; i < set->modifiers; i++) {
                    EventSetMod *mod = &set->mods[i];
                    if (10 == mod->mod_type) {
                        jdwp_set_debug_step(jdwpserver, JDWP_EVENTSET_SET, mod->thread, mod->size, mod->depth);
                    }
                }
                break;
            }
            case JDWP_EVENTKIND_BREAKPOINT: {
                s32 i;
                for (i = 0; i < set->modifiers; i++) {
                    EventSetMod *mod = &set->mods[i];
                    if (mod->mod_type == 7) {
                        ret = jdwp_set_breakpoint(jdwpserver, JDWP_EVENTSET_SET, mod->loc.classID, mod->loc.methodID, mod->loc.execIndex);
                    }
                }
                break;
            }
            case JDWP_EVENTKIND_FRAME_POP: {
                break;
            }
            case JDWP_EVENTKIND_EXCEPTION: {
                break;
            }
            case JDWP_EVENTKIND_USER_DEFINED: {
                break;
            }
            case JDWP_EVENTKIND_THREAD_START: {
                break;
            }
            case JDWP_EVENTKIND_CLASS_PREPARE: {
                HashtableIterator hti;
                hashtable_iterate(jdwpserver->jvm->boot_classloader->classes, &hti);
                for (; hashtable_iter_has_more(&hti);) {
                    Utf8String *k = hashtable_iter_next_key(&hti);
                    JClass *cl = hashtable_get(jdwpserver->jvm->boot_classloader->classes, k);

                    //event_on_class_prepare(jdwpserver, NULL, cl);
                }
                break;
            }
            case JDWP_EVENTKIND_CLASS_UNLOAD: {
                break;
            }
            case JDWP_EVENTKIND_CLASS_LOAD: {
                break;
            }
            case JDWP_EVENTKIND_FIELD_ACCESS: {
                break;
            }
            case JDWP_EVENTKIND_FIELD_MODIFICATION: {
                break;
            }
            case JDWP_EVENTKIND_EXCEPTION_CATCH: {
                break;
            }
            case JDWP_EVENTKIND_METHOD_ENTRY: {
                break;
            }
            case JDWP_EVENTKIND_METHOD_EXIT: {
                break;
            }
            case JDWP_EVENTKIND_METHOD_EXIT_WITH_RETURN_VALUE: {
                break;
            }
            case JDWP_EVENTKIND_VM_DEATH: {
                break;
            }
            default: {
                break;
            }
        }
    }
    return ret;
}

s16 jdwp_eventset_clear(JdwpServer *jdwpserver, s32 id) {
    s16 ret = JDWP_ERROR_NONE;

    EventSet *set = jdwp_eventset_get(jdwpserver, id);
    if (set) {
        switch (set->eventKind) {
            case JDWP_EVENTKIND_VM_DISCONNECTED: {
                break;
            }
            case JDWP_EVENTKIND_VM_START: {
                break;
            }
            case JDWP_EVENTKIND_THREAD_DEATH: {
                break;
            }
            case JDWP_EVENTKIND_SINGLE_STEP: {
                s32 i;
                for (i = 0; i < set->modifiers; i++) {
                    EventSetMod *mod = &set->mods[i];
                    if (10 == mod->mod_type) {
                        jdwp_set_debug_step(jdwpserver, JDWP_EVENTSET_CLEAR, mod->thread, mod->size, mod->depth);
                    }
                }
                break;
            }
            case JDWP_EVENTKIND_BREAKPOINT: {
                s32 i;
                for (i = 0; i < set->modifiers; i++) {
                    EventSetMod *mod = &set->mods[i];
                    if (7 == mod->mod_type) {
                        //maybe class has unloaded
                        ret = jdwp_set_breakpoint(jdwpserver, JDWP_EVENTSET_CLEAR, mod->loc.classID, mod->loc.methodID, mod->loc.execIndex);
                    }
                }
                break;
            }
            case JDWP_EVENTKIND_FRAME_POP: {
                break;
            }
            case JDWP_EVENTKIND_EXCEPTION: {
                break;
            }
            case JDWP_EVENTKIND_USER_DEFINED: {
                break;
            }
            case JDWP_EVENTKIND_THREAD_START: {
                break;
            }
            case JDWP_EVENTKIND_CLASS_PREPARE: {
                break;
            }
            case JDWP_EVENTKIND_CLASS_UNLOAD: {
                break;
            }
            case JDWP_EVENTKIND_CLASS_LOAD: {
                break;
            }
            case JDWP_EVENTKIND_FIELD_ACCESS: {
                break;
            }
            case JDWP_EVENTKIND_FIELD_MODIFICATION: {
                break;
            }
            case JDWP_EVENTKIND_EXCEPTION_CATCH: {
                break;
            }
            case JDWP_EVENTKIND_METHOD_ENTRY: {
                break;
            }
            case JDWP_EVENTKIND_METHOD_EXIT: {
                break;
            }
            case JDWP_EVENTKIND_METHOD_EXIT_WITH_RETURN_VALUE: {
                break;
            }
            case JDWP_EVENTKIND_VM_DEATH: {
                break;
            }
            default: {
                break;
            }
        }
    }
    jdwp_eventset_remove(jdwpserver, id);
    jdwp_eventset_destroy(set);
    return ret;
}

void jdwp_eventset_remove_on_client_close(JdwpServer *jdwpserver, JdwpClient *client) {
    mtx_lock(&jdwpserver->event_sets_lock);
    s32 i;
    for (i = 0; i < jdwpserver->event_sets->count; i++) {
        Pair pair = pairlist_get_pair(jdwpserver->event_sets, i);
        EventSet *set = (EventSet *) pair.right;
        if (set->client == client) {
            jdwp_eventset_clear(jdwpserver, set->requestId); //here is removed the event in jdwpserver->event_sets
            i--;
        }
    }
    mtx_unlock(&jdwpserver->event_sets_lock);
}

s32 is_class_exists(MiniJVM *jvm, JClass *clazz) {
    s32 exist = 0;
    spin_lock(&jvm->lock_cloader);
    {
        s32 i, count;
        count = classes_loaded_count_unsafe(jvm);
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            HashtableIterator hti;
            hashtable_iterate(pcl->classes, &hti);
            for (; hashtable_iter_has_more(&hti);) {
                JClass *cl = hashtable_iter_next_value(&hti);
                if (cl == clazz) {
                    exist = 1;
                    break;
                }
            }
            if (exist)break;
        }
    }
    spin_unlock(&jvm->lock_cloader);
    return exist;
}

static FieldInfo *jdwp_find_field_id(JClass *clazz, FieldInfo *field_id) {
    while (clazz) {
        s32 i;
        for (i = 0; i < clazz->fieldPool.field_used; i++) {
            if (&clazz->fieldPool.field[i] == field_id) return &clazz->fieldPool.field[i];
        }
        clazz = getSuperClass(clazz);
    }
    return NULL;
}

static MethodInfo *jdwp_find_method_id(JClass *clazz, MethodInfo *method_id) {
    while (clazz) {
        s32 i;
        for (i = 0; i < clazz->methodPool.method_used; i++) {
            if (&clazz->methodPool.method[i] == method_id) return &clazz->methodPool.method[i];
        }
        clazz = getSuperClass(clazz);
    }
    return NULL;
}

typedef struct {
    Runtime *runtime;
    s32 suspend_count; //count the debugger had when the invoke started
    u8 we_suspended;   //we moved it 0 -> 1 for the single-executor window
} JdwpInvokeSnapshot;

//Invoke execution model: the invoked method runs on the jdwp dispatcher and
//ALL java threads stay frozen (zero frame drift, single executor). Under
//that invariant mutual exclusion is trivially satisfied, so monitorenter /
//monitorexit are skipped (jdwp_is_ignore_sync) instead of taking real locks -
//which would deadlock whenever a frozen thread owns the monitor.
static void jdwp_invoke_end(JdwpServer *jdwpserver, ArrayList *snapshot);

static s16 jdwp_invoke_begin(JdwpServer *jdwpserver, ArrayList **snapshot_out) {
    MiniJVM *jvm = jdwpserver->jvm;
    ArrayList *snapshot = arraylist_create(8);
    //Publish this before taking the snapshot. A thread created concurrently
    //will then either already be in the snapshot or observe invoking=1 and
    //start suspended; there is no untracked runnable window between the two.
    jdwpserver->invoking = 1;
    spin_lock(&jvm->thread_list->spinlock);
    {
        s32 i;
        for (i = 0; i < jvm->thread_list->length; i++) {
            Runtime *t = arraylist_get_value_unsafe(jvm->thread_list, i);
            JdwpInvokeSnapshot *snap = jvm_calloc(sizeof(JdwpInvokeSnapshot));
            snap->runtime = t;
            snap->suspend_count = t->thrd_info->suspend_count;
            arraylist_push_back(snapshot, snap);
        }
    }
    spin_unlock(&jvm->thread_list->spinlock);

    s32 i;
    for (i = 0; i < snapshot->length; i++) {
        JdwpInvokeSnapshot *snap = arraylist_get_value_unsafe(snapshot, i);
        if (snap->suspend_count == 0) {
            //enforce the single-executor invariant: a running thread parks at
            //its next interpreter check point; one deep in native executes no
            //java code and parks as soon as it returns
            jthread_suspend(snap->runtime);
            snap->suspend_count = 1;
            snap->we_suspended = 1;
        }
    }
    *snapshot_out = snapshot;

    //Do not rely on suspend_count alone: an event arms that count before the
    //event thread reaches its park point. Starting the lock-free invoke in
    //that interval would leave two Java executors. Native blocking regions
    //are safe because jthread_block_exit parks before returning to Java.
    for (i = 0; i < snapshot->length; i++) {
        JdwpInvokeSnapshot *snap = arraylist_get_value_unsafe(snapshot, i);
        JavaThreadInfo *ti = snap->runtime->thrd_info;
        s32 spin;
        for (spin = 0; spin < 500 &&
             !ti->is_suspend && !ti->is_blocking &&
             ti->thread_status != THREAD_STATUS_ZOMBIE; spin++) {
            threadSleep(1);
        }
        if (!ti->is_suspend && !ti->is_blocking &&
            ti->thread_status != THREAD_STATUS_ZOMBIE) {
            jdwp_invoke_end(jdwpserver, snapshot);
            *snapshot_out = NULL;
            return JDWP_ERROR_INTERNAL;
        }
    }
    return JDWP_ERROR_NONE;
}

//Monitor queries must not race a running thread's held-lock list or the
//waiting/entering markers. A debugger suspension alone is NOT enough: the
//suspend count is armed before the thread reaches its park point (see the
//event ordering note above). is_blocking is not a monitor-state lock:
//Object.wait completion still mutates native state before clearing it.
//The monitor query handlers therefore also take the corresponding data locks.
static s16 jdwp_wait_thread_parked(Runtime *target, s32 timeout_ms) {
    if (!target)return JDWP_ERROR_INVALID_THREAD;
    JavaThreadInfo *ti = target->thrd_info;
    if (ti->thread_status == THREAD_STATUS_ZOMBIE)return JDWP_ERROR_INVALID_THREAD;
    //is_suspend: parked in check_suspend_and_pause, executing nothing.
    //is_blocking permits native wait completion to mutate monitor state.
    //Callers must snapshot held_locks/markers under their writer locks;
    //GC pause protects lifetime, NOT concurrent native updates.
    if (ti->is_suspend || ti->is_blocking)return JDWP_ERROR_NONE;
    s32 spin;
    for (spin = 0; spin < timeout_ms; spin++) {
        threadSleep(1);
        if (ti->is_suspend || ti->is_blocking)return JDWP_ERROR_NONE;
        if (ti->thread_status == THREAD_STATUS_ZOMBIE)return JDWP_ERROR_INVALID_THREAD;
    }
    return JDWP_ERROR_THREAD_NOT_SUSPENDED;
}

static s32 jdwp_invoke_in_snapshot(ArrayList *snapshot, Runtime *t) {
    s32 i;
    for (i = 0; i < snapshot->length; i++) {
        if (((JdwpInvokeSnapshot *) arraylist_get_value_unsafe(snapshot, i))->runtime == t)return 1;
    }
    return 0;
}

static void jdwp_invoke_end(JdwpServer *jdwpserver, ArrayList *snapshot) {
    MiniJVM *jvm = jdwpserver->jvm;
    jdwpserver->invoking = 0;
    if (!snapshot)return;
    s32 i;
    for (i = 0; i < snapshot->length; i++) {
        JdwpInvokeSnapshot *snap = arraylist_get_value_unsafe(snapshot, i);
        //the thread may have died during the invoke
        if (snap->we_suspended &&
            find_jthread_from_threadlist(jvm, snap->runtime->thrd_info->jthread) == snap->runtime) {
            jthread_resume(snap->runtime);
        }
    }
    //threads born during the invoke start parked (jthread_start checks the
    //invoking flag): release exactly those - i.e. suspended threads that were
    //not present when the invoke began
    spin_lock(&jvm->thread_list->spinlock);
    {
        s32 k;
        for (k = 0; k < jvm->thread_list->length; k++) {
            Runtime *t = arraylist_get_value_unsafe(jvm->thread_list, k);
            if (t->thrd_info->suspend_count > 0 && !jdwp_invoke_in_snapshot(snapshot, t)) {
                spin_unlock(&jvm->thread_list->spinlock);
                jthread_resume(t);
                spin_lock(&jvm->thread_list->spinlock);
            }
        }
    }
    spin_unlock(&jvm->thread_list->spinlock);
    for (i = 0; i < snapshot->length; i++) {
        jvm_free(arraylist_get_value_unsafe(snapshot, i));
    }
    arraylist_destroy(snapshot);
}

void invoke_method(s32 call_mode, JdwpPacket *req, JdwpPacket *res, JdwpClient *client) {
    JdwpServer *jdwpserver = client->jdwpserver;
    GcCollector *collector = client->jvm->collector;
    gc_pause(collector);
    Runtime *runtime = jdwp_get_runtime(jdwpserver);
    JClass *clazz;
    Instance *thread;
    Instance *object = NULL;
    if (call_mode == CALL_MODE_STATIC) {
        clazz = jdwppacket_read_refer(req);
        thread = jdwppacket_read_refer(req);
    } else {
        object = jdwppacket_read_refer(req);
        thread = jdwppacket_read_refer(req);
        clazz = jdwppacket_read_refer(req);
    }
    MethodInfo *method_id = jdwppacket_read_refer(req);
    s32 arguments = jdwppacket_read_int(req);
    s16 validation_error = JDWP_ERROR_NONE;
    Runtime *target_runtime = find_jthread_from_threadlist(client->jvm, thread);
    MethodInfo *methodInfo = NULL;
    if (!is_class_exists(client->jvm, clazz)) {
        validation_error = JDWP_ERROR_INVALID_CLASS;
    } else if (!target_runtime) {
        validation_error = JDWP_ERROR_INVALID_THREAD;
    } else if (target_runtime->thrd_info->suspend_count == 0) {
        validation_error = JDWP_ERROR_THREAD_NOT_SUSPENDED;
    } else if (!(methodInfo = jdwp_find_method_id(clazz, method_id))) {
        validation_error = JDWP_ERROR_INVALID_METHODID;
    } else if (arguments < 0) {
        validation_error = JDWP_ERROR_INVALID_LENGTH;
    } else if (call_mode == CALL_MODE_STATIC && !(methodInfo->access_flags & ACC_STATIC)) {
        validation_error = JDWP_ERROR_INVALID_METHODID;
    } else if (call_mode == CALL_MODE_INSTANCE &&
               ((methodInfo->access_flags & ACC_STATIC) || !object || !instance_of(object, clazz))) {
        validation_error = JDWP_ERROR_INVALID_OBJECT;
    }
    if (validation_error != JDWP_ERROR_NONE) {
        jdwppacket_set_err(res, validation_error);
        jdwp_reply_put(client, res);
        gc_resume(collector);
        return;
    }
    ArrayList *invoke_snapshot = NULL;
    s16 invoke_error = jdwp_invoke_begin(jdwpserver, &invoke_snapshot);
    if (invoke_error != JDWP_ERROR_NONE) {
        jdwppacket_set_err(res, invoke_error);
        jdwp_reply_put(client, res);
        gc_resume(collector);
        return;
    }

    s32 stacksize = stack_size(runtime->stack);
    if (!(methodInfo->is_static)) {
        push_ref(runtime->stack, object);
    }
    runtime->clazz = clazz;
    s32 i;
    for (i = 0; i < arguments; i++) {
        ValueType vt;
        readValueType(req, &vt);

        switch (getSimpleTag(vt.type)) {
            case '8':
                push_long(runtime->stack, vt.value);
                break;
            case 'R':
                push_ref(runtime->stack, (__refer) (intptr_t) vt.value);
                break;
            default:
                push_int(runtime->stack, (s32) vt.value);
        }
    }
    (void) jdwppacket_read_int(req); //invoke options; execution model is documented above

    //headroom for the invoked method: without a fresh collection its first
    //slow-path allocation is refused with OOM (collector stays paused for
    //the whole invoke - see the execution model note above)
    gc_make_room(jdwpserver->jvm);

    jdwpserver->thread_sync_ignore = 1;
    s32 ret = execute_method_impl(methodInfo, runtime);
    jdwpserver->thread_sync_ignore = 0;
    jdwp_invoke_end(jdwpserver, invoke_snapshot);

    if (ret == RUNTIME_STATUS_ERROR) {
        runtime->stack->sp = runtime->stack->store + stacksize;
        jdwppacket_set_err(res, JDWP_ERROR_INTERNAL);
        jdwp_reply_put(client, res);
        gc_resume(collector);
        return;
    }

    jdwppacket_set_err(res, JDWP_ERROR_NONE);

    ValueType returnValue, exceptionValue;
    memset(&returnValue, 0, sizeof(ValueType));
    memset(&exceptionValue, 0, sizeof(ValueType));

    if (ret == RUNTIME_STATUS_EXCEPTION) {
        // Method threw an exception
        print_exception(runtime);

        // Get the exception object from stack top
        Instance *exception = (Instance *) pop_ref(runtime->stack);

        // Set return value to default (void/null)
        returnValue.type = 'V'; // void type
        returnValue.value = 0;

        // Set exception object
        if (exception) {
            exceptionValue.type = getInstanceOfClassTag(exception);
            exceptionValue.value = (s64) (intptr_t) exception;
            jdwp_client_hold_obj(client, exception);
        } else {
            exceptionValue.type = 'L';
            exceptionValue.value = 0;
        }
    } else {
        // Method completed normally
        if (stack_size(runtime->stack) > stacksize) {
            Utf8String *us = utf8_create_copy(methodInfo->descriptor);
            utf8_substring(us, utf8_indexof_c(us, ")") + 1, us->length);
            returnValue.type = getJdwpTag(us);
            switch (getSimpleTag(returnValue.type)) {
                case '8':
                    returnValue.value = pop_long(runtime->stack);
                    break;
                case 'R': {
                    __refer r = pop_ref(runtime->stack);
                    returnValue.type = getInstanceOfClassTag(r); //recorrect type, may be Arraylist<String>
                    returnValue.value = (s64) (intptr_t) r;
                    jdwp_client_hold_obj(client, r);
                    break;
                }
                default:
                    returnValue.value = pop_int(runtime->stack);
            }
            utf8_destroy(us);
        } else {
            // Void method
            returnValue.type = 'V';
            returnValue.value = 0;
        }

        // No exception
        exceptionValue.type = 'L';
        exceptionValue.value = 0;
    }

    // Write return value and exception according to JDWP protocol
    writeValueType(res, &returnValue);
    writeValueType(res, &exceptionValue);
    jdwp_reply_put(client, res);

    gc_move_objs_thread_2_gc(runtime);
    gc_resume(collector); //
}

//==================================================    process packet    ==================================================

s32 jdwp_client_process(JdwpServer *jdwpserver, JdwpClient *client) {
    JdwpPacket *req = NULL;
    MiniJVM *jvm = jdwpserver->jvm;
    s32 packetCount = 0;
    //Bound input work as well as output work. A flooding client must not
    //starve other connections or grow an unbounded reply queue under backpressure.
    while (!client->closed && !client->close_after_flush && packetCount < 16 && !client->sending
           && (req = jdwp_readpacket(client)) != NULL) {
        u16 cmd = jdwppacket_get_cmd_err(req);
        JdwpPacket *res = jdwppacket_create();
        jdwppacket_set_flag(res, JDWP_PACKET_RESPONSE);
        s32 reqid = jdwppacket_get_id(req);
        //        jvm_printf("jdwp_client_process: %x  id=%d\n", cmd, reqid);
        jdwppacket_set_id(res, reqid);
        switch (cmd) {
            //set 1
            case JDWP_CMD_VirtualMachine_Version: {
                //1.1
                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                Utf8String *ustr = utf8_create();
                utf8_append_c(ustr, "jdwp 1.2");
                jdwppacket_write_utf(res, ustr);
                jdwppacket_write_int(res, 1);
                jdwppacket_write_int(res, 3);
                utf8_clear(ustr);
                utf8_append_c(ustr, "1.3.0");
                jdwppacket_write_utf(res, ustr);
                utf8_clear(ustr);
                utf8_append_c(ustr, "Mini jvm");
                jdwppacket_write_utf(res, ustr);
                jdwp_reply_put(client, res);
                utf8_destroy(ustr);
                while (!jvm->thread_list->length) {
                    threadSleep(20);
                }
                Runtime *mainthread = (Runtime *) arraylist_get_value(jdwpserver->jvm->thread_list, 0);
                //JDI attach waits for the VM_START event unconditionally;
                //it must be sent even when suspend=n (policy NONE above).
                event_on_vmstart(jdwpserver, mainthread->thrd_info->jthread, client);
                break;
            }
            case JDWP_CMD_VirtualMachine_ClassesBySignature: {
                //1.2
                Utf8String *signature = jdwppacket_read_utf(req);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                signatureToName(signature);
                MiniJVM *jvm = jdwpserver->jvm;
                spin_lock(&jvm->lock_cloader);
                {
                    s32 i, found = 0;
                    ByteBuf *bb = bytebuf_create(64);
                    for (i = 0; i < jvm->classloaders->length; i++) {
                        PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
                        JClass *cl = hashtable_get(pcl->classes, signature);
                        if (cl != NULL) {
                            bytebuf_write_byte(bb, getClassType(cl));
                            if (sizeof(__refer) == 4) {
                                bytebuf_write_int(bb, (s32) (intptr_t) cl);
                            } else {
                                bytebuf_write_long(bb, (s64) (intptr_t) cl);
                            }
                            bytebuf_write_int(bb, getClassStatus(cl));
                            //                                jdwppacket_write_byte(res, getClassType(cl));
                            //                                jdwppacket_write_refer(res, cl);
                            //                                jdwppacket_write_int(res, getClassStatus(cl));
                            found++;
                        }
                    }
                    jdwppacket_write_int(res, found);
                    if (found) {
                        jdwppacket_write_buf(res, bb->buf, bb->wp);
                    }
                    bytebuf_destroy(bb);
                }
                spin_unlock(&jvm->lock_cloader);
                //jvm_printf("[JDWP]VirtualMachine_ClassesBySignature:%s ,%lld\n", utf8_cstr(signature), (s64) (intptr_t) cl);
                jdwp_reply_put(client, res);
                utf8_destroy(signature);
                break;
            }
            case JDWP_CMD_VirtualMachine_AllClasses: {
                //1.3
                PeerClassLoader *boot_classloader = jdwpserver->jvm->boot_classloader;
                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                Utf8String *ustr = utf8_create();

                MiniJVM *jvm = jdwpserver->jvm;
                spin_lock(&jvm->lock_cloader);
                {
                    s32 i, count;
                    count = classes_loaded_count_unsafe(jdwpserver->jvm);
                    jdwppacket_write_int(res, count);
                    for (i = 0; i < jvm->classloaders->length; i++) {
                        PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
                        HashtableIterator hti;
                        hashtable_iterate(pcl->classes, &hti);
                        for (; hashtable_iter_has_more(&hti);) {
                            Utf8String *k = hashtable_iter_next_key(&hti);
                            JClass *cl = hashtable_get(pcl->classes, k);

                            jdwppacket_write_byte(res, getClassType(cl));
                            jdwppacket_write_refer(res, cl);
                            utf8_clear(ustr);
                            utf8_append(ustr, cl->name);
                            nameToSignature(ustr);
                            //jvm_printf("jdwp:%s\n", utf8_cstr(ustr));
                            jdwppacket_write_utf(res, ustr);
                            jdwppacket_write_int(res, getClassStatus(cl));
                        }
                    }
                }
                spin_unlock(&jvm->lock_cloader);

                utf8_destroy(ustr);

                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_AllThreads: {
                //1.4
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_int(res, jdwpserver->jvm->thread_list->length);
                s32 i;
                for (i = 0; i < jdwpserver->jvm->thread_list->length; i++) {
                    Runtime *t = threadlist_get(jdwpserver->jvm, i);
                    if (t) {
                        jdwppacket_write_refer(res, t->thrd_info->jthread);
                        //jvm_printf("[JDWP]VirtualMachine_AllThreads: %llx\n", (s64) (intptr_t) t);
                        Instance *jarr_name = jthread_get_name_value(jdwpserver->jvm, t->thrd_info->jthread);
                        Utf8String *ustr = utf8_create();
                        unicode_2_utf8((u16 *) jarray_body(jarr_name), ustr, jarray_length(jarr_name));
                        //printf("[JDWP]%s\n", utf8_cstr(ustr));
                        utf8_destroy(ustr);
                    }
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_TopLevelThreadGroups: {
                //1.5 collect root groups by walking every live thread's group chain
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                Runtime *jr = jdwp_get_runtime(jdwpserver);
                gc_pause(jvm->collector);
                ArrayList *roots = arraylist_create(2);
                spin_lock(&jvm->thread_list->spinlock);
                {
                    s32 i;
                    for (i = 0; i < jvm->thread_list->length; i++) {
                        Runtime *t = arraylist_get_value_unsafe(jvm->thread_list, i);
                        Instance *grp = t->thrd_info->jthread ? jdwp_threadgroup_of(jvm, jr, t->thrd_info->jthread) : NULL;
                        while (grp) {
                            Instance *parent = jdwp_threadgroup_parent(jvm, jr, grp);
                            if (!parent) {
                                if (arraylist_index_of(roots, arraylist_compare_ptr, grp) < 0) {
                                    arraylist_push_back(roots, grp);
                                }
                                break;
                            }
                            grp = parent;
                        }
                    }
                }
                spin_unlock(&jvm->thread_list->spinlock);
                jdwppacket_write_int(res, roots->length);
                s32 i;
                for (i = 0; i < roots->length; i++) {
                    jdwppacket_write_refer(res, arraylist_get_value_unsafe(roots, i));
                }
                arraylist_destroy(roots);
                gc_move_objs_thread_2_gc(jr);
                gc_resume(jvm->collector);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_Dispose: {
                //1.6

                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                jdwp_reply_put(client, res);
                client->close_after_flush = 1;
                break;
            }
            case JDWP_CMD_VirtualMachine_IDSizes: {
                //1.7

                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                jdwppacket_write_int(res, sizeof(__refer));
                jdwppacket_write_int(res, sizeof(__refer));
                jdwppacket_write_int(res, sizeof(__refer));
                jdwppacket_write_int(res, sizeof(__refer));
                jdwppacket_write_int(res, sizeof(__refer));
                jdwp_reply_put(client, res);


                //this  is the first jdwp client command
                //resume_all_thread();

                break;
            }
            case JDWP_CMD_VirtualMachine_Suspend: {
                //1.8
                suspend_all_thread(jdwpserver->jvm);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_Resume: {
                //1.9
                resume_all_thread(jdwpserver->jvm);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_Exit: {
                //1.10 spec: the debugger must receive the reply BEFORE the
                //VM dies - flush it synchronously, then take the same exit
                //path as System.exit (flag + stop all threads; main() then
                //tears down, jdwp_stop_server sends VM_DEATH). no_pause=1
                //lets even debugger-suspended threads unwind and die.
                s32 exit_code = jdwppacket_read_int(req);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwp_reply_put(client, res);
                jdwp_send_packets(client); //deliver the reply now
                jdwpserver->jvm->collector->exit_flag = 1;
                jdwpserver->jvm->collector->exit_code = exit_code;
                thread_stop_all(jdwpserver->jvm);
                break;
            }
            case JDWP_CMD_VirtualMachine_CreateString: {
                //1.11
                Utf8String *str = jdwppacket_read_utf(req);
                gc_pause(jdwpserver->jvm->collector);
                Runtime *runtime = jdwp_get_runtime(jdwpserver);
                Instance *jstr = jstring_create(str, runtime);
                jdwp_client_hold_obj(client, jstr); // prevent garbage collection, holding is required here
                gc_move_objs_thread_2_gc(runtime);
                gc_resume(jdwpserver->jvm->collector);
                utf8_destroy(str);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_refer(res, jstr);
                //jvm_printf("[JDWP]VirtualMachine_CreateString: %s , rid: %llx\n", utf8_cstr(str), (s64) (intptr_t) jstr);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_Capabilities: {
                //1.12
                //                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                //                s32 i;
                //                for (i = 0; i < 7; i++) {
                //                    jdwppacket_write_byte(res, 0);
                //                }
                //                jdwp_event_packet_put(res);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_byte(res, 0); //canWatchFieldModification
                jdwppacket_write_byte(res, 0); //canWatchFieldAccess
                jdwppacket_write_byte(res, 1); //canGetBytecodes (Method.Bytecodes)
                jdwppacket_write_byte(res, 0); //canGetSyntheticAttribute
                jdwppacket_write_byte(res, 1); //canGetOwnedMonitorInfo (ThreadReference.OwnedMonitors)
                jdwppacket_write_byte(res, 1); //canGetCurrentContendedMonitor (ThreadReference.CurrentContendedMonitor)
                jdwppacket_write_byte(res, 1); //canGetMonitorInfo (ObjectReference.MonitorInfo)
                //jvm_printf("[JDWP]VirtualMachine_Capabilities");
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_ClassPaths: {
                //1.13
                //jdb calls VM.classPath() unconditionally during connect,
                //NOT_IMPLEMENTED here aborts the whole jdb session.
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                Utf8String *empty = utf8_create_c("");
                jdwppacket_write_utf(res, empty); //baseDir
                utf8_destroy(empty);

                //classpath from java.class.path property
                {
                    Utf8String *key = utf8_create_c(STR_VM_JAVA_CLASS_PATH);
                    Utf8String *cp = (Utf8String *) hashtable_get(jvm->sys_prop, key);
                    utf8_destroy(key);
                    Utf8String *part = utf8_create();
                    s32 cnt = 0, i;
                    for (i = 0;; i++) {
                        utf8_clear(part);
                        utf8_split_get_part(cp, PATHSEPARATOR, i, part);
                        if (!part->length)break;
                        cnt++;
                    }
                    jdwppacket_write_int(res, cnt);
                    for (i = 0; i < cnt; i++) {
                        utf8_clear(part);
                        utf8_split_get_part(cp, PATHSEPARATOR, i, part);
                        jdwppacket_write_utf(res, part);
                    }
                    utf8_destroy(part);
                }
                //bootclasspath from boot classloader entries
                {
                    PeerClassLoader *bootcl = jvm->boot_classloader;
                    jdwppacket_write_int(res, bootcl->classpath->length);
                    s32 i;
                    for (i = 0; i < bootcl->classpath->length; i++) {
                        jdwppacket_write_utf(res, (Utf8String *) arraylist_get_value_unsafe(bootcl->classpath, i));
                    }
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_DisposeObjects: {
                //1.14
                s32 requests = jdwppacket_read_int(req);
                //                s32 i;
                //                for (i = 0; i < requests; i++) {
                //                    __refer ins = jdwppacket_read_refer(req);
                //                    memoryblock_destroy(ins);
                //                    s32 count = jdwppacket_read_int(req);
                //                    jdwp_client_release_obj(client, ins);//release obj
                //                    jvm_printf("[JDWP]%x disposed.\n", (s64) (intptr_t) ins);
                //                }
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_HoldEvents: {
                //1.15
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_ReleaseEvents: {
                //1.16
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_CapabilitiesNew: {
                //1.17 spec: the FIRST SEVEN fields repeat the old
                //Capabilities command verbatim (verified against the JDK's
                //generated JDWP$VirtualMachine$CapabilitiesNew), new
                //capabilities only start at field 8. The seven writes below
                //mirror the old handler - keep both in sync on change.
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_byte(res, 0); //canWatchFieldModification
                jdwppacket_write_byte(res, 0); //canWatchFieldAccess
                jdwppacket_write_byte(res, 1); //canGetBytecodes
                jdwppacket_write_byte(res, 0); //canGetSyntheticAttribute
                jdwppacket_write_byte(res, 1); //canGetOwnedMonitorInfo
                jdwppacket_write_byte(res, 1); //canGetCurrentContendedMonitor
                jdwppacket_write_byte(res, 1); //canGetMonitorInfo
                s32 i;
                for (i = 7; i < 32; i++) {
                    jdwppacket_write_byte(res, 0); //redefine/popFrames/... not implemented
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_RedefineClasses: {
                //1.18
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_SetDefaultStratum: {
                //1.19
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_VirtualMachine_AllClassesWithGeneric: {
                //1.20 same layout as AllClasses plus a (generic) signature field
                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                Utf8String *ustr = utf8_create();

                MiniJVM *jvm = jdwpserver->jvm;
                spin_lock(&jvm->lock_cloader);
                {
                    s32 i, count;
                    count = classes_loaded_count_unsafe(jdwpserver->jvm);
                    jdwppacket_write_int(res, count);
                    for (i = 0; i < jvm->classloaders->length; i++) {
                        PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
                        HashtableIterator hti;
                        hashtable_iterate(pcl->classes, &hti);
                        for (; hashtable_iter_has_more(&hti);) {
                            Utf8String *k = hashtable_iter_next_key(&hti);
                            JClass *cl = hashtable_get(pcl->classes, k);

                            jdwppacket_write_byte(res, getClassType(cl));
                            jdwppacket_write_refer(res, cl);
                            utf8_clear(ustr);
                            utf8_append(ustr, cl->name);
                            nameToSignature(ustr);
                            jdwppacket_write_utf(res, ustr);
                            utf8_clear(ustr);
                            jdwppacket_write_utf(res, ustr); //generic signature is unavailable
                            jdwppacket_write_int(res, getClassStatus(cl));
                        }
                    }
                }
                spin_unlock(&jvm->lock_cloader);

                utf8_destroy(ustr);
                jdwp_reply_put(client, res);
                break;
            }
            //set 2

            case JDWP_CMD_ReferenceType_Signature: {
                //2.1
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    Utf8String *str = utf8_create();
                    getClassSignature(ref, str);
                    jdwppacket_write_utf(res, str);
                    utf8_destroy(str);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                //jvm_printf("[JDWP]ReferenceType_Signature:%llx , %s \n", (s64) (intptr_t) ref, utf8_cstr(str));
                break;
            }
            case JDWP_CMD_ReferenceType_ClassLoader: {
                //2.2
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_refer(res, ref->jloader);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_Modifiers: {
                //2.3
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_int(res, ref->cff.access_flags);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_Fields: {
                //2.4
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    s32 len = ref->fieldPool.field_used;
                    jdwppacket_write_int(res, len);
                    s32 i;
                    for (i = 0; i < len; i++) {
                        ////jvm_printf("[JDWP]method[" + i + "]" + ref.methods[i]);
                        jdwppacket_write_refer(res, &ref->fieldPool.field[i]);
                        jdwppacket_write_utf(res, ref->fieldPool.field[i].name);
                        jdwppacket_write_utf(res, ref->fieldPool.field[i].descriptor);
                        jdwppacket_write_int(res, ref->fieldPool.field[i].access_flags);
                    }
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_Methods: {
                //2.5
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    s32 len = ref->methodPool.method_used;
                    jdwppacket_write_int(res, len);
                    s32 i;
                    for (i = 0; i < len; i++) {
                        ////jvm_printf("[JDWP]method[" + i + "]" + ref.methods[i]);
                        jdwppacket_write_refer(res, &ref->methodPool.method[i]);
                        jdwppacket_write_utf(res, ref->methodPool.method[i].name);
                        jdwppacket_write_utf(res, ref->methodPool.method[i].descriptor);
                        jdwppacket_write_int(res, ref->methodPool.method[i].access_flags);
                    }
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_GetValues: {
                //2.6
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    gc_pause(jdwpserver->jvm->collector);
                    Runtime *runtime = jdwp_get_runtime(jdwpserver);
                    s32 fields = jdwppacket_read_int(req);
                    jdwppacket_set_err(res, fields < 0 ? JDWP_ERROR_INVALID_LENGTH : JDWP_ERROR_NONE);
                    if (fields >= 0) jdwppacket_write_int(res, fields);
                    s32 i;
                    for (i = 0; i < fields; i++) {
                        FieldInfo *field_id = jdwppacket_read_refer(req);
                        FieldInfo *fi = jdwp_find_field_id(ref, field_id);
                        if (!fi || !(fi->access_flags & ACC_STATIC)) {
                            jdwppacket_set_err(res, JDWP_ERROR_INVALID_FIELDID);
                            break;
                        }
                        ValueType vt;
                        vt.type = getJdwpTag(fi->descriptor);
                        c8 *ptr = getStaticFieldPtr(fi);
                        vt.value = getPtrValue(vt.type, ptr);
                        writeValueType(res, &vt);
                    }

                    gc_move_objs_thread_2_gc(runtime);
                    gc_resume(jdwpserver->jvm->collector);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_SourceFile: {
                //2.7
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_utf(res, ref->source ? ref->source : ref->name); //Lambda has no source
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_NestedTypes: {
                //2.8
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_Status: {
                //2.9
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_int(res, getClassStatus(ref));
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_Interfaces: {
                //2.10
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    s32 len = ref->interfacePool.clasz_used;
                    jdwppacket_write_int(res, len);
                    s32 i;
                    for (i = 0; i < len; i++) {
                        JClass *cl = classes_get(jdwpserver->jvm, ref->jloader, ref->interfacePool.clasz[i].name);
                        jdwppacket_write_refer(res, cl);
                    }
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_ClassObject: {
                //2.11
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_refer(res, ref);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_SourceDebugExtension: {
                //2.12
                jdwppacket_set_err(res, JDWP_ERROR_ABSENT_INFORMATION);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_SignatureWithGeneric: {
                //2.13
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);

                    Utf8String *str = utf8_create();
                    getClassSignature(ref, str);
                    jdwppacket_write_utf(res, str);
                    utf8_clear(str);
                    jdwppacket_write_utf(res, str);
                    utf8_destroy(str);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                //jvm_printf("[JDWP]JDWP_CMD_ReferenceType_SignatureWithGeneric:%llx , %s \n", (s64) (intptr_t) ref, utf8_cstr(str));
                break;
            }
            case JDWP_CMD_ReferenceType_FieldsWithGeneric: {
                //2.14
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    s32 len = ref->fieldPool.field_used;
                    jdwppacket_write_int(res, len);
                    Utf8String *ustr = utf8_create();
                    s32 i;
                    for (i = 0; i < len; i++) {
                        jdwppacket_write_refer(res, &ref->fieldPool.field[i]);
                        jdwppacket_write_utf(res, ref->fieldPool.field[i].name);
                        jdwppacket_write_utf(res, ref->fieldPool.field[i].descriptor);
                        jdwppacket_write_utf(res, ustr);
                        jdwppacket_write_int(res, ref->fieldPool.field[i].access_flags);
                    }
                    utf8_destroy(ustr);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ReferenceType_MethodsWithGeneric: {
                //2.15
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    s32 len = ref->methodPool.method_used;
                    jdwppacket_write_int(res, len);
                    Utf8String *ustr = utf8_create();
                    s32 i;
                    for (i = 0; i < len; i++) {
                        jdwppacket_write_refer(res, &ref->methodPool.method[i]);
                        jdwppacket_write_utf(res, ref->methodPool.method[i].name);
                        jdwppacket_write_utf(res, ref->methodPool.method[i].descriptor);
                        jdwppacket_write_utf(res, ustr);
                        jdwppacket_write_int(res, ref->methodPool.method[i].access_flags);
                    }
                    utf8_destroy(ustr);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            //set 3

            case JDWP_CMD_ClassType_Superclass: {
                //3.1
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_refer(res, getSuperClass(ref));
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ClassType_SetValues: {
                //3.2
                JClass *ref = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    gc_pause(jdwpserver->jvm->collector);
                    Runtime *runtime = jdwp_get_runtime(jdwpserver);
                    s32 fields = jdwppacket_read_int(req);
                    jdwppacket_set_err(res, fields < 0 ? JDWP_ERROR_INVALID_LENGTH : JDWP_ERROR_NONE);
                    s32 i;
                    for (i = 0; i < fields; i++) {
                        FieldInfo *field_id = jdwppacket_read_refer(req);
                        FieldInfo *fi = jdwp_find_field_id(ref, field_id);
                        if (!fi || !(fi->access_flags & ACC_STATIC)) {
                            jdwppacket_set_err(res, JDWP_ERROR_INVALID_FIELDID);
                            break;
                        }
                        ValueType vt;
                        vt.type = getJdwpTag(fi->descriptor);
                        readValueType_untagged(req, &vt);
                        c8 *ptr = getStaticFieldPtr(fi);
                        setPtrValue(vt.type, ptr, vt.value);
                    }
                    jdwp_reply_put(client, res);

                    gc_move_objs_thread_2_gc(runtime);
                    gc_resume(jdwpserver->jvm->collector);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                    jdwp_reply_put(client, res);
                }
                break;
            }
            case JDWP_CMD_ClassType_InvokeMethod: {
                //3.3
                invoke_method(CALL_MODE_STATIC, req, res, client);
                break;
            }
            case JDWP_CMD_ClassType_NewInstance: {
                //3.4
                JClass *clazz = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, clazz)) {
                    // Check if class is instantiable (not interface, not abstract, not array)
                    if ((clazz->cff.access_flags & ACC_INTERFACE) ||
                        (clazz->cff.access_flags & ACC_ABSTRACT) ||
                        clazz->mb.arr_type_index != 0) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                        jdwp_reply_put(client, res);
                        break;
                    }

                    Instance *thread = jdwppacket_read_refer(req);
                    MethodInfo *constructor_id = jdwppacket_read_refer(req);
                    s32 arguments = jdwppacket_read_int(req);
                    Runtime *target_runtime = find_jthread_from_threadlist(jdwpserver->jvm, thread);
                    MethodInfo *constructor = jdwp_find_method_id(clazz, constructor_id);

                    // Validate constructor
                    if (!constructor || constructor->_this_class != clazz ||
                        utf8_equals_c(constructor->name, "<init>") != 1 || arguments < 0) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_METHODID);
                        jdwp_reply_put(client, res);
                        break;
                    }
                    if (!target_runtime) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                        jdwp_reply_put(client, res);
                        break;
                    }
                    if (target_runtime->thrd_info->suspend_count == 0) {
                        jdwppacket_set_err(res, JDWP_ERROR_THREAD_NOT_SUSPENDED);
                        jdwp_reply_put(client, res);
                        break;
                    }

                    gc_pause(jdwpserver->jvm->collector);
                    Runtime *runtime = jdwp_get_runtime(jdwpserver);

                    // Create new instance
                    Instance *newInstance = instance_create(runtime, clazz);
                    if (newInstance) {
                        jdwp_client_hold_obj(client, newInstance);

                        // Create parameter stack for constructor
                        RuntimeStack *paraStack = NULL;
                        if (arguments > 0) {
                            //long/double arguments occupy two VM stack slots
                            paraStack = stack_create(arguments * 2 + 1);
                            s32 i;
                            for (i = 0; i < arguments; i++) {
                                ValueType vt;
                                readValueType(req, &vt);

                                switch (getSimpleTag(vt.type)) {
                                    case '8':
                                        push_long(paraStack, vt.value);
                                        break;
                                    case 'R':
                                        push_ref(paraStack, (__refer) (intptr_t) vt.value);
                                        break;
                                    default:
                                        push_int(paraStack, (s32) vt.value);
                                }
                            }
                        }
                        (void) jdwppacket_read_int(req); //invoke options

                        // Execute constructor using instance_init_with_para
                        ArrayList *ctor_snapshot = NULL;
                        s16 invoke_error = jdwp_invoke_begin(jdwpserver, &ctor_snapshot);
                        if (invoke_error != JDWP_ERROR_NONE) {
                            if (paraStack) stack_destroy(paraStack);
                            jdwppacket_set_err(res, invoke_error);
                            jdwp_reply_put(client, res);
                            gc_move_objs_thread_2_gc(runtime);
                            gc_resume(jdwpserver->jvm->collector);
                            break;
                        }
                        gc_make_room(jdwpserver->jvm); //headroom, see invoke_method
                        jdwpserver->thread_sync_ignore = 1;

                        // Save current stack state
                        s32 saved_stack_size = stack_size(runtime->stack);

                        // Build method signature and call constructor
                        Utf8String *methodSig = utf8_create_copy(constructor->descriptor);
                        instance_init_with_para(newInstance, runtime, (c8 *) utf8_cstr(methodSig), paraStack);
                        utf8_destroy(methodSig);

                        jdwpserver->thread_sync_ignore = 0;
                        jdwp_invoke_end(jdwpserver, ctor_snapshot);

                        // Check if constructor threw exception
                        s32 current_stack_size = stack_size(runtime->stack);
                        s32 ret = (current_stack_size > saved_stack_size)
                                      ? RUNTIME_STATUS_EXCEPTION
                                      : RUNTIME_STATUS_NORMAL;

                        jdwppacket_set_err(res, JDWP_ERROR_NONE);

                        ValueType newInstanceValue, exceptionValue;
                        memset(&newInstanceValue, 0, sizeof(ValueType));
                        memset(&exceptionValue, 0, sizeof(ValueType));

                        if (ret == RUNTIME_STATUS_EXCEPTION) {
                            // Constructor threw an exception
                            Instance *exception = (Instance *) pop_ref(runtime->stack);

                            // Set return value to null
                            newInstanceValue.type = JDWP_TAG_OBJECT;
                            newInstanceValue.value = 0;

                            // Set exception object
                            if (exception) {
                                exceptionValue.type = getInstanceOfClassTag(exception);
                                exceptionValue.value = (s64) (intptr_t) exception;
                                jdwp_client_hold_obj(client, exception);
                            } else {
                                exceptionValue.type = JDWP_TAG_OBJECT;
                                exceptionValue.value = 0;
                            }
                        } else {
                            // Constructor completed normally
                            newInstanceValue.type = getInstanceOfClassTag(newInstance);
                            newInstanceValue.value = (s64) (intptr_t) newInstance;

                            // No exception
                            exceptionValue.type = JDWP_TAG_OBJECT;
                            exceptionValue.value = 0;
                        }

                        // Write return value and exception
                        writeValueType(res, &newInstanceValue);
                        writeValueType(res, &exceptionValue);

                        // Clean up parameter stack
                        if (paraStack) {
                            stack_destroy(paraStack);
                        }
                    } else {
                        // Instance creation failed
                        jdwppacket_set_err(res, JDWP_ERROR_OUT_OF_MEMORY);
                    }

                    jdwp_reply_put(client, res);
                    gc_move_objs_thread_2_gc(runtime);
                    gc_resume(jdwpserver->jvm->collector);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                    jdwp_reply_put(client, res);
                }
                break;
            }
            //set 4

            case JDWP_CMD_ArrayType_NewInstance: {
                //4.1
                JClass *arrayType = jdwppacket_read_refer(req);
                if (is_class_exists(jdwpserver->jvm, arrayType)) {
                    // Check if it's actually an array type
                    if (arrayType->mb.arr_type_index == 0) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                        jdwp_reply_put(client, res);
                        break;
                    }

                    s32 length = jdwppacket_read_int(req);

                    if (length < 0) {
                        // Negative array size
                        jdwppacket_set_err(res, JDWP_ERROR_ILLEGAL_ARGUMENT);
                        jdwp_reply_put(client, res);
                        break;
                    }

                    // Check for potential integer overflow in size calculation
                    // (array header size included, per the current layout)
                    s32 typeIdx = arrayType->mb.arr_type_index;
                    if (jvm_array_alloc_size(typeIdx, length) < 0) {
                        jdwppacket_set_err(res, JDWP_ERROR_OUT_OF_MEMORY);
                        jdwp_reply_put(client, res);
                        break;
                    }

                    gc_pause(jdwpserver->jvm->collector);
                    Runtime *runtime = jdwp_get_runtime(jdwpserver);

                    // Create array instance
                    Instance *newArray = jarray_create_by_class(runtime, length, arrayType);
                    if (newArray) {
                        jdwp_client_hold_obj(client, newArray);
                        jdwppacket_set_err(res, JDWP_ERROR_NONE);

                        // Return new array instance
                        ValueType arrayValue;
                        arrayValue.type = getInstanceOfClassTag(newArray);
                        arrayValue.value = (s64) (intptr_t) newArray;
                        writeValueType(res, &arrayValue);
                    } else {
                        // Array creation failed
                        jdwppacket_set_err(res, JDWP_ERROR_OUT_OF_MEMORY);
                    }

                    jdwp_reply_put(client, res);
                    gc_move_objs_thread_2_gc(runtime);
                    gc_resume(jdwpserver->jvm->collector);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                    jdwp_reply_put(client, res);
                }
                break;
            }
            //set 5
            //set 6

            case JDWP_CMD_Method_LineTable: {
                //6.1
                __refer refType = jdwppacket_read_refer(req);
                JClass *ref = (JClass *) (refType);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    MethodInfo *method_id = jdwppacket_read_refer(req);
                    MethodInfo *method = jdwp_find_method_id(ref, method_id);
                    if (!method) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_METHODID);
                        jdwp_reply_put(client, res);
                        break;
                    }
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    CodeAttribute *ca = method->converted_code;
                    if (method->is_native) {
                        jdwppacket_write_long(res, -1);
                        jdwppacket_write_long(res, -1);
                        jdwppacket_write_int(res, 0);
                    } else {
                        jdwppacket_write_long(res, 0);
                        jdwppacket_write_long(res, ca ? ca->code_length : 0);
                        jdwppacket_write_int(res, ca ? ca->line_number_table_length : 0);
                        s32 i;
                        for (i = 0; ca && i < ca->line_number_table_length; i++) {
                            jdwppacket_write_long(res, ca->line_number_table[i].start_pc);
                            jdwppacket_write_int(res, ca->line_number_table[i].line_number);
                        }
                    }
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_Method_VariableTable: {
                //6.2
                __refer refType = jdwppacket_read_refer(req);
                JClass *ref = (JClass *) (refType);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    MethodInfo *method_id = jdwppacket_read_refer(req);
                    MethodInfo *method = jdwp_find_method_id(ref, method_id);
                    if (!method) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_METHODID);
                        jdwp_reply_put(client, res);
                        break;
                    }
                    CodeAttribute *ca = method->converted_code;
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    s32 tablen = ca ? ca->local_var_table_length : 0;
                    jdwppacket_write_int(res, method->para_slots); //slot count
                    jdwppacket_write_int(res, tablen); // para count
                    s32 i;
                    for (i = 0; i < tablen; i++) {
                        LocalVarTable *tab = &ca->local_var_table[i];
                        jdwppacket_write_long(res, tab->start_pc);
                        jdwppacket_write_utf(res, class_get_utf8_string(ref, tab->name_index));
                        jdwppacket_write_utf(res, class_get_utf8_string(ref, tab->descriptor_index));
                        jdwppacket_write_int(res, tab->length);
                        jdwppacket_write_int(res, tab->index);
                    }
                    //jvm_printf("[JDWP]Method_VariableTable:\n");
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_Method_Bytecodes: {
                //6.3 classfile bytecode. `ca->code` is rewritten in place at
                //runtime (op_getstatic_ref etc.), and the empty-call NOP
                //optimization additionally touches `bytecode_for_jit`, so
                //neither buffer is strictly pristine - accepted per project
                //decision (impact is 3 NOP bytes in rare call sites). We
                //serve `bytecode_for_jit`: it keeps javac's original
                //instruction stream everywhere except those NOP-ed empty
                //calls. Native/abstract methods report an empty array (JDI
                //bytecodes() contract). GC is paused BEFORE resolving and
                //traversing class/method: the class may not unload mid-read.
                __refer refType = jdwppacket_read_refer(req);
                JClass *ref = (JClass *) (refType);
                gc_pause(jdwpserver->jvm->collector);
                if (is_class_exists(jdwpserver->jvm, ref)) {
                    MethodInfo *method_id = jdwppacket_read_refer(req);
                    MethodInfo *method = jdwp_find_method_id(ref, method_id);
                    if (!method) {
                        gc_resume(jdwpserver->jvm->collector);
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_METHODID);
                        jdwp_reply_put(client, res);
                        break;
                    }
                    CodeAttribute *ca = method->converted_code;
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    if (ca && ca->bytecode_for_jit) {
                        jdwppacket_write_int(res, ca->code_length);
                        jdwppacket_write_buf(res, (c8 *) ca->bytecode_for_jit, ca->code_length);
                    } else {
                        jdwppacket_write_int(res, 0);
                    }
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS);
                }
                gc_resume(jdwpserver->jvm->collector);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_Method_IsObsolete: {
                //6.4
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_Method_VariableTableWithGeneric: {
                //6.5
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            //set 8
            //set 9

            case JDWP_CMD_ObjectReference_ReferenceType: {
                //9.1
                Instance *obj = (Instance *) jdwppacket_read_refer(req);
                JClass *ref = obj->mb.clazz;
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_byte(res, getClassType(ref));
                if (obj->mb.type == MEM_TYPE_CLASS) {
                    //类对象
                    ref = classes_get_c(jdwpserver->jvm, NULL, STR_CLASS_JAVA_LANG_CLASS);
                }
                jdwppacket_write_refer(res, ref);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ObjectReference_GetValues: {
                //9.2
                gc_pause(jdwpserver->jvm->collector);
                Runtime *runtime = jdwp_get_runtime(jdwpserver);
                Instance *obj = (Instance *) jdwppacket_read_refer(req);
                if (!obj || !gc_is_alive(jdwpserver->jvm->collector, obj)) {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_OBJECT);
                    jdwp_reply_put(client, res);
                    gc_resume(jdwpserver->jvm->collector);
                    break;
                }
                JClass *ref = obj->mb.clazz;
                s32 fields = jdwppacket_read_int(req);
                jdwppacket_set_err(res, fields < 0 ? JDWP_ERROR_INVALID_LENGTH : JDWP_ERROR_NONE);
                //values count MUST lead the reply: without it the debugger
                //parses the first tag byte into the count (garbage ~2 billion)
                //and dies rendering - the "statics show OOM / cannot expand"
                //IDE symptom
                if (fields >= 0) jdwppacket_write_int(res, fields);
                s32 i;
                for (i = 0; i < fields; i++) {
                    FieldInfo *field_id = jdwppacket_read_refer(req);
                    FieldInfo *fi = jdwp_find_field_id(ref, field_id);
                    if (!fi || (fi->access_flags & ACC_STATIC)) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_FIELDID);
                        break;
                    }
                    ValueType vt;
                    vt.type = getJdwpTag(fi->descriptor);
                    c8 *ptr = getFieldPtr_byName(obj, obj->mb.clazz->name, fi->name, fi->descriptor, runtime);
                    vt.value = getPtrValue(vt.type, ptr);
                    writeValueType(res, &vt);
                }
                jdwp_reply_put(client, res);

                gc_move_objs_thread_2_gc(runtime);
                gc_resume(jdwpserver->jvm->collector);
                break;
            }
            case JDWP_CMD_ObjectReference_SetValues: {
                //9.3
                gc_pause(jdwpserver->jvm->collector);
                Runtime *runtime = jdwp_get_runtime(jdwpserver);
                Instance *obj = (Instance *) jdwppacket_read_refer(req);
                if (!obj) {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_OBJECT);
                    jdwp_reply_put(client, res);
                    gc_resume(jdwpserver->jvm->collector);
                    break;
                }
                JClass *ref = obj->mb.clazz;
                s32 fields = jdwppacket_read_int(req);
                jdwppacket_set_err(res, fields < 0 ? JDWP_ERROR_INVALID_LENGTH : JDWP_ERROR_NONE);
                s32 i;
                for (i = 0; i < fields; i++) {
                    FieldInfo *field_id = jdwppacket_read_refer(req);
                    FieldInfo *fi = jdwp_find_field_id(ref, field_id);
                    if (!fi || (fi->access_flags & ACC_STATIC)) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_FIELDID);
                        break;
                    }
                    ValueType vt;
                    vt.type = getJdwpTag(fi->descriptor);
                    readValueType_untagged(req, &vt);
                    c8 *ptr = getInstanceFieldPtr(obj, fi);
                    setPtrValue(vt.type, ptr, vt.value);
                }
                jdwp_reply_put(client, res);

                gc_move_objs_thread_2_gc(runtime);
                gc_resume(jdwpserver->jvm->collector);
                break;
            }
            case JDWP_CMD_ObjectReference_MonitorInfo: {
                //9.5 owner + entry count + waiting threads, from the
                // ThreadLock side table (query only: never create a lock).
                //GC is paused BEFORE any dereference: the object id may be
                //stale, and owner->jthread stays reachable only under the
                //pause. An object WITHOUT a monitor is answered null/0/0 -
                //scanning against a NULL tl would falsely list every
                //unlocked thread (their markers are NULL too).
                Instance *obj = (Instance *) jdwppacket_read_refer(req);
                gc_pause(jdwpserver->jvm->collector);
                if (!obj || !gc_is_alive(jdwpserver->jvm->collector, obj)) {
                    gc_resume(jdwpserver->jvm->collector);
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_OBJECT);
                    jdwp_reply_put(client, res);
                    break;
                }
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                ThreadLock *tl = obj->mb.thread_lock;
                if (!tl) {
                    jdwppacket_write_refer(res, NULL);
                    jdwppacket_write_int(res, 0);
                    jdwppacket_write_int(res, 0);
                    gc_resume(jdwpserver->jvm->collector);
                    jdwp_reply_put(client, res);
                    break;
                }
                Instance *owner_thread = NULL;
                s32 entry_count = 0;
                spin_lock(&tl->metadata_lock);
                if (tl->owner_thread) {
                    owner_thread = tl->owner_thread->jthread;
                    entry_count = (s32) tl->recursion_count;
                }
                spin_unlock(&tl->metadata_lock);
                //waiters: threads parked via is_suspend (executing nothing)
                //or is_blocking (parked inside native; its marker is stable
                //while blocked and only transitions on wake, where both the
                //before and after states are legitimate observations).
                {
                    MiniJVM *jvm = jdwpserver->jvm;
                    ArrayList *waiters = arraylist_create(4);
                    spin_lock(&jvm->thread_list->spinlock);
                    {
                        s32 i;
                        for (i = 0; i < jvm->thread_list->length; i++) {
                            Runtime *t = arraylist_get_value_unsafe(jvm->thread_list, i);
                            spin_lock(&t->thrd_info->lock);
                            s32 waiting = t->thrd_info->waiting_lock == tl
                                          || t->thrd_info->entering_lock == tl;
                            spin_unlock(&t->thrd_info->lock);
                            if (waiting && (t->thrd_info->is_suspend || t->thrd_info->is_blocking)) {
                                arraylist_push_back(waiters, t->thrd_info->jthread);
                            }
                        }
                    }
                    spin_unlock(&jvm->thread_list->spinlock);
                    jdwppacket_write_refer(res, owner_thread);
                    jdwppacket_write_int(res, entry_count);
                    jdwppacket_write_int(res, waiters->length);
                    s32 i;
                    for (i = 0; i < waiters->length; i++) {
                        jdwppacket_write_refer(res, arraylist_get_value_unsafe(waiters, i));
                    }
                    arraylist_destroy(waiters);
                }
                gc_resume(jdwpserver->jvm->collector);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ObjectReference_InvokeMethod: {
                //9.6

                invoke_method(CALL_MODE_INSTANCE, req, res, client);
                break;
            }
            case JDWP_CMD_ObjectReference_DisableCollection: {
                //9.7
                gc_pause(jdwpserver->jvm->collector);
                Instance *obj = (Instance *) jdwppacket_read_refer(req);
                jdwp_client_hold_obj(client, obj);

                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwp_reply_put(client, res);
                //                //jvm_printf("[JDWP]ObjectReference_DisableCollection:" + obj);
                gc_resume(jdwpserver->jvm->collector);
                break;
            }
            case JDWP_CMD_ObjectReference_EnableCollection: {
                //9.8
                Instance *obj = (Instance *) jdwppacket_read_refer(req);
                //Eclipse have a nonhuman opration that access a object after enable collection the same one
                //so this garbage_derefer may be release the object
                //                garbage_refer_count_dec(obj);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwp_reply_put(client, res);
                //                //jvm_printf("[JDWP]ObjectReference_EnableCollection:" + obj);
                break;
            }
            case JDWP_CMD_ObjectReference_IsCollected: {
                //9.9
                gc_pause(jdwpserver->jvm->collector);
                Instance *obj = (Instance *) jdwppacket_read_refer(req);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_byte(res, !gc_is_alive(jdwpserver->jvm->collector, obj));
                jdwp_reply_put(client, res);
                gc_resume(jdwpserver->jvm->collector);
                break;
            }
            //set 10

            case JDWP_CMD_StringReference_Value: {
                //10.1
                Instance *jstr = jdwppacket_read_refer(req);
                Utf8String *ustr = utf8_create();
                Runtime *runtime = jdwp_get_runtime(jdwpserver);
                jstring_2_utf8(jstr, ustr, runtime);
                gc_move_objs_thread_2_gc(runtime);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_utf(res, ustr);
                jdwp_reply_put(client, res);
                //jvm_printf("[JDWP]ThreadReference_Name:%s\n", utf8_cstr(ustr));
                utf8_destroy(ustr);
                break;
            }
            //set 11

            case JDWP_CMD_ThreadReference_Name: {
                //11.1
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r) {
                    Instance *jarr_name = jthread_get_name_value(jdwpserver->jvm, jthread);
                    Utf8String *ustr = utf8_create();
                    unicode_2_utf8((u16 *) jarray_body(jarr_name), ustr, jarray_length(jarr_name));

                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_utf(res, ustr);
                    utf8_destroy(ustr);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                jdwp_reply_put(client, res);
                //jvm_printf("[JDWP]ThreadReference_Name:%s\n", utf8_cstr(ustr));
                break;
            }
            case JDWP_CMD_ThreadReference_Suspend: {
                //11.2
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r) {
                    jthread_suspend(r);
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_Resume: {
                //11.3
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r) {
                    if (r->thrd_info->suspend_count > 0)jthread_resume(r);;
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_Status: {
                //11.4
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_int(res, jdwp_map_thread_status(r->thrd_info->thread_status));
                    //JDWP suspend status means "suspended by the debugger".
                    //A thread merely blocking in native/sleep/lock (is_blocking) is
                    //NOT suspended; reporting it as such makes the debugger cache
                    //frames read at that moment and later display stale locations.
                    jdwppacket_write_int(res, r->thrd_info->suspend_count > 0
                                                  ? JDWP_SUSPEND_STATUS_SUSPENDED
                                                  : 0);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                jdwp_reply_put(client, res);
                break;
            }

            case JDWP_CMD_ThreadReference_ThreadGroup: {
                //11.5
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r) {
                    Runtime *jr = jdwp_get_runtime(jdwpserver);
                    gc_pause(jvm->collector);
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_refer(res, jdwp_threadgroup_of(jvm, jr, jthread));
                    gc_move_objs_thread_2_gc(jr);
                    gc_resume(jvm->collector);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_Frames: {
                //11.6
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *rt = NULL;
                s32 startFrame = jdwppacket_read_int(req);
                s32 length = jdwppacket_read_int(req);
                s16 validation = jdwp_validate_suspended_thread(jdwpserver, jthread, &rt);
                if (validation != JDWP_ERROR_NONE) {
                    jdwppacket_set_err(res, validation);
                    jdwp_reply_put(client, res);
                    break;
                }
                s32 depth = getRuntimeDepth(rt);
                if (startFrame < 0 || startFrame > depth) {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_INDEX);
                    jdwp_reply_put(client, res);
                    break;
                }
                if (length < -1) {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_LENGTH);
                    jdwp_reply_put(client, res);
                    break;
                }
                s32 remaining = depth - startFrame;
                if (length == -1 || length > remaining) length = remaining;
                //jvm_printf("[JDWP]ThreadReference_Frames: startFrame=%d, len=%d\n", startFrame, length);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_int(res, length);
                Runtime *r = getLastSon(rt);
                s32 i;
                for (i = 0; r && i < startFrame + length; i++) {
                    if (i >= startFrame) {
                        jdwppacket_write_refer(res, r);
                        Location loc;
                        loc.typeTag = getClassType(r->clazz);
                        loc.classID = r->clazz;
                        loc.methodID = r->method;
                        if (r->method->converted_code)
                            loc.execIndex = (s64) (intptr_t) r->pc - (s64) (intptr_t) r->method->converted_code->code;
                        else
                            loc.execIndex = 0;
                        writeLocation(res, &loc);
                    }
                    r = r->parent;
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_FrameCount: {
                //11.7
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r && r->thrd_info->suspend_count == 0) {
                    r = NULL;
                    jdwppacket_set_err(res, JDWP_ERROR_THREAD_NOT_SUSPENDED);
                    jdwp_reply_put(client, res);
                    break;
                }
                if (r) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_int(res, getRuntimeDepth(r));
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                //jvm_printf("[JDWP]ThreadReference_FrameCount:%d\n", getRuntimeDepth(r));
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_OwnedMonitors: {
                //11.8 mirror of the per-thread held-lock side table that
                // thread_add_held_lock maintains in JDWP mode. Entries are
                // MemoryBlock*: instance monitors report the object id,
                // static-synchronized locks report the JClass (== class
                // object id in our id scheme).
                Instance *jthread = jdwppacket_read_refer(req);
                //GC paused across the WHOLE handler incl. the park wait:
                //runtime disposal runs on the GC thread, so the Runtime and
                //its held_locks cannot be freed under us even if the thread
                //exits meanwhile
                gc_pause(jdwpserver->jvm->collector);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r && r->thrd_info->suspend_count == 0) {
                    r = NULL;
                    gc_resume(jdwpserver->jvm->collector);
                    jdwppacket_set_err(res, JDWP_ERROR_THREAD_NOT_SUSPENDED);
                    jdwp_reply_put(client, res);
                    break;
                }
                s16 parked = jdwp_wait_thread_parked(r, 500);
                if (r && parked != JDWP_ERROR_NONE) {
                    gc_resume(jdwpserver->jvm->collector);
                    jdwppacket_set_err(res, parked);
                    jdwp_reply_put(client, res);
                    break;
                }
                if (r) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    spin_lock(&r->thrd_info->lock);
                    ArrayList *held = r->thrd_info->held_locks;
                    spin_unlock(&r->thrd_info->lock);
                    if (held) spin_lock(&held->spinlock);
                    s32 n = held ? held->length : 0;
                    //snapshot the list first: encoding while paused keeps
                    //the object ids alive regardless of later releases
                    jdwppacket_write_int(res, n);
                    s32 i;
                    for (i = 0; i < n; i++) {
                        __refer m = (__refer) arraylist_get_value_unsafe(held, i);
                        //spec: each owned monitor is a TAGGED object id
                        jdwppacket_write_byte(res, getInstanceOfClassTag((Instance *) m));
                        jdwppacket_write_refer(res, m);
                    }
                    if (held) spin_unlock(&held->spinlock);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                gc_resume(jdwpserver->jvm->collector);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_CurrentContendedMonitor: {
                //11.9 the monitor this thread is waiting in (Object.wait)
                // or blocked entering (monitorenter), via the side-table
                // markers jthread_wait/jthread_lock maintain
                Instance *jthread = jdwppacket_read_refer(req);
                gc_pause(jdwpserver->jvm->collector); //whole-handler lifetime, see OwnedMonitors
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r && r->thrd_info->suspend_count == 0) {
                    r = NULL;
                    gc_resume(jdwpserver->jvm->collector);
                    jdwppacket_set_err(res, JDWP_ERROR_THREAD_NOT_SUSPENDED);
                    jdwp_reply_put(client, res);
                    break;
                }
                s16 parked = jdwp_wait_thread_parked(r, 500);
                if (r && parked != JDWP_ERROR_NONE) {
                    gc_resume(jdwpserver->jvm->collector);
                    jdwppacket_set_err(res, parked);
                    jdwp_reply_put(client, res);
                    break;
                }
                if (r) {
                    spin_lock(&r->thrd_info->lock);
                    ThreadLock *tl = r->thrd_info->waiting_lock
                                     ? r->thrd_info->waiting_lock
                                     : r->thrd_info->entering_lock;
                    spin_unlock(&r->thrd_info->lock);
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    //spec: tagged object id (null stays plain null)
                    if (tl) {
                        jdwppacket_write_byte(res, getInstanceOfClassTag((Instance *) tl->object));
                        jdwppacket_write_refer(res, tl->object);
                    } else {
                        jdwppacket_write_byte(res, JDWP_TAG_OBJECT);
                        jdwppacket_write_refer(res, NULL);
                    }
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                gc_resume(jdwpserver->jvm->collector);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_Stop: {
                //11.10
                //
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r) {
                    r->thrd_info->is_stop = 1;
                    jthread_wakeup(r);
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_Interrupt: {
                //11.11
                //
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r) {
                    jthread_interrupt(r);
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadReference_SuspendCount: {
                //11.12
                //
                Instance *jthread = jdwppacket_read_refer(req);
                Runtime *r = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
                if (r) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    jdwppacket_write_int(res, r->thrd_info->suspend_count);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_THREAD);
                }
                //jvm_printf("[JDWP]ThreadReference_SuspendCount:%llx,%d\n", (s64) (intptr_t) jthread,r->threadInfo->suspend_count);
                jdwp_reply_put(client, res);
                break;
            }
            //set 12

            case JDWP_CMD_ThreadGroupReference_Name: {
                //12.1
                Instance *group = jdwppacket_read_refer(req);
                Runtime *jr = jdwp_get_runtime(jdwpserver);
                gc_pause(jvm->collector);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                Utf8String *ustr = utf8_create();
                jdwp_threadgroup_name(jvm, jr, group, ustr);
                jdwppacket_write_utf(res, ustr);
                utf8_destroy(ustr);
                gc_move_objs_thread_2_gc(jr);
                gc_resume(jvm->collector);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadGroupReference_Parent: {
                //12.2
                Instance *group = jdwppacket_read_refer(req);
                Runtime *jr = jdwp_get_runtime(jdwpserver);
                gc_pause(jvm->collector);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_refer(res, jdwp_threadgroup_parent(jvm, jr, group));
                gc_move_objs_thread_2_gc(jr);
                gc_resume(jvm->collector);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ThreadGroupReference_Children: {
                //12.3 child threads live in thread_list, child groups are
                //inferred from those threads' group.parent chains
                Instance *group = jdwppacket_read_refer(req);
                Runtime *jr = jdwp_get_runtime(jdwpserver);
                gc_pause(jvm->collector);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                ArrayList *threads = arraylist_create(4);
                ArrayList *groups = arraylist_create(4);
                spin_lock(&jvm->thread_list->spinlock);
                {
                    s32 i;
                    for (i = 0; i < jvm->thread_list->length; i++) {
                        Runtime *t = arraylist_get_value_unsafe(jvm->thread_list, i);
                        Instance *jthread = t->thrd_info->jthread;
                        Instance *grp = jthread ? jdwp_threadgroup_of(jvm, jr, jthread) : NULL;
                        if (grp == group) {
                            arraylist_push_back(threads, jthread);
                        } else if (grp) {
                            Instance *parent = jdwp_threadgroup_parent(jvm, jr, grp);
                            if (parent == group &&
                                arraylist_index_of(groups, arraylist_compare_ptr, grp) < 0) {
                                arraylist_push_back(groups, grp);
                            }
                        }
                    }
                }
                spin_unlock(&jvm->thread_list->spinlock);
                jdwppacket_write_int(res, threads->length);
                s32 i;
                for (i = 0; i < threads->length; i++) {
                    jdwppacket_write_refer(res, arraylist_get_value_unsafe(threads, i));
                }
                jdwppacket_write_int(res, groups->length);
                for (i = 0; i < groups->length; i++) {
                    jdwppacket_write_refer(res, arraylist_get_value_unsafe(groups, i));
                }
                arraylist_destroy(threads);
                arraylist_destroy(groups);
                gc_move_objs_thread_2_gc(jr);
                gc_resume(jvm->collector);
                jdwp_reply_put(client, res);
                break;
            }
            //set 13
            case JDWP_CMD_ArrayReference_Length: {
                //13.1
                Instance *arr = jdwppacket_read_refer(req);
                //                if (gc_is_alive(arr)) {
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwppacket_write_int(res, jarray_length(arr));
                //                } else {
                //                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_ARRAY);
                //                }
                //jvm_printf("[JDWP]ArrayReference_Length:%d\n", jarray_length(arr));
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ArrayReference_GetValues: {
                //13.2
                Instance *arr = jdwppacket_read_refer(req);
                s32 firstIndex = jdwppacket_read_int(req);
                s32 length = jdwppacket_read_int(req);
                if (jarray_length(arr) < firstIndex + length) {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_LENGTH);
                    jdwp_reply_put(client, res);
                    break;
                }
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                writeArrayRegion(res, arr, firstIndex, length);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_ArrayReference_SetValues: {
                //13.3
                Instance *arr = jdwppacket_read_refer(req);
                s32 firstIndex = jdwppacket_read_int(req);
                s32 length = jdwppacket_read_int(req);
                if (jarray_length(arr) < firstIndex + length) {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_LENGTH);
                    jdwp_reply_put(client, res);
                    break;
                }
                readArrayRegion(req, arr, firstIndex, length);
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwp_reply_put(client, res);
                break;
            }
            //set 14
            case JDWP_CMD_ClassLoaderReference_VisibleClasses: {
                //14.1
                //VisibleClasses Command
                //The list contains each reference type defined by this loader and any types for which loading was delegated by this class loader to another class loader.
                //
                Instance *classLoader = jdwppacket_read_refer(req);
                MiniJVM *jvm = jdwpserver->jvm;
                PeerClassLoader *pcl = classLoaders_find_by_instance(jvm, classLoader);
                if (pcl) {
                    spin_lock(&jvm->lock_cloader);
                    {
                        jdwppacket_set_err(res, JDWP_ERROR_NONE);
                        jdwppacket_write_int(res, (s32) (pcl->classes->entries));
                        HashtableIterator hti;
                        hashtable_iterate(pcl->classes, &hti);
                        for (; hashtable_iter_has_more(&hti);) {
                            Utf8String *k = hashtable_iter_next_key(&hti);
                            JClass *cl = hashtable_get(pcl->classes, k);

                            jdwppacket_write_byte(res, getClassType(cl));
                            jdwppacket_write_refer(res, cl);
                        }
                    }
                    spin_unlock(&jvm->lock_cloader);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_CLASS_LOADER);
                }
                jdwp_reply_put(client, res);
                break;
            }
            //set 15
            case JDWP_CMD_EventRequest_Set: {
                //15.1
                EventSet *eventSet = jdwp_eventset_create(jdwpserver, client, req);
                jdwp_eventset_put(jdwpserver, eventSet);
                s16 ret = jdwp_eventset_set(jdwpserver, eventSet);


                if (ret == JDWP_ERROR_NONE) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                } else {
                    jdwppacket_set_err(res, ret);
                }
                jdwppacket_write_int(res, eventSet->requestId);
                jdwp_reply_put(client, res);
                break;
            } //
            case JDWP_CMD_EventRequest_Clear: {
                //15.2
                u8 eventKind = jdwppacket_read_byte(req);
                s32 requestID = jdwppacket_read_int(req);
                //jvm_printf("[JDWP]EventRequest_Clear:eventKind=%d, requestID=%d\n", eventKind, requestID);
                jdwp_eventset_clear(jdwpserver, requestID);


                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_EventRequest_ClearAllBreakpoints: {
                //15.3 jdb's bare `clear` relies on this removing every breakpoint
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                //drop breakpoint event sets first
                {
                    ArrayList *ids = arraylist_create(4);
                    mtx_lock(&jdwpserver->event_sets_lock);
                    {
                        s32 i;
                        for (i = 0; i < jdwpserver->event_sets->count; i++) {
                            Pair pair = pairlist_get_pair(jdwpserver->event_sets, i);
                            EventSet *set = (EventSet *) pair.right;
                            if (set->eventKind == JDWP_EVENTKIND_BREAKPOINT) {
                                arraylist_push_back(ids, pair.left);
                            }
                        }
                    }
                    mtx_unlock(&jdwpserver->event_sets_lock);
                    s32 i;
                    for (i = 0; i < ids->length; i++) {
                        jdwp_eventset_clear(jdwpserver, (s32) (intptr_t) arraylist_get_value_unsafe(ids, i));
                    }
                    arraylist_destroy(ids);
                }
                //then strip breakpoints from every loaded method
                {
                    MiniJVM *jvm = jdwpserver->jvm;
                    spin_lock(&jvm->lock_cloader);
                    {
                        s32 i;
                        for (i = 0; i < jvm->classloaders->length; i++) {
                            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
                            HashtableIterator hti;
                            hashtable_iterate(pcl->classes, &hti);
                            for (; hashtable_iter_has_more(&hti);) {
                                JClass *cl = hashtable_iter_next_value(&hti);
                                s32 j;
                                for (j = 0; j < cl->methodPool.method_used; j++) {
                                    MethodInfo *mi = &cl->methodPool.method[j];
                                    if (mi->breakpoint) {
                                        pairlist_destroy(mi->breakpoint);
                                        mi->breakpoint = NULL;
                                    }
                                }
                            }
                        }
                    }
                    spin_unlock(&jvm->lock_cloader);
                }
                jdwp_reply_put(client, res);
                break;
            }
            //set 16
            case JDWP_CMD_StackFrame_GetValues: {
                //16.1
                Instance *thread = jdwppacket_read_refer(req);
                Runtime *frame = jdwppacket_read_refer(req);
                s16 validation = jdwp_validate_suspended_frame(jdwpserver, thread, frame);
                if (validation != JDWP_ERROR_NONE) {
                    jdwppacket_set_err(res, validation);
                    jdwp_reply_put(client, res);
                    break;
                }
                CodeAttribute *ca = frame->method->converted_code;
                if (ca) {
                    jdwppacket_set_err(res, JDWP_ERROR_NONE);
                    s32 slots = jdwppacket_read_int(req);
                    if (slots < 0) {
                        jdwppacket_set_err(res, JDWP_ERROR_INVALID_LENGTH);
                        jdwp_reply_put(client, res);
                        break;
                    }
                    jdwppacket_write_int(res, slots);
                    Long2Double l2d;
                    s32 i, invalid_slot = 0;
                    for (i = 0; i < slots; i++) {
                        s32 slot = jdwppacket_read_int(req);
                        ValueType vt;
                        vt.type = jdwppacket_read_byte(req);
                        vt.value = 0;
                        if (slot >= 0 && slot < ca->max_locals) {
                            switch (getSimpleTag(vt.type)) {
                                case 'R': {
                                    Instance *ins = localvar_getRefer(frame->localvar, slot);
                                    vt.type = getInstanceOfClassTag(ins);
                                    vt.value = (s64) (intptr_t) ins;
                                    break;
                                }
                                case '8':
                                    l2d.l = localvar_getLong(frame->localvar, slot);
                                    vt.value = l2d.l;
                                    //can't skip i, localvar getvalue from received slot .
                                    break;
                                case '4':
                                case '2':
                                case '1':
                                    vt.value = localvar_getInt(frame->localvar, slot);
                                    break;
                            }
                        } else {
                            invalid_slot = 1;
                        }
                        writeValueType(res, &vt);
                        //jvm_printf("[JDWP]JDWP_CMD_StackFrame_GetValues,thead=%llx , frame=%llx, val=%llx\n", (s64) (intptr_t) thread,
                        //(s64) (intptr_t) frame, vt.value);
                    }
                    if (invalid_slot) jdwppacket_set_err(res, JDWP_ERROR_INVALID_SLOT);
                } else {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_SLOT);
                }
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_StackFrame_SetValues: {
                //16.2
                Instance *thread = jdwppacket_read_refer(req);
                Runtime *frame = jdwppacket_read_refer(req);
                s16 validation = jdwp_validate_suspended_frame(jdwpserver, thread, frame);
                if (validation != JDWP_ERROR_NONE) {
                    jdwppacket_set_err(res, validation);
                    jdwp_reply_put(client, res);
                    break;
                }

                s32 slotValues = jdwppacket_read_int(req);
                if (slotValues < 0) {
                    jdwppacket_set_err(res, JDWP_ERROR_INVALID_LENGTH);
                    jdwp_reply_put(client, res);
                    break;
                }
                CodeAttribute *ca = frame->method->converted_code;
                Long2Double l2d;
                s32 i, invalid_slot = ca ? 0 : 1;
                for (i = 0; i < slotValues; i++) {
                    s32 slot = jdwppacket_read_int(req);
                    ValueType vt;
                    readValueType(req, &vt);
                    if (ca && slot >= 0 && slot < ca->max_locals) {
                        switch (getSimpleTag(vt.type)) {
                            case 'R':
                                localvar_setRefer(frame->localvar, slot, (__refer) (intptr_t) vt.value);
                                break;
                            case '8':
                                l2d.l = vt.value;
                                localvar_setLong(frame->localvar, slot, l2d.l);
                                break;
                            case '4':
                            case '2':
                            case '1':
                                localvar_setInt(frame->localvar, slot, (s32) vt.value);
                                break;
                        }
                    } else {
                        invalid_slot = 1;
                    }

                    //jvm_printf("[JDWP]StackFrame_SetValues,thead=%llx , frame=%llx, val=%llx\n", (s64) (intptr_t) thread,
                    //(s64) (intptr_t) frame, vt.value);
                }

                jdwppacket_set_err(res, invalid_slot ? JDWP_ERROR_INVALID_SLOT : JDWP_ERROR_NONE);
                jdwp_reply_put(client, res);
                break;
            }
            case JDWP_CMD_StackFrame_ThisObject: {
                //16.3
                Instance *thread = jdwppacket_read_refer(req);
                Runtime *frame = jdwppacket_read_refer(req);
                s16 validation = jdwp_validate_suspended_frame(jdwpserver, thread, frame);
                if (validation != JDWP_ERROR_NONE) {
                    jdwppacket_set_err(res, validation);
                    jdwp_reply_put(client, res);
                    break;
                }
                jdwppacket_set_err(res, JDWP_ERROR_NONE);
                ValueType vt;
                if (frame->method->is_static || frame->method->is_native) {
                    vt.type = JDWP_TAG_OBJECT;
                    vt.value = 0;
                } else {
                    Instance *ins = localvar_getRefer(frame->localvar, 0);
                    vt.type = getInstanceOfClassTag(ins);
                    vt.value = (s64) (intptr_t) ins;
                }
                writeValueType(res, &vt);
                jdwp_reply_put(client, res);
                //jvm_printf("[JDWP]StackFrame_ThisObject,thead=%llx , frame=%llx \n", (s64) (intptr_t) thread,
                //(s64) (intptr_t) frame);
                break;
            }
            case JDWP_CMD_StackFrame_PopFrames: {
                //16.4
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            //set 17
            case JDWP_CMD_ClassObjectReference_ReflectedType: {
                //17.1
                JClass *classObject = jdwppacket_read_refer(req);

                jdwppacket_set_err(res, JDWP_ERROR_NONE);

                jdwppacket_write_byte(res, getClassType(classObject));
                jdwppacket_write_refer(res, classObject);
                //jvm_printf("[JDWP]ClassObjectReference_ReflectedType:%s\n", utf8_cstr(classObject->mb.clazz->name));
                jdwp_reply_put(client, res);
                break;
            }
            //set 64
            case JDWP_CMD_Event_Composite: {
                jvm_printf("[JDWP]%x not support\n", jdwppacket_get_cmd_err(req));
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
            default: {
                //every command must be answered, otherwise the debugger blocks
                //forever waiting for the reply
                jvm_printf("[JDWP]unknown cmd: %x\n", cmd);
                jdwppacket_set_err(res, JDWP_ERROR_NOT_IMPLEMENTED);
                jdwp_reply_put(client, res);
                break;
            }
        }
        jdwppacket_destroy(req);
        packetCount++;
    }
    return packetCount;
}

//==================================================    thread group    ==================================================

static Instance *jdwp_threadgroup_of(MiniJVM *jvm, Runtime *r, Instance *jthread) {
    c8 *ptr = getFieldPtr_byName_c(jthread, STR_CLASS_JAVA_LANG_THREAD, "group", "Ljava/lang/ThreadGroup;", r);
    if (!ptr)return NULL;
    return (Instance *) getFieldRefer(ptr);
}

static Instance *jdwp_threadgroup_parent(MiniJVM *jvm, Runtime *r, Instance *group) {
    if (!group)return NULL;
    c8 *ptr = getFieldPtr_byName_c(group, STR_CLASS_JAVA_LANG_THREAD_GROUP, "parent", "Ljava/lang/ThreadGroup;", r);
    if (!ptr)return NULL;
    return (Instance *) getFieldRefer(ptr);
}

static void jdwp_threadgroup_name(MiniJVM *jvm, Runtime *r, Instance *group, Utf8String *out) {
    utf8_clear(out);
    if (!group)return;
    c8 *ptr = getFieldPtr_byName_c(group, STR_CLASS_JAVA_LANG_THREAD_GROUP, "name", "Ljava/lang/String;", r);
    if (!ptr)return;
    Instance *name = (Instance *) getFieldRefer(ptr);
    if (name) {
        jstring_2_utf8(name, out, r);
    }
}

static s32 jdwp_map_thread_status(u8 status) {
    //JDWP: ZOMBIE=0 RUNNING=1 SLEEPING=2 MONITOR=3 WAIT=4
    switch (status) {
        case THREAD_STATUS_RUNNING:
            return JDWP_THREAD_RUNNING;
        case THREAD_STATUS_SLEEPING:
            return JDWP_THREAD_SLEEPING;
        case THREAD_STATUS_MONITOR:
            return JDWP_THREAD_MONITOR;
        case THREAD_STATUS_WAIT:
            return JDWP_THREAD_WAIT;
        default:
            return JDWP_THREAD_ZOMBIE;
    }
}

static s16 jdwp_validate_suspended_thread(JdwpServer *jdwpserver, Instance *jthread, Runtime **thread_runtime) {
    Runtime *runtime = find_jthread_from_threadlist(jdwpserver->jvm, jthread);
    if (thread_runtime) *thread_runtime = runtime;
    if (!runtime) return JDWP_ERROR_INVALID_THREAD;
    if (runtime->thrd_info->suspend_count == 0) return JDWP_ERROR_THREAD_NOT_SUSPENDED;
    return JDWP_ERROR_NONE;
}

static s16 jdwp_validate_suspended_frame(JdwpServer *jdwpserver, Instance *jthread, Runtime *frame) {
    Runtime *thread_runtime = NULL;
    s16 error = jdwp_validate_suspended_thread(jdwpserver, jthread, &thread_runtime);
    if (error != JDWP_ERROR_NONE) return error;
    Runtime *candidate = getLastSon(thread_runtime);
    while (candidate && candidate->parent) {
        if (candidate == frame) return JDWP_ERROR_NONE;
        candidate = candidate->parent;
    }
    return JDWP_ERROR_INVALID_FRAMEID;
}

Runtime *find_jthread_from_threadlist(MiniJVM *jvm, Instance *jthread) {
    Runtime *otr = NULL;
    spin_lock(&jvm->thread_list->spinlock);
    {
        s32 i = 0;
        for (; i < jvm->thread_list->length; i++) {
            Runtime *r = arraylist_get_value_unsafe(jvm->thread_list, i);
            if (r->thrd_info->jthread == jthread) {
                otr = r;
            }
        }
    }
    spin_unlock(&jvm->thread_list->spinlock);
    if (!otr) {
        s32 debug = 1;
    }
    return otr;
}
