package com.GASB.google_drive_func.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Component
@Slf4j
public class FileEncUtil {

    @Value("${file.aes.key}")
    private String fileEncAESkey;

    @Value("${file.enc.basepath}")
    private String basePath;

    private SecretKey secretKey;

    @PostConstruct
    public void setUp(){
        try {
            secretKey = decodeKeyFromBase64(fileEncAESkey);
        } catch (IllegalArgumentException e) {
            log.error("AES 키 복호화 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("AES 키 복호화 실패", e);
        }
    }

    // 파일 암호화 및 저장
    public Path encryptAndSaveFile(String sourceFilePath) throws Exception {
        if (sourceFilePath == null || sourceFilePath.isEmpty()) {
            log.error("파일 경로가 null이거나 빈 값입니다.");
            throw new IllegalArgumentException("파일 경로는 null이거나 빈 값일 수 없습니다.");
        }
        if (secretKey == null) {
            log.error("AES 키가 초기화되지 않았습니다.");
            throw new IllegalStateException("AES 키가 설정되지 않았습니다.");
        }

        try {
            // 파일명 추출
            String fileName = Paths.get(sourceFilePath).getFileName().toString();
            log.info("암호화할 파일명: {}", fileName);

            // 파일 읽기
            byte[] fileContent = readFileContent(sourceFilePath);
            log.info("파일 읽기 성공: {} (크기: {} bytes)", sourceFilePath, fileContent.length);

            // 파일 암호화
            byte[] encryptedContent = encryptFile(fileContent, secretKey);
            log.info("파일 암호화 성공. 파일명: {}", fileName);

            // 암호화된 파일 저장
            return saveFileContent(encryptedContent, fileName);
        } catch (IOException e) {
            log.error("파일 처리 중 IO 오류 발생: {}", e.getMessage());
            throw new IOException("파일 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("파일 암호화 중 오류 발생: {}", e.getMessage());
            throw new Exception("파일 암호화 중 오류가 발생했습니다.", e);
        }
    }

    private Path saveFileContent(byte[] content, String fileName) throws IOException {
        try {
            Path dstPath = Paths.get(basePath).resolve(fileName);
            Files.createDirectories(dstPath.getParent()); // 상위 디렉터리 생성
            Files.write(dstPath, content);
            return dstPath;
        } catch (IOException e) {
            log.error("암호화된 파일 저장 중 오류 발생: {}", e.getMessage());
            throw new IOException("암호화된 파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    private byte[] readFileContent(String filePath) throws IOException {
        try {
            Path path = Paths.get(filePath);
            byte[] fileContent = Files.readAllBytes(path);
            log.info("파일 읽기 성공: {}", filePath);
            return fileContent;
        } catch (IOException e) {
            log.error("파일 읽기 중 오류 발생: {}", e.getMessage());
            throw new IOException("파일을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    public static byte[] encryptFile(byte[] content, SecretKey secretKey) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            log.debug("AES 암호화 준비 완료.");
            return cipher.doFinal(content);
        } catch (Exception e) {
            log.error("파일 암호화 중 오류 발생: {}", e.getMessage());
            throw new Exception("파일 암호화 중 오류가 발생했습니다.", e);
        }
    }

    public static SecretKey decodeKeyFromBase64(String base64Key) {
        if (base64Key == null || base64Key.isEmpty()) {
            log.error("복원할 Base64 인코딩된 키가 없습니다.");
            throw new IllegalArgumentException("복원할 Base64 인코딩된 키가 없습니다.");
        }
        log.debug("AES 키 복호화 시작.");
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}