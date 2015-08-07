/*
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are licensed under
 * GNU General Public License, version 2, with the Classpath Exception
 * 
 * http://openjdk.java.net/legal/gplv2+ce.html
 */
package co.paralleluniverse.xst;

// import java.lang.reflect.Executable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * An element in a stack trace. Each element represents a single stack frame.
 *
 * Same as {@link StackTraceElement}, but with additional data.
 *
 * @author pron
 */
public class ExtendedStackTraceElement {
    private final String declaringClassName;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;
    private final int bci;
    Class<?> clazz;
    Member /*Executable*/ method;

    public ExtendedStackTraceElement(StackTraceElement ste) {
        this(ste, null, null, -1);
    }

    public ExtendedStackTraceElement(StackTraceElement ste, Class<?> clazz, Member method, int bci) {
        this(ste.getClassName(), ste.getMethodName(), ste.getFileName(), ste.getLineNumber(), clazz, method, bci);
    }

    public ExtendedStackTraceElement(Class<?> clazz, Method method, String fileName, int lineNumber, int bci) {
        this(clazz.getName(), method.getName(), fileName, lineNumber, clazz, method, bci);
    }

    public ExtendedStackTraceElement(String declaringClassName, String methodName, String fileName, int lineNumber, Class<?> clazz, Member method, int bci) {
        Objects.requireNonNull(declaringClassName, "Declaring class is null");
        Objects.requireNonNull(methodName, "Method name is null");
        if (clazz != null && !declaringClassName.equals(clazz.getName()))
            throw new IllegalArgumentException("Class name mismatch: " + declaringClassName + ", " + clazz.getName());
        if (method != null && !methodName.equals(method.getName()))
            throw new IllegalArgumentException("Method name mismatch: " + methodName + ", " + method.getName());
        this.declaringClassName = declaringClassName;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.clazz = clazz;
        this.bci = bci;
        this.method = method;
    }

    /**
     * Returns the name of the source file containing the execution point
     * represented by this stack trace element. Generally, this corresponds
     * to the {@code SourceFile} attribute of the relevant {@code class}
     * file (as per <i>The Java Virtual Machine Specification</i>, Section
     * 4.7.7). In some systems, the name may refer to some source code unit
     * other than a file, such as an entry in source repository.
     *
     * @return the name of the file containing the execution point
     *         represented by this stack trace element, or {@code null} if
     *         this information is unavailable.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the line number of the source line containing the execution
     * point represented by this stack trace element. Generally, this is
     * derived from the {@code LineNumberTable} attribute of the relevant
     * {@code class} file (as per <i>The Java Virtual Machine
     * Specification</i>, Section 4.7.8).
     *
     * @return the line number of the source line containing the execution
     *         point represented by this stack trace element, or a negative
     *         number if this information is unavailable.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the fully qualified name of the class containing the
     * execution point represented by this stack trace element.
     *
     * @return the fully qualified name of the {@code Class} containing
     *         the execution point represented by this stack trace element.
     */
    public String getClassName() {
        return declaringClassName;
    }

    /**
     * Returns the name of the method containing the execution point
     * represented by this stack trace element. If the execution point is
     * contained in an instance or class initializer, this method will return
     * the appropriate <i>special method name</i>, {@code <init>} or
     * {@code <clinit>}, as per Section 3.9 of <i>The Java Virtual
     * Machine Specification</i>.
     *
     * @return the name of the method containing the execution point
     *         represented by this stack trace element.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns true if the method containing the execution point
     * represented by this stack trace element is a native method.
     *
     * @return {@code true} if the method containing the execution point
     *         represented by this stack trace element is a native method.
     */
    public boolean isNativeMethod() {
        return lineNumber == -2;
    }

    /**
     * Returns the class containing the execution point represented by this stack trace element.
     */
    public Class<?> getDeclaringClass() {
        return clazz;
    }

    /**
     * Returns the bytecode index within the method of the execution point represented by this stack trace element.
     */
    public int getBytecodeIndex() {
        return bci;
    }

    /**
     * Returns the method containing the execution point represented by this stack trace element.
     */
    public Member getMethod() {
        return method;
    }

    /**
     * Converts this extended stack element into a plain {@link StackTraceElement} (obviously with no extended information).
     */
    public StackTraceElement getStackTraceElement() {
        return new StackTraceElement(declaringClassName, methodName, fileName, lineNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof ExtendedStackTraceElement))
            return false;
        ExtendedStackTraceElement e = (ExtendedStackTraceElement) obj;
        return e.declaringClassName.equals(declaringClassName)
                && e.lineNumber == lineNumber
                && Objects.equals(methodName, e.methodName)
                && Objects.equals(fileName, e.fileName);
    }

    @Override
    public int hashCode() {
        int result = 31 * declaringClassName.hashCode() + methodName.hashCode();
        result = 31 * result + Objects.hashCode(fileName);
        result = 31 * result + lineNumber;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (method != null)
            sb.append(toString(method));
        else
            sb.append(getClassName()).append('.').append(methodName);
        sb.append(' ');
        if (isNativeMethod())
            sb.append("(Native Method)");
        else {
            sb.append('(');
            if (fileName != null) {
                sb.append(fileName);
                if (lineNumber >= 0)
                    sb.append(':').append(lineNumber);
            } else
                sb.append("Unknown Source");
            if (bci >= 0)
                sb.append(" bci: ").append(bci);
            sb.append(')');
        }
        return sb.toString();
    }

    private static String toString(Member method) {
        final StringBuilder sb = new StringBuilder();
        sb.append(getTypeName(method.getDeclaringClass())) // .getTypeName()
                .append('.')
                .append(method.getName());
        sb.append('(');
        for (Class<?> type : getParameterTypes(method))
            sb.append(getTypeName(type)).append(','); //.getTypeName()
        sb.delete(sb.length() - 1, sb.length());
        sb.append(')');

        return sb.toString();
    }

    private static Class<?>[] getParameterTypes(Member m) {
        if (m instanceof Constructor)
            return ((Constructor<?>) m).getParameterTypes();
        else
            return ((Method) m).getParameterTypes();
    }

    // In Java 8, replaced by Class.getTypeName()
    private static String getTypeName(Class<?> type) {
        if (type.isArray()) {
            try {
                Class<?> cl = type;
                int dimensions = 0;
                while (cl.isArray()) {
                    dimensions++;
                    cl = cl.getComponentType();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable e) { /*FALLTHRU*/ }
        }
        return type.getName();
    }
}
