/*
 * Copyright 2006 Antonio S. R. Gomes
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package net.sf.jreloader.agent;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * CLASS_COMMENT
 *
 * @author Antonio S. R. Gomes
 */
public class Logger {

    private String name;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");

    private static PrintWriter out;

    private static Level level = Level.DEBUG;

    public static void setLevel(Level level) {
        Logger.level = level;
    }

    static {
        try {
            String fileName = System.getProperty("jreloader.logFile", "jreloader.log");
            out = new PrintWriter(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("*** JRELOADER will not produce any logging output ***");
        }
    }

    public Logger(String name) {
        this.name = name;
    }

    public void debug(String message) {
        print(Level.DEBUG, message);
    }

    public void info(String message) {
        print(Level.INFO, message);
    }

    public void warn(String message) {
        print(Level.WARN, message);
    }

    public void error(String message) {
        print(Level.ERROR, message);
    }

    public void error(String message, Throwable t) {
        print(Level.ERROR, message, t);
    }

    private void print(Level level, String message) {
        print(level, message, null);
    }

    private void print(Level level, String message, Throwable t) {
        print0(out, level, message, t);
        //print0(new PrintWriter(System.err), level, message, t);
    }

    private void print0(PrintWriter out, Level level, String message, Throwable t) {
        if (out != null && level.ordinal()>= Logger.level.ordinal()) {
            Date now = new Date();
            out.format("%s %-5s [%s] - %s",
                       dateFmt.format(now),
                       level.name(),
                       name,
                       message);
            out.println();
            if (t!=null) {
                t.printStackTrace(out);
            }
            out.flush();
        }
    }

}
