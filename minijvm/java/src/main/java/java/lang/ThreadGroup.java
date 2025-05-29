/*
 * Copyright (c) 1995, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import java.io.PrintStream;
import java.util.Arrays;
import java.lang.ref.WeakReference;


/**
 * A thread group represents a set of threads. In addition, a thread
 * group can also include other thread groups. The thread groups form
 * a tree in which every thread group except the initial thread group
 * has a parent.
 * <p>
 * A thread is allowed to access information about its own thread
 * group, but not to access information about its thread group's
 * parent thread group or any other thread groups.
 *
 * @author unascribed
 * @since JDK1.0
 */
/* The locking strategy for this code is to try to lock only one level of the
 * tree wherever possible, but otherwise to lock from the bottom up.
 * That is, from child thread groups to parents.
 * This has the advantage of limiting the number of locks that need to be held
 * and in particular avoids having to grab the lock for the root thread group,
 * (or a global lock) which would be a source of contention on a
 * multi-processor system with many thread groups.
 * This policy often leads to taking a snapshot of the state of a thread group
 * and working off of that snapshot, rather than holding the thread group locked
 * while we work on the children.
 */
public
class ThreadGroup implements Thread.UncaughtExceptionHandler {
    private final ThreadGroup parent;
    String name;
    int maxPriority;
    boolean destroyed;
    boolean daemon;
    boolean vmAllowSuspension;

    int nUnstartedThreads = 0;
    int nthreads;
    WeakReference<Thread> threads[];

    int ngroups;
    ThreadGroup groups[];

    /**
     * Creates an empty Thread group that is not in any Thread group.
     * This method is used to create the system Thread group.
     */
    ThreadGroup() {     // called from C code
        this.name = "system";
        this.maxPriority = Thread.MAX_PRIORITY;
        this.parent = null;
    }

    /**
     * Constructs a new thread group. The parent of this new group is
     * the thread group of the currently running thread.
     * <p>
     * The <code>checkAccess</code> method of the parent thread group is
     * called with no arguments; this may result in a security exception.
     *
     * @param name the name of the new thread group.
     * @throws SecurityException if the current thread cannot create a
     *                           thread in the specified thread group.
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public ThreadGroup(String name) {
        this(Thread.currentThread().getThreadGroup(), name);
    }

    /**
     * Creates a new thread group. The parent of this new group is the
     * specified thread group.
     * <p>
     * The <code>checkAccess</code> method of the parent thread group is
     * called with no arguments; this may result in a security exception.
     *
     * @param parent the parent thread group.
     * @param name   the name of the new thread group.
     * @throws NullPointerException if the thread group argument is
     *                              <code>null</code>.
     * @throws SecurityException    if the current thread cannot create a
     *                              thread in the specified thread group.
     * @see SecurityException
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public ThreadGroup(ThreadGroup parent, String name) {
        this(checkParentAccess(parent), parent, name);
    }

    private ThreadGroup(Void unused, ThreadGroup parent, String name) {
        this.name = name;
        this.maxPriority = parent.maxPriority;
        this.daemon = parent.daemon;
        this.vmAllowSuspension = parent.vmAllowSuspension;
        this.parent = parent;
        parent.add(this);
    }

    /*
     * @throws  NullPointerException  if the parent argument is {@code null}
     * @throws  SecurityException     if the current thread cannot create a
     *                                thread in the specified thread group.
     */
    private static Void checkParentAccess(ThreadGroup parent) {
        parent.checkAccess();
        return null;
    }

    /**
     * Returns the name of this thread group.
     *
     * @return the name of this thread group.
     * @since JDK1.0
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the parent of this thread group.
     * <p>
     * First, if the parent is not <code>null</code>, the
     * <code>checkAccess</code> method of the parent thread group is
     * called with no arguments; this may result in a security exception.
     *
     * @return the parent of this thread group. The top-level thread group
     * is the only thread group whose parent is <code>null</code>.
     * @throws SecurityException if the current thread cannot modify
     *                           this thread group.
     * @see ThreadGroup#checkAccess()
     * @see SecurityException
     * @see RuntimePermission
     * @since JDK1.0
     */
    public final ThreadGroup getParent() {
        if (parent != null)
            parent.checkAccess();
        return parent;
    }

