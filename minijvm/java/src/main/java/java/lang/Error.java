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

/**
 * An <code>Error</code> is a subclass of <code>Throwable</code>
 * that indicates serious problems that a reasonable application
 * should not try to catch. Most such errors are abnormal conditions.
 * <p>
 * A method is not required to declare in its <code>throws</code>
 * clause any subclasses of <code>Error</code> that might be thrown
 * during the execution of the method but not caught, since these
 * errors are abnormal conditions that should never occur.
 *
 * @author  Frank Yellin
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.0, CLDC 1.0
 */
public class Error extends Throwable {
  public Error(String message, Throwable cause) {
    super(message, cause);
  }

  public Error(String message) {
    this(message, null);
  }

  public Error(Throwable cause) {
    this(null, cause);
  }

  public Error() {
    this(null, null);
  }
}
