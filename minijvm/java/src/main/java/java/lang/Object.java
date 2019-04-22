/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package java.lang;

/**
 * Class <code>Object</code> is the root of the class hierarchy.
 * Every class has <code>Object</code> as a superclass. All objects,
 * including arrays, implement the methods of this class.
 *
 * @author  unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see     java.lang.Class
 * @since   JDK1.0, CLDC 1.0
 */
public class Object {

    /**
     * Returns the runtime class of an object. That <tt>Class</tt>
     * object is the object that is locked by <tt>static synchronized</tt>
     * methods of the represented class.
     *
     * @return  the object of type <code>Class</code> that represents the
     *          runtime class of the object.
     */
    public final native Class getClass();

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * <p>
     * The general contract of <code>hashCode</code> is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     *     an execution of a Java application, the <tt>hashCode</tt> method
     *     must consistently return the same integer, provided no information
     *     used in <tt>equals</tt> comparisons on the object is modified.
     *     This integer need not remain consistent from one execution of an
     *     application to another execution of the same application.
     * <li>If two objects are equal according to the <tt>equals(Object)</tt>
     *     method, then calling the <code>hashCode</code> method on each of
     *     the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     *     according to the {@link java.lang.Object#equals(java.lang.Object)}
     *     method, then calling the <tt>hashCode</tt> method on each of the
     *     two objects must produce distinct integer results.  However, the
     *     programmer should be aware that producing distinct integer results
     *     for unequal objects may improve the performance of hashtables.
     * </ul>
     * <p>
     * As much as is reasonably practical, the hashCode method defined by
     * class <tt>Object</tt> does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java<font size="-2"><sup>TM</sup></font> programming language.)
     *
     * @return  a hash code value for this object.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable
     */
    public native int hashCode();

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The <code>equals</code> method implements an equivalence relation:
     * <ul>
     * <li>It is <i>reflexive</i>: for any reference value <code>x</code>,
     *     <code>x.equals(x)</code> should return <code>true</code>.
     * <li>It is <i>symmetric</i>: for any reference values <code>x</code> and
     *     <code>y</code>, <code>x.equals(y)</code> should return
     *     <code>true</code> if and only if <code>y.equals(x)</code> returns
     *     <code>true</code>.
     * <li>It is <i>transitive</i>: for any reference values <code>x</code>,
     *     <code>y</code>, and <code>z</code>, if <code>x.equals(y)</code>
     *     returns  <code>true</code> and <code>y.equals(z)</code> returns
     *     <code>true</code>, then <code>x.equals(z)</code> should return
     *     <code>true</code>.
     * <li>It is <i>consistent</i>: for any reference values <code>x</code>
     *     and <code>y</code>, multiple invocations of <tt>x.equals(y)</tt>
     *     consistently return <code>true</code> or consistently return
     *     <code>false</code>, provided no information used in
     *     <code>equals</code> comparisons on the object is modified.
     * <li>For any non-null reference value <code>x</code>,
     *     <code>x.equals(null)</code> should return <code>false</code>.
     * </ul>
     * <p>
     * The <tt>equals</tt> method for class <code>Object</code> implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any reference values <code>x</code> and <code>y</code>,
     * this method returns <code>true</code> if and only if <code>x</code> and
     * <code>y</code> refer to the same object (<code>x==y</code> has the
     * value <code>true</code>).
     *
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     * @see     java.lang.Boolean#hashCode()
     * @see     java.util.Hashtable
     */
    public boolean equals(Object obj) {
        return (this == obj);
    }