    /**
     * Returns the maximum priority of this thread group. Threads that are
     * part of this group cannot have a higher priority than the maximum
     * priority.
     *
     * @return the maximum priority that a thread in this thread group
     * can have.
     * @see #setMaxPriority
     * @since JDK1.0
     */
    public final int getMaxPriority() {
        return maxPriority;
    }

    /**
     * Tests if this thread group is a daemon thread group. A
     * daemon thread group is automatically destroyed when its last
     * thread is stopped or its last thread group is destroyed.
     *
     * @return <code>true</code> if this thread group is a daemon thread group;
     * <code>false</code> otherwise.
     * @since JDK1.0
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * Tests if this thread group has been destroyed.
     *
     * @return true if this object is destroyed
     * @since JDK1.1
     */
    public synchronized boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Changes the daemon status of this thread group.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * A daemon thread group is automatically destroyed when its last
     * thread is stopped or its last thread group is destroyed.
     *
     * @param daemon if <code>true</code>, marks this thread group as
     *               a daemon thread group; otherwise, marks this
     *               thread group as normal.
     * @throws SecurityException if the current thread cannot modify
     *                           this thread group.
     * @see SecurityException
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public final void setDaemon(boolean daemon) {
        checkAccess();
        this.daemon = daemon;
    }

