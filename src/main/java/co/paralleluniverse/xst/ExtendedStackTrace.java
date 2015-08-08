/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are licensed under
 * GNU General Public License, version 2, with the Classpath Exception
 * 
 * http://openjdk.java.net/legal/gplv2+ce.html
 */
package co.paralleluniverse.xst;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
//import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Represents a captured stack trace which contains more information than that returned by {@link Throwable#getStackTrace()}.
 * The extended information is captured on a best-effort basis, and depends on the JVM used.
 *
 * @author pron
 */
public class ExtendedStackTrace implements Iterable<ExtendedStackTraceElement> {
    /**
     * Returns a stack trace with extended information for the given {@code Throwable}.
     * @param t
     */
    public static ExtendedStackTrace of(Throwable t) {
        if (t == null)
            return null;
        try {
            return new ExtendedStackTraceHotSpot(t);
        } catch (Throwable e) {
            return new ExtendedStackTrace(t);
        }
    }

    /**
     * Returns a stack trace for the current execution point.
     */
    public static ExtendedStackTrace here() {
        try {
            return new ExtendedStackTraceHotSpot(new Exception("Stack trace"));
        } catch (Throwable e) {
            return new ExtendedStackTraceClassContext();
        }
    }

    protected final Throwable t;
    private ExtendedStackTraceElement[] est;
//    private transient Map<Class<?>, Member[]> methods; // cache

    protected ExtendedStackTrace(Throwable t) {
        this.t = t;
    }

    @Override
    public Iterator<ExtendedStackTraceElement> iterator() {
        return Arrays.asList(get()).iterator();
    }

