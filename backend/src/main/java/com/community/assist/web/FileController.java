package com.community.assist.web;

import com.community.assist.service.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * 使用照片等附件上传/访问。GET 免登（文件名为 UUID 不可猜测），POST 需登录。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final Path uploadDir;

    public FileController(@Value("${app.upload-dir:uploads}") String dir) throws IOException {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("文件为空");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw BizException.badRequest("文件不能超过 10MB");
        }
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String name = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), uploadDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return Map.of("name", name, "url", "/api/files/" + name);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> get(@PathVariable String name) throws IOException {
        Path p = uploadDir.resolve(name).normalize();
        if (!p.startsWith(uploadDir) || !Files.exists(p)) {
            throw BizException.notFound("文件不存在");
        }
        String ct = Files.probeContentType(p);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct == null ? "application/octet-stream" : ct))
                .body(new FileSystemResource(p));
    }
}
