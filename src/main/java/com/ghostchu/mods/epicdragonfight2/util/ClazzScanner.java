package com.ghostchu.mods.epicdragonfight2.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClazzScanner {
    private final String packagePath;

    public ClazzScanner(String packagePath) {
        this.packagePath = packagePath;
    }

    public List<Class<?>> getScanResult(){
        try {
            return scanClasses();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Class<?>> loadClasses(List<File> classes, String scan) throws ClassNotFoundException {
        List<Class<?>> clazzes = new ArrayList<>();
        for (File file : classes) {
            String fPath = file.getAbsolutePath().replaceAll("\\\\", "/");
            String packageName = fPath.substring(fPath.lastIndexOf(scan));
            packageName = packageName.replace(".class", "").replaceAll("/", ".");
            clazzes.add(Class.forName(packageName));
        }
        return clazzes;

    }

    private static void listFiles(File dir, List<File> fileList) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                listFiles(f, fileList);
            }
        } else {
            if (dir.getName().endsWith(".class")) {
                fileList.add(dir);
            }
        }
    }


    public List<Class<?>> scanClasses() throws IOException, ClassNotFoundException {
        List<Class<?>> result = new ArrayList<>();
        String scan = packagePath;
        scan = scan.replaceAll("\\.", "/");
        Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(scan);
        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            if (url.getProtocol().equals("file")) {
                List<File> classes = new ArrayList<>();
                listFiles(new File(url.getFile()), classes);
                result.addAll(loadClasses(classes, scan));
            } else if (url.getProtocol().equals("jar")) {
                continue; // Unsupported
            }
        }
        return result;
    }
}