    /**
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return  a string representation of the object.
     */
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * Wakes up a single thread that is waiting on this object's
     * monitor. If any threads are waiting on this object, one of them
     * is chosen to be awakened. The choice is arbitrary and occurs at
     * the discretion of the implementation. A thread waits on an object's
     * monitor by calling one of the <code>wait</code> methods.
     * <p>
     * The awakened thread will not be able to proceed until the current
     * thread relinquishes the lock on this object. The awakened thread will
     * compete in the usual manner with any other threads that might be
     * actively competing to synchronize on this object; for example, the
     * awakened thread enjoys no reliable privilege or disadvantage in being
     * the next thread to lock this object.
     * <p>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. A thread becomes the owner of the
     * object's monitor in one of three ways:
     * <ul>
     * <li>By executing a synchronized instance method of that object.
     * <li>By executing the body of a <code>synchronized</code> statement
     *     that synchronizes on the object.
     * <li>For objects of type <code>Class,</code> by executing a
     *     synchronized static method of that class.
     * </ul>
     * <p>
     * Only one thread at a time can own an object's monitor.
     *
     * @exception  IllegalMonitorStateException  if the current thread is not
     *               the owner of this object's monitor.
     * @see        java.lang.Object#notifyAll()
     * @see        java.lang.Object#wait()
     */
    public final native void notify();

    /**
     * Wakes up all threads that are waiting on this object's monitor. A
     * thread waits on an object's monitor by calling one of the
     * <code>wait</code> methods.
     * <p>
     * The awakened threads will not be able to proceed until the current
     * thread relinquishes the lock on this object. The awakened threads
     * will compete in the usual manner with any other threads that might
     * be actively competing to synchronize on this object; for example,
     * the awakened threads enjoy no reliable privilege or disadvantage in
     * being the next thread to lock this object.
     * <p>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. See the <code>notify</code> method for a
     * description of the ways in which a thread can become the owner of
     * a monitor.
     *
     * @exception  IllegalMonitorStateException  if the current thread is
     *             not the owner of this object's monitor.
     * @see        java.lang.Object#notify()
     * @see        java.lang.Object#wait()
     */
    public final native void notifyAll();

    /**
     * Causes current thread to wait until either another thread invokes the
     * {@link java.lang.Object#notify()} method or the
     * {@link java.lang.Object#notifyAll()} method for this object, or a
     * specified amount of time has elapsed.
     * <p>
     * The current thread must own this object's monitor.
     * <p>
     * This method causes the current thread (call it <var>T</var>) to
     * place itself in the wait set for this object and then to relinquish
     * any and all synchronization claims on this object. Thread <var>T</var>
     * becomes disabled for thread scheduling purposes and lies dormant
     * until one of four things happens:
     * <ul>
     * <li>Some other thread invokes the <tt>notify</tt> method for this
     * object and thread <var>T</var> happens to be arbitrarily chosen as
     * the thread to be awakened.
     * <li>Some other thread invokes the <tt>notifyAll</tt> method for this
     * object.
     * <li>Some other thread {@link java.lang.Thread#interrupt() interrupts}
     * thread <var>T</var>.
     * <li>The specified amount of real time has elapsed, more or less.  If
     * <tt>timeout</tt> is zero, however, then real time is not taken into
     * consideration and the thread simply waits until notified.
     * </ul><p>
     * The thread <var>T</var> is then removed from the wait set for this
     * object and re-enabled for thread scheduling. It then competes in the
     * usual manner with other threads for the right to synchronize on the
     * object; once it has gained control of the object, all its
     * synchronization claims on the object are restored to the status quo
     * ante - that is, to the situation as of the time that the <tt>wait</tt>
     * method was invoked. Thread <var>T</var> then returns from the
     * invocation of the <tt>wait</tt> method. Thus, on return from the
     * <tt>wait</tt> method, the synchronization state of the object and of
     * thread <var>T</var> is exactly as it was when the <tt>wait</tt> method
     * was invoked.
     * <p>
     * If the current thread is
     * {@link java.lang.Thread#interrupt() interrupted} by another thread
     * while it is waiting, then an <tt>InterruptedException</tt> is thrown.
     * This exception is not thrown until the lock status of this object has
     * been restored as described above.
     * <p>
     * Note that the <tt>wait</tt> method, as it places the current thread
     * into the wait set for this object, unlocks only this object; any
     * other objects on which the current thread may be synchronized remain
     * locked while the thread waits.
     * <p>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. See the <code>notify</code> method for a
     * description of the ways in which a thread can become the owner of
     * a monitor.
     *
     * @param      timeout   the maximum time to wait in milliseconds.
     * @exception  IllegalArgumentException      if the value of timeout is
     *               negative.
     * @exception  IllegalMonitorStateException  if the current thread is not
     *               the owner of the object's monitor.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     * @see        java.lang.Object#notify()
     * @see        java.lang.Object#notifyAll()
     */
    public final native void wait(long timeout) throws InterruptedException;