    /**
     * Sets the maximum priority of the group. Threads in the thread
     * group that already have a higher priority are not affected.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * If the <code>pri</code> argument is less than
     * {@link Thread#MIN_PRIORITY} or greater than
     * {@link Thread#MAX_PRIORITY}, the maximum priority of the group
     * remains unchanged.
     * <p>
     * Otherwise, the priority of this ThreadGroup object is set to the
     * smaller of the specified <code>pri</code> and the maximum permitted
     * priority of the parent of this thread group. (If this thread group
     * is the system thread group, which has no parent, then its maximum
     * priority is simply set to <code>pri</code>.) Then this method is
     * called recursively, with <code>pri</code> as its argument, for
     * every thread group that belongs to this thread group.
     *
     * @param pri the new priority of the thread group.
     * @throws SecurityException if the current thread cannot modify
     *                           this thread group.
     * @see #getMaxPriority
     * @see SecurityException
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public final void setMaxPriority(int pri) {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            if (pri < Thread.MIN_PRIORITY || pri > Thread.MAX_PRIORITY) {
                return;
            }
            maxPriority = (parent != null) ? Math.min(pri, parent.maxPriority) : pri;
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].setMaxPriority(pri);
        }
    }

    /**
     * Tests if this thread group is either the thread group
     * argument or one of its ancestor thread groups.
     *
     * @param g a thread group.
     * @return <code>true</code> if this thread group is the thread group
     * argument or one of its ancestor thread groups;
     * <code>false</code> otherwise.
     * @since JDK1.0
     */
    public final boolean parentOf(ThreadGroup g) {
        for (; g != null; g = g.parent) {
            if (g == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the currently running thread has permission to
     * modify this thread group.
     * <p>
     * If there is a security manager, its <code>checkAccess</code> method
     * is called with this thread group as its argument. This may result
     * in throwing a <code>SecurityException</code>.
     *
     * @throws SecurityException if the current thread is not allowed
     *                           to access this thread group.
     * @see SecurityManager#checkAccess(ThreadGroup)
     * @since JDK1.0
     */
    public final void checkAccess() {
    }

    /**
     * Returns an estimate of the number of active threads in this
     * thread group.  The result might not reflect concurrent activity,
     * and might be affected by the presence of certain system threads.
     * <p>
     * Due to the inherently imprecise nature of the result, it is
     * recommended that this method only be used for informational purposes.
     *
     * @return an estimate of the number of active threads in this thread
     * group and in any other thread group that has this thread
     * group as an ancestor.
     * @since JDK1.0
     */
    public int activeCount() {
        int result;
        // Snapshot sub-group data so we don't hold this lock
        // while our children are computing.
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }

            // Count only alive threads
            result = 0;
            for (int i = 0; i < nthreads; i++) {
                Thread t = threads[i].get();
                if (t != null && t.isAlive()) {
                    result++;
                }
            }

            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            result += groupsSnapshot[i].activeCount();
        }
        return result;
    }

    /**
     * Copies into the specified array every active thread in this
     * thread group and its subgroups.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * An application might use the <code>activeCount</code> method to
     * get an estimate of how big the array should be, however <i>if the
     * array is too short to hold all the threads, the extra threads are
     * silently ignored.</i>  If it is critical to obtain every active
     * thread in this thread group and its subgroups, the caller should
     * verify that the returned int value is strictly less than the length
     * of <tt>list</tt>.
     * <p>
     * Due to the inherent race condition in this method, it is recommended
     * that the method only be used for informational purposes.
     *
     * @param list an array into which to place the list of threads.
     * @return the number of threads put into the array.
     * @throws SecurityException if the current thread does not
     *                           have permission to enumerate this thread group.
     * @see ThreadGroup#activeCount()
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public int enumerate(Thread list[]) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    /**
     * Copies into the specified array every active thread in this
     * thread group. If the <code>recurse</code> flag is
     * <code>true</code>, references to every active thread in this
     * thread's subgroups are also included. If the array is too short to
     * hold all the threads, the extra threads are silently ignored.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * An application might use the <code>activeCount</code> method to
     * get an estimate of how big the array should be, however <i>if the
     * array is too short to hold all the threads, the extra threads are
     * silently ignored.</i>  If it is critical to obtain every active thread
     * in this thread group, the caller should verify that the returned int
     * value is strictly less than the length of <tt>list</tt>.
     * <p>
     * Due to the inherent race condition in this method, it is recommended
     * that the method only be used for informational purposes.
     *
     * @param list    an array into which to place the list of threads.
     * @param recurse a flag indicating whether also to include threads
     *                in thread groups that are subgroups of this
     *                thread group.
     * @return the number of threads placed into the array.
     * @throws SecurityException if the current thread does not
     *                           have permission to enumerate this thread group.
     * @see ThreadGroup#activeCount()
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public int enumerate(Thread list[], boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    private int enumerate(Thread list[], int n, boolean recurse) {
        int ngroupsSnapshot = 0;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }

            // Count only alive threads
            int nt = nthreads;
            if (nt > list.length - n) {
                nt = list.length - n;
            }
            int added = 0;
            for (int i = 0; i < nthreads && added < nt; i++) {
                Thread t = threads[i].get();
                if (t != null && t.isAlive()) {
                    list[n + added] = t;
                    added++;
                }
            }
            n += added;

            if (recurse) {
                ngroupsSnapshot = ngroups;
                if (groups != null) {
                    groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
                } else {
                    groupsSnapshot = null;
                }
            }
        }
        if (recurse) {
            for (int i = 0; i < ngroupsSnapshot; i++) {
                n = groupsSnapshot[i].enumerate(list, n, true);
            }
        }
        return n;
    }

    /**
     * Returns an estimate of the number of active groups in this
     * thread group.  The result might not reflect concurrent activity.
     * <p>
     * Due to the inherently imprecise nature of the result, it is
     * recommended that this method only be used for informational purposes.
     *
     * @return the number of active thread groups with this thread group as
     * an ancestor.
     * @since JDK1.0
     */
    public int activeGroupCount() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        int n = ngroupsSnapshot;
        for (int i = 0; i < ngroupsSnapshot; i++) {
            n += groupsSnapshot[i].activeGroupCount();
        }
        return n;
    }

