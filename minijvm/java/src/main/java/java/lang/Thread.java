/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

import org.mini.vm.RefNative;
import org.mini.vm.ThreadLifeHandler;
import sun.reflect.Reflection;

/**
 * A <i>thread</i> is a thread of execution in a program. The Java Virtual
 * Machine allows an application to have multiple threads of execution running
 * concurrently.
 * <p>
 * Every thread has a priority. Threads with higher priority are executed in
 * preference to threads with lower priority.
 * <p>
 * There are two ways to create a new thread of execution. One is to declare a
 * class to be a subclass of <code>Thread</code>. This subclass should override
 * the <code>run</code> method of class <code>Thread</code>. An instance of the
 * subclass can then be allocated and started. For example, a thread that
 * computes primes larger than a stated value could be written as follows:
 * <p>
 * <hr><blockquote><pre>
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <p>
 * <blockquote><pre>
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * </pre></blockquote>
 * <p>
 * The other way to create a thread is to declare a class that implements the
 * <code>Runnable</code> interface. That class then implements the
 * <code>run</code> method. An instance of the class can then be allocated,
 * passed as an argument when creating <code>Thread</code>, and started. The
 * same example in this other style looks like the following:
 * <p>
 * <hr><blockquote><pre>
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <p>
 * <blockquote><pre>
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * </pre></blockquote>
 * <p>
 *
 * @author unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see java.lang.Runnable
 * @see java.lang.Runtime#exit(int)
 * @see java.lang.Thread#run()
 * @since JDK1.0, CLDC 1.0
 */
public class Thread implements Runnable {

    /* Thread priority */
    private int priority = NORM_PRIORITY;

    /* Internal thread queue */
    private Thread threadQ;

    /* What will be run. */
    private Runnable target;

    /* Thread name */
    private char name[];

    /*  save top runtime   */
    private long stackFrame;// = createStackFrame();
    /**
     * The minimum priority that a thread can have.
     */
    public final static int MIN_PRIORITY = 1;

    /**
     * The default priority that is assigned to a thread.
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * The maximum priority that a thread can have.
     */
    public final static int MAX_PRIORITY = 10;

    ThreadLocal.ThreadLocalMap threadLocals;

    /* Whether or not the thread is a daemon thread. */
    private boolean daemon = false;

    /* The group of this thread */
    private ThreadGroup group;

    private native long createStackFrame();

    /**
     * The argument supplied to the current call to
     * java.util.concurrent.locks.LockSupport.park.
     * Set by (private) java.util.concurrent.locks.LockSupport.setBlocker
     * Accessed using java.util.concurrent.locks.LockSupport.getBlocker
     */
    volatile Object parkBlocker;

    /**
     * Returns a reference to the currently executing <code>Thread</code>
     * object.
     *
     * @return the currently executing thread.
     */
    public static native Thread currentThread();

    /* For autonumbering anonymous threads. */
    private static int threadInitNumber;

    private static synchronized int nextThreadNum() {
        return ++threadInitNumber;
    }

    /**
     * Causes the currently executing thread object to temporarily pause and
     * allow other threads to execute.
     */
    public static native void yield();

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds. The thread does not
     * lose ownership of any monitors.
     *
     * @param millis the length of time to sleep in milliseconds.
     * @throws InterruptedException if another thread has interrupted the
     *                              current thread. The <i>interrupted status</i> of the current thread is
     *                              cleared when this exception is thrown.
     * @see java.lang.Object#notify()
     */
    public static native void sleep(long millis) throws InterruptedException;

