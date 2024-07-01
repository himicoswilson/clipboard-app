package com.example.clipboard.controller;

import com.example.clipboard.model.TemporaryFile;
import com.example.clipboard.repository.TemporaryFileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class TemporaryFileController {

    private final TemporaryFileRepository temporaryFileRepository;

    public TemporaryFileController(TemporaryFileRepository temporaryFileRepository) {
        this.temporaryFileRepository = temporaryFileRepository;
    }

    private static final String UPLOAD_DIR = "uploads/";

    @GetMapping
    public List<TemporaryFile> getAllFiles() {
        return temporaryFileRepository.findAll();
    }

    @PostMapping("/upload")
    public TemporaryFile uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        String baseFilename = originalFilename;

        // 獲取文件名及拓展名
        assert originalFilename != null;
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            baseFilename = originalFilename.substring(0, dotIndex);
            fileExtension = originalFilename.substring(dotIndex);
        }

        // 上傳路徑 無則創建
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成路徑 並判斷是否存在相同路徑 是則修改路徑
        Path path = uploadPath.resolve(originalFilename);
        int count = 1;
        while (Files.exists(path)) {
            String newFilename = baseFilename + "_" + count + fileExtension;
            path = uploadPath.resolve(newFilename);
            count++;
        }

        // 寫入文件
        Files.write(path, file.getBytes());

        // 存到數據庫
        TemporaryFile tempFile = new TemporaryFile();
        tempFile.setFileName(file.getOriginalFilename());
        tempFile.setFilePath(path.toString());
        tempFile.setFileSize(file.getSize());
        temporaryFileRepository.save(tempFile);

        // 判斷總文件數是否大於10 是則刪最先前保留最後
        List<TemporaryFile> files = temporaryFileRepository.findAll();
        if (files.size() > 10) {
            files.sort(Comparator.comparing(TemporaryFile::getCreatedAt));
            TemporaryFile fileToDelete = files.get(0);
            Path filePathToDelete = Paths.get(fileToDelete.getFilePath());
            Files.delete(filePathToDelete);
            temporaryFileRepository.delete(fileToDelete);
        }
        return tempFile;
    }

    @GetMapping("/download/{id}")
    public void downloadFileById(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<TemporaryFile> fileOptional = temporaryFileRepository.findById(id);

        if (fileOptional.isEmpty()) {
            throw new FileNotFoundException("File not found with id " + id);
        }

        TemporaryFile tempFile = fileOptional.get();
        String filePath = tempFile.getFilePath();
        String fileName = new File(filePath).getName();

        // 獲取瀏覽器信息
        String agent = request.getHeader("USER-AGENT");
        if (agent != null && agent.toLowerCase().contains("firefox")) {
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + new String(fileName.getBytes("GB2312"), StandardCharsets.ISO_8859_1));
        } else if (agent != null && agent.toLowerCase().contains("safari")) {
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        } else {
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
        }

        File file = new File(filePath);
        try (FileInputStream fileInputStream = new FileInputStream(file);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             OutputStream outputStream = response.getOutputStream()) {

            byte[] bytes = new byte[1024];
            int len;
            while ((len = fileInputStream.read(bytes)) != -1) {
                byteArrayOutputStream.write(bytes, 0, len);
            }

            response.setContentType("application/octet-stream");
            response.setContentLength((int) file.length());

            byte[] fileByte = byteArrayOutputStream.toByteArray();
            outputStream.write(fileByte);
            outputStream.flush();
        }
    }

    @DeleteMapping("/{id}")
    public void deleteFile(@PathVariable Long id) throws Exception {
        TemporaryFile fileToDelete = temporaryFileRepository.findById(id).orElseThrow(() -> new Exception("File not found"));
        // 獲取需要刪除的文件
        Path filePathToDelete = Paths.get(fileToDelete.getFilePath());
        // 判斷是否存在 存在及刪除
        if (Files.exists(filePathToDelete)) {
            Files.delete(filePathToDelete);
        }
        // 刪除數據庫數據
        temporaryFileRepository.delete(fileToDelete);
    }
}