    /**
     * Copies into the specified array references to every active
     * subgroup in this thread group.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * An application might use the <code>activeGroupCount</code> method to
     * get an estimate of how big the array should be, however <i>if the
     * array is too short to hold all the thread groups, the extra thread
     * groups are silently ignored.</i>  If it is critical to obtain every
     * active subgroup in this thread group, the caller should verify that
     * the returned int value is strictly less than the length of
     * <tt>list</tt>.
     * <p>
     * Due to the inherent race condition in this method, it is recommended
     * that the method only be used for informational purposes.
     *
     * @param list an array into which to place the list of thread groups.
     * @return the number of thread groups put into the array.
     * @throws SecurityException if the current thread does not
     *                           have permission to enumerate this thread group.
     * @see ThreadGroup#activeGroupCount()
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public int enumerate(ThreadGroup list[]) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    /**
     * Copies into the specified array references to every active
     * subgroup in this thread group. If the <code>recurse</code> flag is
     * <code>true</code>, references to all active subgroups of the
     * subgroups and so forth are also included.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * An application might use the <code>activeGroupCount</code> method to
     * get an estimate of how big the array should be, however <i>if the
     * array is too short to hold all the thread groups, the extra thread
     * groups are silently ignored.</i>  If it is critical to obtain every
     * active subgroup in this thread group, the caller should verify that
     * the returned int value is strictly less than the length of
     * <tt>list</tt>.
     * <p>
     * Due to the inherent race condition in this method, it is recommended
     * that the method only be used for informational purposes.
     *
     * @param list    an array into which to place the list of threads.
     * @param recurse a flag indicating whether to recursively enumerate
     *                all included thread groups.
     * @return the number of thread groups put into the array.
     * @throws SecurityException if the current thread does not
     *                           have permission to enumerate this thread group.
     * @see ThreadGroup#activeGroupCount()
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public int enumerate(ThreadGroup list[], boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    private int enumerate(ThreadGroup list[], int n, boolean recurse) {
        int ngroupsSnapshot = 0;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            int ng = ngroups;
            if (ng > list.length - n) {
                ng = list.length - n;
            }
            if (ng > 0) {
                System.arraycopy(groups, 0, list, n, ng);
                n += ng;
            }
            if (recurse) {
                ngroupsSnapshot = ngroups;
                if (groups != null) {
                    groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
                } else {
                    groupsSnapshot = null;
                }
            }
        }
        if (recurse) {
            for (int i = 0; i < ngroupsSnapshot; i++) {
                n = groupsSnapshot[i].enumerate(list, n, true);
            }
        }
        return n;
    }

    @Deprecated
    public final void stop() {
    }

    /**
     * Interrupts all threads in this thread group.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * This method then calls the <code>interrupt</code> method on all the
     * threads in this thread group and in all of its subgroups.
     *
     * @throws SecurityException if the current thread is not allowed
     *                           to access this thread group or any of the threads in
     *                           the thread group.
     * @see Thread#interrupt()
     * @see SecurityException
     * @see ThreadGroup#checkAccess()
     * @since 1.2
     */
    public final void interrupt() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();