    public static void sleep(long millis, int nanos)
            throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        sleep(millis);
    }

    /**
     * Initialize a Thread.
     *
     * @param target the object whose run() method gets called
     * @param name   the name of the new thread
     */
    private void init(Runnable target, String name) {
        init(null, target, name);
    }

    private void init(ThreadGroup group, Runnable target, String name) {
        Thread parent = currentThread();

        if (group == null) {
            ThreadGroup pg = parent.getThreadGroup();
            if (pg == null) {
                ThreadGroup systemThreadGroup = new ThreadGroup();
                parent.group = new ThreadGroup(systemThreadGroup, "main");
                parent.group.add(parent);
                this.group = parent.group;
            } else {
                this.group = pg;
            }
        } else {
            this.group = group;
        }
        this.group.add(this);

        this.target = target;
        this.name = name.toCharArray();
        this.priority = parent.getPriority();
        setPriority0(priority);
        try {
            if (lifeHandler != null) {
                lifeHandler.threadCreated(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Allocates a new <code>Thread</code> object.
     * <p>
     * Threads created this way must have overridden their <code>run()</code>
     * method to actually do anything.
     *
     * @see java.lang.Runnable
     */
    public Thread() {
        init(null, "Thread-" + nextThreadNum());
    }

    /**
     * Allocates a new <code>Thread</code> object with the given name.
     * <p>
     * Threads created this way must have overridden their <code>run()</code>
     * method to actually do anything.
     *
     * @param name the name of the new thread.
     */
    public Thread(String name) {
        init(null, name);
    }

    /**
     * Allocates a new <code>Thread</code> object with a specific target object
     * whose <code>run</code> method is called.
     *
     * @param target the object whose <code>run</code> method is called.
     */
    public Thread(Runnable target) {
        init(target, "Thread-" + nextThreadNum());
    }

    public Thread(ThreadGroup group, Runnable target) {
        init(group, target, "Thread-" + nextThreadNum());
    }

    /**
     * Allocates a new <code>Thread</code> object with the given target and
     * name.
     *
     * @param target the object whose <code>run</code> method is called.
     * @param name   the name of the new thread.
     */
    public Thread(Runnable target, String name) {
        init(target, name);
    }

    public Thread(ThreadGroup group, Runnable target, String name) {
        init(group, target, name);
    }

    /**
     * Causes this thread to begin execution; the Java Virtual Machine calls the
     * <code>run</code> method of this thread.
     * <p>
     * The result is that two threads are running concurrently: the current
     * thread (which returns from the call to the <code>start</code> method) and
     * the other thread (which executes its <code>run</code> method).
     *
     * @throws IllegalThreadStateException if the thread was already started.
     * @see java.lang.Thread#run()
     */
    public synchronized native void start();

    /**
     * If this thread was constructed using a separate <code>Runnable</code> run
     * object, then that <code>Runnable</code> object's <code>run</code> method
     * is called; otherwise, this method does nothing and returns.
     * <p>
     * Subclasses of <code>Thread</code> should override this method.
     *
     * @see java.lang.Thread#start()
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    @Deprecated
    public void stop() {
        RefNative.stopThread(this, null);
    }

    @Deprecated
    public void stop(Throwable obj) {
        RefNative.stopThread(this, obj);
    }

    @Deprecated
    public void suspend() {
        RefNative.suspendThread(this);
    }

    @Deprecated
    public void resume() {
        RefNative.resumeThread(this);
    }

    /**
     * Interrupts this thread. In an implementation conforming to the CLDC
     * Specification, this operation is not required to cancel or clean up any
     * pending I/O operations that the thread may be waiting for.
     *
     * @since JDK 1.0, CLDC 1.1
     */
    public void interrupt() {
        interrupt0(this);
    }


    static public boolean interrupted() {
        return interrupted0(Thread.currentThread());
    }

    static public native boolean interrupted0(Thread t);

    public boolean isInterrupted() {
        return interrupted0(this);
    }

    /**
     * Tests if this thread is alive. A thread is alive if it has been started
     * and has not yet died.
     *
     * @return <code>true</code> if this thread is alive; <code>false</code>
     * otherwise.
     */
    public final native boolean isAlive();

    /**
     * Changes the priority of this thread.
     *
     * @param newPriority priority to set this thread to
     * @throws IllegalArgumentException If the priority is not in the range
     *                                  <code>MIN_PRIORITY</code> to <code>MAX_PRIORITY</code>.
     * @see java.lang.Thread#getPriority()
     * @see java.lang.Thread#MAX_PRIORITY
     * @see java.lang.Thread#MIN_PRIORITY
     */
    public final void setPriority(int newPriority) {
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        setPriority0(priority = newPriority);
    }

    /**
     * Returns this thread's priority.
     *
     * @return this thread's priority.
     * @see java.lang.Thread#setPriority(int)
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * Returns this thread's name. Note that in CLDC the name of the thread can
     * only be set when creating the thread.
     *
     * @return this thread's name.
     */
    public final String getName() {
        return String.valueOf(name);
    }

    /**
     * setup thread name
     *
     * @param tname
     */
    public final void setName(String tname) {
        if (tname != null) {
            name = tname.toCharArray();
        }
    }

    /**
     * Returns the current number of active threads in the virtual machine.
     *
     * @return the current number of active threads.
     */
    public static native int activeCount();

    /**
     * Waits for this thread to die.
     *
     * @throws InterruptedException if another thread has interrupted the
     *                              current thread. The <i>interrupted status</i> of the current thread is
     *                              cleared when this exception is thrown.
     */
    public synchronized final void join() throws InterruptedException {
        while (isAlive()) {
            wait(100);
        }
    }

    public final synchronized void join(long millis)
            throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (millis == 0) {
            while (isAlive()) {
                wait(0);
            }
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }

    public final synchronized void join(long millis, int nanos)
            throws InterruptedException {

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        join(millis);
    }

    /**
     * Marks this thread as either a {@linkplain #isDaemon daemon} thread or a
     * user thread. The Java Virtual Machine exits when the only threads running
     * are all daemon threads.
     *
     * <p>
     * This method must be invoked before the thread is started.
     *
     * @param on if {@code true}, marks this thread as a daemon thread
     * @throws IllegalThreadStateException if this thread is
     *                                     {@linkplain #isAlive alive}
     * @throws SecurityException           determines that the
     *                                     current thread cannot modify this thread
     */
    public final void setDaemon(boolean on) {
        if (isAlive()) {
            throw new IllegalThreadStateException();
        }
        daemon = on;
    }

    /**
     * Tests if this thread is a daemon thread.
     *
     * @return <code>true</code> if this thread is a daemon thread;
     * <code>false</code> otherwise.
     * @see #setDaemon(boolean)
     */
    public final boolean isDaemon() {
        return daemon;
    }


    static RuntimePermission SET_CONTEXT_CLASS_LOADER_PERMISSION = new RuntimePermission("setContextClassLoader");
    static RuntimePermission GET_CONTEXT_CLASS_LOADER_PERMISSION = new RuntimePermission("getContextClassLoader");

    public void setContextClassLoader(ClassLoader cl) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SET_CONTEXT_CLASS_LOADER_PERMISSION);
        }
        setContextClassLoader0(cl);
    }

    public ClassLoader getContextClassLoader() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GET_CONTEXT_CLASS_LOADER_PERMISSION);
        }
        return getContextClassLoader0();
    }

    /**
     * Returns the identifier of this Thread.  The thread ID is a positive
     * <tt>long</tt> number generated when this thread was created.
     * The thread ID is unique and remains unchanged during its lifetime.
     * When a thread is terminated, this thread ID may be reused.
     *
     * @return this thread's ID.
     * @since 1.5
     */
    public long getId() {
        return stackFrame;
    }

    /**
     * This method is called by the system to give a Thread
     * a chance to clean up before it actually exits.
     */
    private void exit() {
        try {
            if (lifeHandler != null) {
                lifeHandler.threadDestroy(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (group != null) {
            group.remove(this);
            group = null;
        }
        /* Aggressively null out all reference fields: see bug 4006245 */
        target = null;
        /* Speed the release of some of these resources */
        threadLocals = null;
        uncaughtExceptionHandler = null;
    }

    /**
     * Returns a string representation of this thread, including the thread's
     * name and priority.
     *
     * @return a string representation of this thread.
     */
    public String toString() {
        return "Thread[" + getName() + "," + getPriority() + "," + (group == null ? "" : group.getName()) + "]";
    }

    private native void setContextClassLoader0(ClassLoader cl);

    private native ClassLoader getContextClassLoader0();

    /* Some private helper methods */
    private native void setPriority0(int newPriority);

    static private native void interrupt0(Thread t);

    //Returns an array of stack trace elements representing the stack dump of this thread.
    public StackTraceElement[] getStackTrace() {
        return Throwable.getStackTrace(Throwable.buildStackElement(this));
    }

    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }


    // Added in JSR-166
    public interface UncaughtExceptionHandler {
        void uncaughtException(Thread t, Throwable e);
    }

    // null unless explicitly set
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    private static ThreadLifeHandler lifeHandler;

    // null unless explicitly set
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        defaultUncaughtExceptionHandler = eh;
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler != null ?
                uncaughtExceptionHandler : group;
    }

    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        uncaughtExceptionHandler = eh;
    }

    /**
     * Dispatch an uncaught exception to the handler. This method is
     * intended to be called only by the JVM.
     */
    private void dispatchUncaughtException(Throwable e) {
        getUncaughtExceptionHandler().uncaughtException(this, e);
    }


    private static void checkCaller() {
        Throwable throwable = new Throwable();
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements.length < 3 || !stackTraceElements[2].getClassName().equals("org.mini.vm.VmUtil")) {
            throw new SecurityException("setThreadCreateHandler must be called by VmUtil, Please add handler from VmUtil");
        }
    }

    public static void setThreadLifeHandler(ThreadLifeHandler r) {
        //if caller is not VmUtil.class, throw SecurityException
        checkCaller();
        lifeHandler = r;
    }

    public static ThreadLifeHandler getThreadLifeHandler() {
        checkCaller();
        return lifeHandler;
    }

    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED;
    }

    public State getState() {
        // get current thread state
        int state = RefNative.getStatus(this);
        switch (state) {
            case 0:
                return State.NEW;
            case 1:
                return State.RUNNABLE;
            case 2:
                return State.BLOCKED;
            case 3:
                return State.WAITING;
            case 4:
                return State.WAITING;
            default:
                return State.TERMINATED;
        }
    }
}
