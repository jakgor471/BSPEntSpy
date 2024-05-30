/*
 * Decompiled with CFR 0_102.
 */
package util;

import java.io.PrintStream;

public class Cons {
    public static void print(String str) {
        System.out.print(str);
    }

    public static void println(String str) {
        Cons.print(str + "\n");
    }

    public static void println() {
        Cons.print("\n");
    }

    public static void print(Object obj) {
        Cons.print(String.valueOf(obj));
    }

    public static void println(Object obj) {
        Cons.print(obj);
        Cons.println();
    }
}

