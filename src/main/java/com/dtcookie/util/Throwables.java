package com.dtcookie.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public interface Throwables {
    
    public static String toString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
}
