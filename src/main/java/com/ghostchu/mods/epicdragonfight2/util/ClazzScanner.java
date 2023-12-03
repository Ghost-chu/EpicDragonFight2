package com.ghostchu.mods.epicdragonfight2.util;

import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClazzScanner {
    private final String packagePath;

    public ClazzScanner(String packagePath) {
        this.packagePath = packagePath;
    }

    public List<Class<?>> getScanResult() {
        try {
            return scanClasses();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Class<?>> scanClasses() throws IOException, ClassNotFoundException {
        List<Class<?>> found = new ArrayList<>();
        try (ScanResult scanResult =
                     new ClassGraph()
                             .verbose()               // Log to stderr
                             .enableAllInfo()         // Scan classes, methods, fields, annotations
                             .acceptPackages(packagePath)     // Scan com.xyz and subpackages (omit to scan all packages)
                             .scan()) {               // Start the scan
            for (ClassInfo routeClassInfo : scanResult.getClassesWithAnnotation(EpicSkill.class)) {
                found.add(routeClassInfo.loadClass(true));
            }
        }
        return found;
    }
}
