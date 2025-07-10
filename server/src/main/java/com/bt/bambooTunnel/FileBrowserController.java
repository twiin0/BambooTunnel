package com.bt.bambooTunnel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class FileBrowserController {

    private final Path downloadDir;

    public FileBrowserController(@Value("${download.dir}") String downloadPath) {
        this.downloadDir = Paths.get(downloadPath).normalize();
    }

    @GetMapping("/browse")
    public ResponseEntity<?> browse(@RequestParam(defaultValue = "") String path) {
        Path current = downloadDir.resolve(path).normalize();

        if (!current.startsWith(downloadDir)) {
            return ResponseEntity.status(403).body("Access denied");
        }

        try (Stream<Path> stream = Files.list(current)) {
            var list = stream.map(p -> {
                String type = Files.isDirectory(p) ? "directory" : "file";
                String name = p.getFileName().toString();
                String relativePath = downloadDir.relativize(p).toString().replace("\\", "/");
                String preview = findPreviewUrlFor(p);

                return Map.of(
                        "name", name,
                        "type", type,
                        "path", relativePath,
                        "preview", preview == null ? "" : preview
                );
            }).toList();

            return ResponseEntity.ok(list);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to read folder");
        }
    }

    private String findPreviewUrlFor(Path p) {
        try {
            if (Files.isDirectory(p)) {
                String[] commonNames = { "cover.jpg", "folder.jpg", "album.jpg" };
                for (String name : commonNames) {
                    Path candidate = p.resolve(name);
                    if (Files.exists(candidate)) {
                        return "/download/" + encodePath(downloadDir.relativize(candidate).toString());
                    }
                }

                try (Stream<Path> stream = Files.list(p)) {
                    return stream
                            .filter(file -> !Files.isDirectory(file))
                            .filter(this::isImageFile)
                            .map(file -> "/download/" + encodePath(downloadDir.relativize(file).toString()))
                            .findFirst()
                            .orElse(null);
                }
            } else {
                Path parent = p.getParent();
                if (parent != null) {
                    try (Stream<Path> stream = Files.list(parent)) {
                        return stream
                                .filter(file -> !Files.isDirectory(file))
                                .filter(this::isImageFile)
                                .map(file -> "/download/" + encodePath(downloadDir.relativize(file).toString()))
                                .findFirst()
                                .orElse(null);
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }

        return null;
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".webp");
    }

    private String encodePath(String path) {
        return Arrays.stream(path.replace("\\", "/").split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }
}


