/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are licensed under
 * GNU General Public License, version 2, with the Classpath Exception
 * 
 * http://openjdk.java.net/legal/gplv2+ce.html
 */
/**
 * <h1>Extended Stack-Trace</h1>
 * <h2>Enhanced stack traces for the JVM</h2>
 * 
 * This library allows you to capture stack traces with more information than {@code Throwable.getStackTrace}. 
 * In addition to the information in the JDK's {@link java.lang.StackTraceElement} The captured stack elements contain:
 * <p>
 * <ul>
 * <li>The decalaring class -- the actual {@link java.lang.Class} object -- not just the name.</li>
 * <li>The method -- the actual {@link java.lang.reflect.Method Method} object -- not just the method name.</li>
 * <li>The bytecode index</li>
 * </ul>
 * <p>
 * Works best when running on a Java 8 HotSpot (OpenJDK/Oracle JDK) JVM.
 * 
 * <h2>Usage</h2>
 * Obtain the extended stack trace information with {@link co.paralleluniverse.xst.ExtendedStackTrace#here()} or {@link co.paralleluniverse.xst.ExtendedStackTrace#of(Throwable)}
 * 
 * <h2>Details</h2>
 * On the HotSpot JVM (OpenJDK/Oracle JDK) for Java 8, the extended information is always available, and obtaining it is as efficient as a plain {@link Throwable#getStackTrace()}.
 *
 * On other JVMs/Java versions the extended information may be incomplete. There are (much) better chances for obtaining extended information when capturing the stack with 
 * {@link co.paralleluniverse.xst.ExtendedStackTrace#here()} than when extracting extended information from a {@code Throwable} with {@link co.paralleluniverse.xst.ExtendedStackTrace#of(Throwable)}.
 * Also, getting the method object carries a significant cost.
 *
 * <h2>License</h2>
 * This project is free software free software published under the following license:
 * <p>
 * <pre>
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are licensed under
 * GNU General Public License, version 2, with the Classpath Exception (same as OpenJDK)
 * 
 * http://openjdk.java.net/legal/gplv2+ce.html
 * </pre>
 */
package co.paralleluniverse.xst;
