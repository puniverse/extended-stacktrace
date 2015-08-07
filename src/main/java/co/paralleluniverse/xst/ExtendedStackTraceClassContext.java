/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are licensed under
 * GNU General Public License, version 2, with the Classpath Exception
 * 
 * http://openjdk.java.net/legal/gplv2+ce.html
 */
package co.paralleluniverse.xst;

/**
 * @author pron
 */
class ExtendedStackTraceClassContext extends ExtendedStackTrace {
    private static final ClassContext classContextGenerator = new ClassContext();
    private ExtendedStackTraceElement[] est;
    private final Class[] classContext;

    ExtendedStackTraceClassContext() {
        super(new Throwable());
        this.classContext = classContextGenerator.getClassContext();

//        int i = 0;
//        for (Class c : classContext)
//            System.out.println("== " + i++ + " " + c.getName());
//        System.out.println("");
//        i = 0;
//        for (StackTraceElement e : t.getStackTrace())
//            System.out.println("-- " + i++ + " " + e);
    }

    @Override
    public ExtendedStackTraceElement[] get() {
        synchronized (this) {
            if (est == null) {
                final StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    est = new ExtendedStackTraceElement[st.length - 1];
                    for (int i = 1, k = 2; i < st.length; i++, k++) {
                        if (skipCTX(classContext[k]))
                            i--;
                        else {
                            final StackTraceElement ste = st[i];
                            final Class<?> clazz;
                            if (skipSTE(st[i])) {
                                k--;
                                clazz = null;
                            } else
                                clazz = classContext[k];
                            est[i - 1] = new BasicExtendedStackTraceElement(ste, clazz);
                            // System.out.println(">>>> " + k + ": " + (clazz != null ? clazz.getName() : null) + " :: " + i + ": " + ste);
                        }
                    }
                }
            }
            return est;
        }
    }

    static boolean skipSTE(StackTraceElement ste) {
        return (ste.getClassName().startsWith("sun.reflect")
                || ste.getClassName().equals("java.lang.reflect.Method")
                || ste.getClassName().startsWith("java.lang.invoke."));
    }

    private static boolean skipCTX(Class c) {
        return c.getName().startsWith("java.lang.invoke.");
    }

    private static class ClassContext extends SecurityManager {
        @Override
        public Class[] getClassContext() {
            return super.getClassContext();
        }
    }
}
