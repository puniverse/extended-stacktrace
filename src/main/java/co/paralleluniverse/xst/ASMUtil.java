/*
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.xst;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

final class ASMUtil {
    private static final String CLASS_FILE_NAME_EXTENSION = ".class";
    
    public static InputStream getClassInputStream(String className, ClassLoader cl) {
        return cl.getResourceAsStream(classToResource(className));
    }

    public static InputStream getClassInputStream(Class<?> clazz) {
        final InputStream is = getClassInputStream(clazz.getName(), clazz.getClassLoader());
        if (is == null)
            throw new UnsupportedOperationException("Class file " + clazz.getName() + " could not be loaded by the class's classloader " + clazz.getClassLoader());
        return is;
    }

    public static <T extends ClassVisitor> T accept(InputStream is, int flags, T visitor) throws IOException {
        if (is == null)
            return null;
        try (InputStream is1 = is) {
            new ClassReader(is1).accept(visitor, flags);
            return visitor;
        }
    }

    public static <T extends ClassVisitor> T accept(byte[] buffer, int flags, T visitor) throws IOException {
        if (buffer == null)
            throw new NullPointerException("Buffer is null");
        new ClassReader(buffer).accept(visitor, flags);
        return visitor;
    }

    public static <T extends ClassVisitor> T accept(String className, ClassLoader cl, int flags, T visitor) throws IOException {
        return accept(getClassInputStream(className, cl), flags, visitor);
    }

    public static <T extends ClassVisitor> T accept(Class<?> clazz, int flags, T visitor) throws IOException {
        return accept(getClassInputStream(clazz), flags, visitor);
    }

    public static String classToResource(String className) {
        if (className == null)
            return null;
        return className.replace('.', '/') + CLASS_FILE_NAME_EXTENSION;
    }

    public static String classToResource(Class<?> clazz) {
        if (clazz == null)
            return null;
        return classToResource(clazz.getName());
    }

    public static String classToSlashed(String className) {
        if (className == null)
            return null;
        return className.replace('.', '/');
    }

    private ASMUtil() {
    }
}
