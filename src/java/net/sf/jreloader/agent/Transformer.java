/*
 * Copyright 2006 Antonio S. R. Gomes Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package net.sf.jreloader.agent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class file transformer (this class is tricky).
 * @author Antonio S. R. Gomes
 */
public class Transformer implements ClassFileTransformer {

    private Logger log = new Logger("Transformer");

    private Map<String, Entry> entries = new LinkedHashMap<String, Entry>();
    private ReloadThread thread;

    public Transformer() {

        String[] dirNames = System.getProperty("jreloader.dirs", ".").split("\\,");

        for (String dirName : dirNames) {
            File d = new File(dirName).getAbsoluteFile();
            log.info("Added class dir '" + d.getAbsolutePath() + "'");
            scan(d, d);
        }
        findGroups();
        log.info(" \\-- Found " + entries.size() + " classes");
        thread = new ReloadThread();
        thread.start();
    }

    private FileFilter filter = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory() || pathname.getName().endsWith(".class");
        };
    };

    private void scan(File base, File dir) {
        File[] files = dir.listFiles(filter);
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    Entry e = new Entry();
                    e.name = nameOf(base, file);
                    log.debug(" found class " + e.name);
                    e.file = file;
                    e.lastModified = file.lastModified();
                    entries.put(e.name, e);
                } else {
                    scan(base, file);
                }
            }
        }
    }

    private void findGroups() {
        for (java.util.Map.Entry<String, Entry> e : entries.entrySet()) {
            String n = e.getValue().name;
            int p = n.indexOf('$');
            if (p != -1) {
                String parentName = n.substring(0, p);
                entries.get(parentName).addChild(e.getValue());
            }
        }
    }

    private String nameOf(File base, File f) {
        String s = base.getAbsolutePath();
        String s1 = f.getAbsolutePath();
        return s1
            .substring(s.length() + 1, s1.length() - ".class".length())
                .replace(File.separatorChar, '/');
    }

    private class Entry {

        String name;
        File file;
        long lastModified;
        List<Entry> children;
        Entry parent;
        WeakReference<ClassLoader> loaderRef;

        void addChild(Entry e) {
            children = (children == null) ? new ArrayList<Entry>() : children;
            children.add(e);
            e.parent = this;
        }

        boolean isDirty() {
            return file.lastModified() > lastModified;
        }

        void clearDirty() {
            // System.err.println("clearDirty: " + name);
            lastModified = file.lastModified();
            if (children != null) {
                for (Entry e : children) {
                    e.lastModified = e.file.lastModified();
                }
            }
        }

        /**
         *
         */
        public void forceDirty() {
            if (parent == null) {
                lastModified = 0;
                if (children != null) {
                    for (Entry e : children) {
                        e.lastModified = 0;
                    }
                }
            } else {
                parent.forceDirty();
            }
        }

    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] currentBytes) throws IllegalClassFormatException {
        String clname = "bootstrap";
        if (loader != null) {
            clname = loader.getClass().getName() + "@"
                    + Integer.toHexString(System.identityHashCode(loader));
        }
        Entry e = entries.get(className);
        if (e != null) {
            log.debug(clname + " is loading " + className);
        }
        if (e != null && loader != null) {
            e.loaderRef = new WeakReference<ClassLoader>(loader);
        }
        return null;
    }

    class ReloadThread extends Thread {
        public ReloadThread() {
            super("ReloadThread");
            setDaemon(true);
            setPriority(MAX_PRIORITY);
        }

        @Override
        public void run() {
            try {
                sleep(5000);
            } catch (InterruptedException e) {
            }
            while (true) {
                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                }
                if (System.getProperty("jreloader.pauseReload") != null) {
                    continue;
                }
                log.debug("Checking changes...");
                List<Entry> aux = new ArrayList<Entry>(entries.values());
                for (Entry e : aux) {
                    if (e.isDirty()) {
                        e.forceDirty();
                    }
                }
                for (Entry e : aux) {
                    if (e.isDirty() && e.parent == null) {
                        log.debug("Reloading " + e.name);
                        try {
                            reload(e);
                        } catch (Throwable t) {
                            log.error("Could not reload " + e.name, t);
                            System.err
                                .println("[JReloader:ERROR] Could not reload class "
                                        + e.name.replace('/', '.'));
                        }
                        e.clearDirty();
                    }
                }
            }
        }

        private List<ClassDefinition> cdefs = new LinkedList<ClassDefinition>();

        private void reload(Entry e)
            throws IOException, ClassNotFoundException, UnmodifiableClassException {
            System.err.println(e.file);
            cdefs.clear();
            if (e.loaderRef != null) {
                ClassLoader cl = e.loaderRef.get();
                if (cl != null) {
                    request(e, cl);
                    if (e.children != null) {
                        for (Entry ce : e.children) {
                            request(ce, cl);
                        }
                    }
                    // System.err.println(cdefs);
                    Agent.inst.redefineClasses(cdefs.toArray(new ClassDefinition[0]));
                } else {
                    e.loaderRef = null;
                }
            }
        }

        private void request(Entry e, ClassLoader cl)
            throws IOException, ClassNotFoundException {
            byte[] bytes = loadBytes(e.file);
            String className = e.name.replace('/', '.');
            Class<?> clazz = cl.loadClass(className);
            log.info("Requesting reload of " + e.name);
            System.out.println("[JReloader:INFO ] Reloading class " + className);
            cdefs.add(new ClassDefinition(clazz, bytes));
        }

    }

    public static byte[] loadBytes(File classFile) throws IOException {
        byte[] buffer = new byte[(int) classFile.length()];
        FileInputStream fis = new FileInputStream(classFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(buffer);
        bis.close();
        return buffer;

    }

}