    /**
     * Returns an array of {@link ExtendedStackTraceElement}s representing the captured stack trace.
     */
    public ExtendedStackTraceElement[] get() {
        synchronized (this) {
            if (est == null) {
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    est = new ExtendedStackTraceElement[st.length];
                    for (int i = 0; i < st.length; i++)
                        est[i] = new BasicExtendedStackTraceElement(st[i]);
                }
            }
            return est;
        }
    }

    protected /*Executable*/ Member getMethod(final ExtendedStackTraceElement este) {
        if (este.getDeclaringClass() == null)
            return null;
        Member[] ms = getMethods(este.getDeclaringClass());
        Member method = null;

        for (Member m : ms) {
            if (este.getMethodName().equals(m.getName())) {
                if (method == null)
                    method = m;
                else {
                    method = null; // more than one match
                    break;
                }
            }
        }
        if (method == null && este.getLineNumber() >= 0) {
            try {
                final AtomicReference<String> descriptor = new AtomicReference<>();
                ASMUtil.accept(este.getDeclaringClass(), ClassReader.SKIP_FRAMES, new ClassVisitor(Opcodes.ASM5) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, final String desc, String signature, String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                        if (descriptor.get() == null && este.getMethodName().equals(name)) {
                            mv = new MethodVisitor(api, mv) {
                                int minLine = Integer.MAX_VALUE, maxLine = Integer.MIN_VALUE;

                                @Override
                                public void visitLineNumber(int line, Label start) {
                                    if (line < minLine)
                                        minLine = line;
                                    if (line > maxLine)
                                        maxLine = line;
                                }

                                @Override
                                public void visitEnd() {
                                    if (minLine <= este.getLineNumber() && maxLine >= este.getLineNumber())
                                        descriptor.set(desc);
                                    super.visitEnd();
                                }
                            };
                        }
                        return mv;
                    }
                });

                if (descriptor.get() != null) {
                    final String desc = descriptor.get();
                    for (Member m : ms) {
                        if (este.getMethodName().equals(getName(m)) && desc.equals(getDescriptor(m))) {
                            method = m;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return method;
    }

    protected static final String getName(Member m) {
        if (m instanceof Constructor)
            return "<init>";
        return ((Method) m).getName();
    }

    protected static final String getDescriptor(Member m) {
        if (m instanceof Constructor)
            return Type.getConstructorDescriptor((Constructor) m);
        return Type.getMethodDescriptor((Method) m);
    }

    protected final Member[] getMethods(Class<?> clazz) {
//        synchronized (this) {
        Member[] es;
//            if (methods == null)
//                methods = new HashMap<>();
//            es = methods.get(clazz);
//            if (es == null) {
        Method[] ms = clazz.getDeclaredMethods();
        Constructor[] cs = clazz.getDeclaredConstructors();
        es = new Member[ms.length + cs.length];
        System.arraycopy(cs, 0, es, 0, cs.length);
        System.arraycopy(ms, 0, es, cs.length, ms.length);

//                methods.put(clazz, es);
//            }
        return es;
//        }
    }

    protected class BasicExtendedStackTraceElement extends ExtendedStackTraceElement {
        protected BasicExtendedStackTraceElement(StackTraceElement ste, Class<?> clazz, Method method, int bci) {
            super(ste, clazz, method, bci);
        }

        protected BasicExtendedStackTraceElement(StackTraceElement ste, Class<?> clazz) {
            super(ste, clazz, null, -1);
        }

        protected BasicExtendedStackTraceElement(StackTraceElement ste) {
            super(ste, null, null, -1);
        }

        @Override
        public Member getMethod() {
            if (method == null) {
                method = ExtendedStackTrace.this.getMethod(this);
                if (method != null && !getMethodName().equals(getName(method))) {
                    throw new IllegalStateException("Method name mismatch: " + getMethodName() + ", " + method.getName());
                    // method = null;
                }
            }
            return method;
        }

        @Override
        public Class<?> getDeclaringClass() {
            if (clazz == null) {
                try {
                    clazz = Class.forName(getClassName());
                } catch (ClassNotFoundException e) {
                }
            }
            return clazz;
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Printing">
    /////////// Printing ///////////////////////////////////
    /*
     * Additional copyright for the printing section, 
     * which is based on OpenJDK code taken from java/lang/Throwable.java:
     */
    /*
     * Copyright (c) 1994, 2011, Oracle and/or its affiliates. All rights reserved.
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
    /**
     * Prints this stack trace to {@link System#err}.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints this stack trace to the given print stream.
     */
    public void printStackTrace(PrintStream s) {
        printStackTrace(new WrappedPrintStream(s));
    }

    /**
     * Prints this stack trace to the given print writer.
     */
    public void printStackTrace(PrintWriter s) {
        printStackTrace(new WrappedPrintWriter(s));
    }

    private void printStackTrace(PrintStreamOrWriter s) {
        synchronized (s.lock()) {
            // Guard against malicious overrides of Throwable.equals by using a Set with identity equality semantics.
            printStackTrace(s, null, "", "", Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>()));
        }
    }

    private void printStackTrace(PrintStreamOrWriter s,
                                 ExtendedStackTraceElement[] enclosingTrace,
                                 String caption,
                                 String prefix,
                                 Set<Throwable> dejaVu) {
        assert Thread.holdsLock(s.lock());
        if (dejaVu.contains(t)) {
            s.println("\t[CIRCULAR REFERENCE:" + this + "]");
        } else {
            dejaVu.add(t);

            final ExtendedStackTraceElement[] trace = get();
            final int unique = countUniqueFrames(trace, enclosingTrace);

            // Print our stack trace
            s.println(prefix + caption + this);
            for (int i = 0; i < unique; i++)
                s.println(prefix + "\tat " + trace[i]);
            
            final int framesInCommon = trace.length - unique;
            if (framesInCommon != 0)
                s.println(prefix + "\t... " + framesInCommon + " more");

            // Print suppressed exceptions, if any
            for (Throwable se : t.getSuppressed())
                ExtendedStackTrace.of(se).printStackTrace(s, trace, SUPPRESSED_CAPTION, prefix + "\t", dejaVu);

            // Print cause, if any
            final ExtendedStackTrace ourCause = ExtendedStackTrace.of(t.getCause());
            if (ourCause != null)
                ourCause.printStackTrace(s, trace, CAUSE_CAPTION, prefix, dejaVu);
        }
    }

    private static int countUniqueFrames(ExtendedStackTraceElement[] trace, ExtendedStackTraceElement[] enclosingTrace) {
        int m = trace.length - 1;
        if (enclosingTrace != null) {
            int n = enclosingTrace.length - 1;
            while (m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n])) {
                m--;
                n--;
            }
        }
        return m + 1;
    }

    private static final String CAUSE_CAPTION = "Caused by: ";
    private static final String SUPPRESSED_CAPTION = "Suppressed: ";

    /**
     * Wrapper class for PrintStream and PrintWriter to enable a single implementation of printStackTrace.
     */
    private abstract static class PrintStreamOrWriter {
        abstract Object lock();

        abstract void println(Object o);
    }

    private static class WrappedPrintStream extends PrintStreamOrWriter {
        private final PrintStream printStream;

        WrappedPrintStream(PrintStream printStream) {
            this.printStream = printStream;
        }

        Object lock() {
            return printStream;
        }

        void println(Object o) {
            printStream.println(o);
        }
    }

    private static class WrappedPrintWriter extends PrintStreamOrWriter {
        private final PrintWriter printWriter;

        WrappedPrintWriter(PrintWriter printWriter) {
            this.printWriter = printWriter;
        }

        Object lock() {
            return printWriter;
        }

        void println(Object o) {
            printWriter.println(o);
        }
    }
    //</editor-fold>
}