            // Count only alive threads
            for (int i = 0; i < nthreads; i++) {
                Thread t = threads[i].get();
                if (t != null) {
                    t.interrupt();
                }
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].interrupt();
        }
    }

    @Deprecated
    public final void suspend() {
    }


    @Deprecated
    public final void resume() {
    }

    /**
     * Destroys this thread group and all of its subgroups. This thread
     * group must be empty, indicating that all threads that had been in
     * this thread group have since stopped.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     *
     * @throws IllegalThreadStateException if the thread group is not
     *                                     empty or if the thread group has already been destroyed.
     * @throws SecurityException           if the current thread cannot modify this
     *                                     thread group.
     * @see ThreadGroup#checkAccess()
     * @since JDK1.0
     */
    public final void destroy() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();

            if (destroyed || (nthreads > 0)) {
                throw new IllegalThreadStateException();
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
            if (parent != null) {
                destroyed = true;
                ngroups = 0;
                groups = null;
                nthreads = 0;
                threads = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i += 1) {
            groupsSnapshot[i].destroy();
        }
        if (parent != null) {
            parent.remove(this);
        }
    }

    /**
     * Adds the specified Thread group to this group.
     *
     * @param g the specified Thread group to be added
     * @throws IllegalThreadStateException If the Thread group has been destroyed.
     */
    private final void add(ThreadGroup g) {
        synchronized (this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }
            if (groups == null) {
                groups = new ThreadGroup[4];
            } else if (ngroups == groups.length) {
                groups = Arrays.copyOf(groups, ngroups * 2);
            }
            groups[ngroups] = g;

            // This is done last so it doesn't matter in case the
            // thread is killed
            ngroups++;
        }
    }

    /**
     * Removes the specified Thread group from this group.
     *
     * @param g the Thread group to be removed
     * @return if this Thread has already been destroyed.
     */
    private void remove(ThreadGroup g) {
        synchronized (this) {
            if (destroyed) {
                return;
            }
            for (int i = 0; i < ngroups; i++) {
                if (groups[i] == g) {
                    ngroups -= 1;
                    System.arraycopy(groups, i + 1, groups, i, ngroups - i);
                    // Zap dangling reference to the dead group so that
                    // the garbage collector will collect it.
                    groups[ngroups] = null;
                    break;
                }
            }
            if (nthreads == 0) {
                notifyAll();
            }
            if (daemon && (nthreads == 0) &&
                    (nUnstartedThreads == 0) && (ngroups == 0)) {
                destroy();
            }
        }
    }


    /**
     * Increments the count of unstarted threads in the thread group.
     * Unstarted threads are not added to the thread group so that they
     * can be collected if they are never started, but they must be
     * counted so that daemon thread groups with unstarted threads in
     * them are not destroyed.
     */
    void addUnstarted() {
        synchronized (this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }
            nUnstartedThreads++;
        }
    }

    /**
     * Adds the specified Thread to this group.
     *
     * @param t the Thread to be added
     * @throws IllegalThreadStateException If the Thread group has been destroyed.
     */
    void add(Thread t) {
        synchronized (this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }

            // Clean up any dead thread references first
            cleanupDeadThreads();

            if (threads == null) {
                threads = new WeakReference[4];
            } else if (nthreads == threads.length) {
                threads = Arrays.copyOf(threads, nthreads * 2);
            }
            //if the thread is already exist, remove it
            for (int i = 0; i < nthreads; i++) {
                if (threads[i].get() == t) {
                    //System.out.println("thread already exist" + t.getName());
                    System.arraycopy(threads, i + 1, threads, i, --nthreads - i);
                    threads[nthreads] = null;
                    break;
                }
            }
            threads[nthreads] = new WeakReference<Thread>(t);

            // This is done last so it doesn't matter in case the
            // thread is killed
            nthreads++;
            nUnstartedThreads--;
        }
    }

    /**
     * Removes the specified Thread from this group.
     *
     * @param t the Thread to be removed
     * @return if the Thread has already been destroyed.
     */
    void remove(Thread t) {
        synchronized (this) {
            if (destroyed) {
                return;
            }

            // Clean up any dead thread references first
            cleanupDeadThreads();

            for (int i = 0; i < nthreads; i++) {
                if (threads[i].get() == t) {
                    System.arraycopy(threads, i + 1, threads, i, --nthreads - i);
                    // Zap dangling reference to the dead thread so that
                    // the garbage collector will collect it.
                    threads[nthreads] = null;
                    break;
                }
            }
            if (nthreads == 0) {
                notifyAll();
            }
            if (daemon && (nthreads == 0) &&
                    (nUnstartedThreads == 0) && (ngroups == 0)) {
                destroy();
            }
        }
    }

    /**
     * Cleans up dead thread references.
     * This method should be called in add() and remove() methods to clean up
     * threads that were created but never started and have been garbage collected.
     */
    private void cleanupDeadThreads() {
        // Use reverse iteration to safely remove elements
        for (int i = nthreads - 1; i >= 0; i--) {
            if (threads[i].get() == null) {
                System.arraycopy(threads, i + 1, threads, i, nthreads - i - 1);
                threads[--nthreads] = null;
            }
        }
    }

    /**
     * Prints information about this thread group to the standard
     * output. This method is useful only for debugging.
     *
     * @since JDK1.0
     */
    public void list() {
        list(System.out, 0);
    }

    void list(PrintStream out, int indent) {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            for (int j = 0; j < indent; j++) {
                out.print(" ");
            }
            out.println(this);
            indent += 4;
            for (int i = 0; i < nthreads; i++) {
                Thread t = threads[i].get();
                if (t != null) {
                    for (int j = 0; j < indent; j++) {
                        out.print(" ");
                    }
                    out.println(t);
                }
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].list(out, indent);
        }
    }

    /**
     * Called by the Java Virtual Machine when a thread in this
     * thread group stops because of an uncaught exception, and the thread
     * does not have a specific {@link Thread.UncaughtExceptionHandler}
     * installed.
     * <p>
     * The <code>uncaughtException</code> method of
     * <code>ThreadGroup</code> does the following:
     * <ul>
     * <li>If this thread group has a parent thread group, the
     *     <code>uncaughtException</code> method of that parent is called
     *     with the same two arguments.
     * <li>Otherwise, this method checks to see if there is a
     *     {@linkplain Thread#getDefaultUncaughtExceptionHandler default
     *     uncaught exception handler} installed, and if so, its
     *     <code>uncaughtException</code> method is called with the same
     *     two arguments.
     * <li>Otherwise, this method determines if the <code>Throwable</code>
     *     argument is an instance of {@link ThreadDeath}. If so, nothing
     *     special is done. Otherwise, a message containing the
     *     thread's name, as returned from the thread's {@link
     *     Thread#getName getName} method, and a stack backtrace,
     *     using the <code>Throwable</code>'s {@link
     *     Throwable#printStackTrace printStackTrace} method, is
     *     printed to the {@linkplain System#err standard error stream}.
     * </ul>
     * <p>
     * Applications can override this method in subclasses of
     * <code>ThreadGroup</code> to provide alternative handling of
     * uncaught exceptions.
     *
     * @param t the thread that is about to exit.
     * @param e the uncaught exception.
     * @since JDK1.0
     */
    public void uncaughtException(Thread t, Throwable e) {
        if (parent != null) {
            parent.uncaughtException(t, e);
        } else {
            Thread.UncaughtExceptionHandler ueh =
                    Thread.getDefaultUncaughtExceptionHandler();
            if (ueh != null) {
                ueh.uncaughtException(t, e);
            } else if (!(e instanceof ThreadDeath)) {
                System.err.print("Exception in thread \""
                        + t.getName() + "\" ");
                e.printStackTrace();
            }
        }
    }

    /**
     * Used by VM to control lowmem implicit suspension.
     *
     * @param b boolean to allow or disallow suspension
     * @return true on success
     * @since JDK1.1
     * @deprecated The definition of this call depends on {@link #suspend},
     * which is deprecated.  Further, the behavior of this call
     * was never specified.
     */
    @Deprecated
    public boolean allowThreadSuspension(boolean b) {
        return true;
    }

    /**
     * Returns a string representation of this Thread group.
     *
     * @return a string representation of this thread group.
     * @since JDK1.0
     */
    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",maxpri=" + maxPriority + "]";
    }
}
