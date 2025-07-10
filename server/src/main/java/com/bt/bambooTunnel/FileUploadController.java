package com.bt.bambooTunnel;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

@RestController
public class FileUploadController {

    private final Path uploadDir;
    private final Path downloadDir;

    public FileUploadController(
            @Value("${upload.dir}") String uploadPath,
            @Value("${download.dir}") String downloadPath
    ) {
        this.uploadDir = Paths.get(uploadPath);
        this.downloadDir = Paths.get(downloadPath);

        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            Path target = uploadDir.resolve(file.getOriginalFilename());
            file.transferTo(target.toFile());
            return ResponseEntity.ok("File uploaded");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
        }
    }

    @GetMapping("/download/**")
    public ResponseEntity<Resource> download(HttpServletRequest request) throws IOException {
        // Extract the path after "/download/"
        String rawPath = request.getRequestURI().substring("/download/".length());

        // Decode URL-encoded path parts (%20 to space, etc.)
        String decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);

        Path fullPath = downloadDir.resolve(decodedPath).normalize();

        if (!fullPath.startsWith(downloadDir) || !Files.exists(fullPath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(fullPath.toUri());

        // Detect MIME type of the file
        String contentType = Files.probeContentType(fullPath);
        if (contentType == null) {
            contentType = "application/octet-stream";  // fallback
        }

        return ResponseEntity.ok()
                // Use inline so browser can display images directly instead of downloading
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fullPath.getFileName() + "\"")
                // Set proper content type
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

}