    /**
     * Causes current thread to wait until another thread invokes the
     * {@link java.lang.Object#notify()} method or the
     * {@link java.lang.Object#notifyAll()} method for this object, or
     * some other thread interrupts the current thread, or a certain
     * amount of real time has elapsed.
     * <p>
     * This method is similar to the <code>wait</code> method of one
     * argument, but it allows finer control over the amount of time to
     * wait for a notification before giving up. The amount of real time,
     * measured in nanoseconds, is given by:
     * <blockquote>
     * <pre>
     * 1000000*timeout+nanos</pre></blockquote>
     * <p>
     * In all other respects, this method does the same thing as the
     * method {@link #wait(long)} of one argument. In particular,
     * <tt>wait(0, 0)</tt> means the same thing as <tt>wait(0)</tt>.
     * <p>
     * The current thread must own this object's monitor. The thread
     * releases ownership of this monitor and waits until either of the
     * following two conditions has occurred:
     * <ul>
     * <li>Another thread notifies threads waiting on this object's monitor
     *     to wake up either through a call to the <code>notify</code> method
     *     or the <code>notifyAll</code> method.
     * <li>The timeout period, specified by <code>timeout</code>
     *     milliseconds plus <code>nanos</code> nanoseconds arguments, has
     *     elapsed.
     * </ul>
     * <p>
     * The thread then waits until it can re-obtain ownership of the
     * monitor and resumes execution
     * <p>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. See the <code>notify</code> method for a
     * description of the ways in which a thread can become the owner of
     * a monitor.
     *
     * @param      timeout   the maximum time to wait in milliseconds.
     * @param      nanos      additional time, in nanoseconds range
     *                       0-999999.
     * @exception  IllegalArgumentException      if the value of timeout is
     *                      negative or the value of nanos is
     *                      not in the range 0-999999.
     * @exception  IllegalMonitorStateException  if the current thread is not
     *               the owner of this object's monitor.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     */
    public final void wait(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && timeout == 0)) {
            timeout++;
        }

        wait(timeout);
    }

    /**
     * Causes current thread to wait until another thread invokes the
     * {@link java.lang.Object#notify()} method or the
     * {@link java.lang.Object#notifyAll()} method for this object.
     * In other word's this method behaves exactly as if it simply
     * performs the call <tt>wait(0)</tt>.
     * <p>
     * The current thread must own this object's monitor. The thread
     * releases ownership of this monitor and waits until another thread
     * notifies threads waiting on this object's monitor to wake up
     * either through a call to the <code>notify</code> method or the
     * <code>notifyAll</code> method. The thread then waits until it can
     * re-obtain ownership of the monitor and resumes execution.
     * <p>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. See the <code>notify</code> method for a
     * description of the ways in which a thread can become the owner of
     * a monitor.
     *
     * @exception  IllegalMonitorStateException  if the current thread is not
     *               the owner of the object's monitor.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     * @see        java.lang.Object#notify()
     * @see        java.lang.Object#notifyAll()
     */
    public final void wait() throws InterruptedException {
        wait(0);
    }

    protected native Object clone();
    
    protected void finalize() throws Throwable { }
}

