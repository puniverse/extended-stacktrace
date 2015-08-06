
# Extended Stack-Trace<br>Enhanced stack traces for the JVM

This library allows you to capture stack traces with more information than `Throwable.getStackTrace`. 
In addition to the information in the JDK's `StackTraceElement` The captured stack elements contain:

* The decalaring class -- the actual `Class` object -- not just the name.
* The method -- the actual `Method` object -- not just the method name.
* The bytecode index

Works best when running on a Java 8 HotSpot (OpenJDK/Oracle JDK) JVM.

## Status

Advanced Beta. 
Used in [Quasar](https://github.com/puniverse/quasar).

## Usage

1. Clone and build the repository with `./gradlew` or use Maven artifact `co.paralleluniverse:extended-stacktrace:0.1.0-SNAPSHOT`
from the Sonatype snapshot repository (`https://oss.sonatype.org/content/repositories/snapshots`)

2. Add the JAR file to your dependencies.

3. Obtain the extended stack trace information with `ExtendedStackTrace.here()` or `ExtendedStackTrace.of(Throwable)`


Please consult the [Javadocs](http://docs.paralleluniverse.co/extended-stacktrace/javadoc/) for detailed information.

## Details

On the HotSpot JVM (OpenJDK/Oracle JDK) for Java 8, the extended information is always available, and obtaining it is as efficient as a plain `Throwable.getStackTrace`.

On other JVMs/Java versions the extended information may be incomplete. There are (much) better chances for obtaining extended information when capturing the stack with 
`ExtendedStackTrace.here()` than when extracting extended information from a `Throwable` with `ExtendedStackTrace.of(Throwable)`. Also, getting the method object carries a significant cost.

## License

This project is free software free software published under the following license:

```
Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.

This program and the accompanying materials are dual-licensed under
either the terms of the Eclipse Public License v1.0 as published by
the Eclipse Foundation

  or (per the licensee's choosing)

under the terms of the GNU Lesser General Public License version 3.0
as published by the Free Software Foundation.
```
