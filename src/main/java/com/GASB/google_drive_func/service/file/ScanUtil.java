package com.GASB.google_drive_func.service.file;

import com.GASB.google_drive_func.model.entity.FileUploadTable;
import com.GASB.google_drive_func.model.entity.TypeScan;
import com.GASB.google_drive_func.model.repository.files.TypeScanRepo;
import com.GASB.google_drive_func.service.event.MessageSender;
import com.GASB.google_drive_func.service.file.enumset.HeaderSignature;
import com.GASB.google_drive_func.service.file.enumset.MimeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScanUtil {

    private final Tika tika;
    private final TypeScanRepo typeScanRepo;
    private final MessageSender messageSender;

    @Async("threadPoolTaskExecutor")
    public void scanFile(String path, FileUploadTable fileUploadTableObject, String MIMEType) {
        try {
            // 중복 튜플 방지
            if (typeScanRepo.existsByUploadId(fileUploadTableObject.getId())) {
                log.error("Duplicated tuple: {}", fileUploadTableObject.getId());
                return;
            }

            File inputFile = new File(path);

            // 파일이 존재하는지 확인
            if (!inputFile.exists()) {
                log.error("File does not exist: {}", inputFile.getAbsolutePath());
                return;
            }
            if (!inputFile.isFile()) {
                log.error("Path is not a file: {}", inputFile.getAbsolutePath());
                return;
            }

            String fileExtension = getFileExtension(inputFile);
            String mimeType = (MIMEType != null && !MIMEType.isEmpty()) ? MIMEType : tika.detect(inputFile);
            String expectedMimeType = MimeType.getMimeTypeByExtension(fileExtension);
            String fileSignature = "unknown";

            boolean isMatched;

            if ("txt".equals(fileExtension)) {
                isMatched = mimeType.equals(expectedMimeType);
                addData(fileUploadTableObject, isMatched, mimeType, fileSignature, fileExtension);
            } else {
                fileSignature = getFileSignature(inputFile, fileExtension);
                if (fileSignature == null) {
                    isMatched = checkWithoutSignature(mimeType, expectedMimeType, fileExtension);
                } else {
                    isMatched = checkAllType(mimeType, fileExtension, fileSignature, expectedMimeType);
                }
                addData(fileUploadTableObject, isMatched, mimeType, fileSignature, fileExtension);
            }

            // 데이터 저장 후 메시지 전송
            messageSender.sendMessage(fileUploadTableObject.getId());

        } catch (IOException e) {
            log.error("Error scanning file: {}", path, e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while scanning file: {}", path, e);
        }
    }

    @Transactional
    protected void addData(FileUploadTable fileUploadTableObject, boolean correct, String mimeType, String signature, String extension) {
        if (fileUploadTableObject == null || fileUploadTableObject.getId() == null) {
            throw new IllegalArgumentException("Invalid file upload object");
        }
        TypeScan typeScan = TypeScan.builder()
                .file_upload(fileUploadTableObject)
                .correct(correct)
                .mimetype(mimeType)
                .signature(signature)
                .extension(extension)
                .build();
        typeScanRepo.save(typeScan);
    }

    private String getFileExtension(File file) {
        String fileName = file.getName();
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return ""; // 확장자가 없는 경우
        }
        return fileName.substring(lastIndexOfDot + 1).toLowerCase();
    }

    private String getFileSignature(File file, String extension) throws IOException {
        if (extension == null || extension.isEmpty()) {
            log.error("Invalid file extension: {}", extension);
            return "unknown";  // 기본값으로 "unknown" 반환
        }

        int signatureLength = HeaderSignature.getSignatureLengthByExtension(extension);
        if (signatureLength == 0) {
            log.info("No signature length for extension: {}", extension);
            return "unknown";
        }

        byte[] bytes = new byte[signatureLength];

        // try-with-resources로 FileInputStream 자동 닫기
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(bytes);
            if (bytesRead < signatureLength) {
                log.error("Could not read the complete file signature for file: {}", file.getName());
                return "unknown";
            }
        }

        StringBuilder sb = new StringBuilder(signatureLength * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }

        String detectedExtension = HeaderSignature.getExtensionBySignature(sb.toString(), extension);
        log.info("Detected extension for signature {}: {}", sb.toString(), detectedExtension);

        return detectedExtension;
    }


    private boolean checkAllType(String mimeType, String extension, String signature, String expectedMimeType) {
        log.info("Checking all types: mimeType={}, extension={}, signature={}, expectedMimeType={}", mimeType, extension, signature, expectedMimeType);
        return mimeType.equals(expectedMimeType) &&
                MimeType.mimeMatch(mimeType, signature) &&
                MimeType.mimeMatch(mimeType, extension);
    }

    private boolean checkWithoutSignature(String mimeType, String expectedMimeType, String extension) {
        return mimeType.equals(expectedMimeType) &&
                MimeType.mimeMatch(mimeType, extension);
    }

    private void deleteFileInLocal(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            log.error("Invalid file path provided for deletion.");
            return;
        }
        File file = new File(filePath);
        if (file.exists() && file.delete()) {
            log.info("File deleted successfully: {}", filePath);
        } else {
            log.warn("Failed to delete or file does not exist: {}", filePath);
        }
    }
}
