package com.deanofwalls.ezscpz.controller;

import com.jcraft.jsch.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/api/scp")
public class ScpController {

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam("username") String username,
                                             @RequestParam("password") String password,
                                             @RequestParam("host") String host,
                                             @RequestParam("port") int port,
                                             @RequestParam("destination") String destination) {
        try {
            System.out.println("Connecting to " + username + "@" + host + ":" + port);
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            String command = "scp -t " + destination;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();
            channel.connect();

            if (checkAck(in) != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to initiate SCP transfer.");
            }

            out.write(("C0644 " + file.getSize() + " " + file.getOriginalFilename() + "\n").getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send file metadata.");
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
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to transfer file.");
            }

            out.close();
            channel.disconnect();
            session.disconnect();

            return ResponseEntity.ok("File uploaded successfully.");
        } catch (JSchException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error uploading file: Authentication failed.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading file: " + e.getMessage());
        }
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
}
