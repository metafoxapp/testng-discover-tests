package com.metafox.qatools;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class PathReader {
    private static String nameToDot(String absolutePath, int strip) {
        return absolutePath.substring(strip).replace(".java", "").replace("/", ".");
    }

    public static List<String> listPackages(String rootDir, String matcherPattern, String glue) {
        return listPackages(Paths.get(rootDir), matcherPattern, glue);
    }

    public static List<String> listPackages(Path rootDir, String matcherPattern, String glue) {
        List<String> result = new ArrayList<>();
        int strip = rootDir.toAbsolutePath().toString().length() + 1;
        listFiles(rootDir, matcherPattern)
                .forEach((Path file) -> {
                    result.add(glue + "." + nameToDot(file.toAbsolutePath().toString(), strip));
                });
        return result;
    }

    public static List<Path> listFiles(String rootDir, String matcherPattern) {
        return PathReader.listFiles(Paths.get(rootDir), matcherPattern);
    }

    public static List<Path> listFiles(Path rootDir, String matcherPattern) {
        final List<Path> result = new ArrayList<>();
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(matcherPattern);
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matcher.matches(file) || matcher.matches(file.getFileName())) {
                        result.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
