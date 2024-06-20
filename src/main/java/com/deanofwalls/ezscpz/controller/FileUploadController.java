package com.deanofwalls.ezscpz.controller;

import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class FileUploadController {

    private final Path rootLocation = Paths.get("uploads");

    @Value("${scp.host}")
    private String scpHost;

    @Value("${scp.username}")
    private String scpUsername;

    @Value("${scp.password}")
    private String scpPassword;

    @Value("${scp.port}")
    private int scpPort;

    @Value("${scp.destination}")
    private String scpDestination;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {
        model.addAttribute("files", Files.walk(this.rootLocation, 1)
                .filter(path -> !path.equals(this.rootLocation))
                .map(this.rootLocation::relativize)
                .map(path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "serveFile", path.toString()).scheme("https").build().toUri().toString())
                .collect(Collectors.toList()));

        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            if (file.isEmpty() || !file.getContentType().startsWith("image/")) {
                model.addAttribute("message", "Please upload a valid image file.");
                return "uploadForm";
            }
            Files.copy(file.getInputStream(), this.rootLocation.resolve(file.getOriginalFilename()), StandardCopyOption.REPLACE_EXISTING);

            // SCP transfer
            String filePath = this.rootLocation.resolve(file.getOriginalFilename()).toString();
            scpTransfer(file, filePath);

            model.addAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");
        } catch (Exception e) {
            model.addAttribute("message", "Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage());
        }

        return "redirect:/";
    }

    private void scpTransfer(MultipartFile file, String filePath) throws JSchException, IOException, InterruptedException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(scpUsername, scpHost, scpPort);
        session.setPassword(scpPassword);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        String command = "scp -t " + scpDestination;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();
        channel.connect();

        if (checkAck(in) != 0) {
            throw new IOException("Failed to initiate SCP transfer.");
        }

        out.write(("C0644 " + file.getSize() + " " + file.getOriginalFilename() + "\n").getBytes());
        out.flush();
        if (checkAck(in) != 0) {
            throw new IOException("Failed to send file metadata.");
        }

        InputStream fis = file.getInputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        fis.close();
        out.write(0);
        out.flush();
        if (checkAck(in) != 0) {
            throw new IOException("Failed to transfer file.");
        }

        out.close();
        channel.disconnect();
        session.disconnect();
    }

    private int checkAck(InputStream in) throws IOException {
        int b = in.read();
        if (b == 0) return b;
        if (b == -1) return b;
        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            System.out.print(sb.toString());
        }
        return b;
    }

    @GetMapping("/gallery")
    @ResponseBody
    public ResponseEntity<?> getGallery() {
        try {
            List<String> files = Files.walk(rootLocation, 1)
                    .filter(path -> !path.equals(rootLocation))
                    .map(path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                            "serveFile", path.getFileName().toString()).scheme("https").build().toUri().toString())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to load gallery");
        }
    }
}
