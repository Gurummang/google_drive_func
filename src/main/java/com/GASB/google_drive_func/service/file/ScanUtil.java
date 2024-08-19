package com.GASB.google_drive_func.service.file;

import com.GASB.google_drive_func.model.entity.FileUploadTable;
import com.GASB.google_drive_func.model.entity.TypeScan;
import com.GASB.google_drive_func.model.repository.files.TypeScanRepo;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ScanUtil {

    private final Tika tika;
    private final TypeScanRepo typeScanRepo;
    
    @Async
    public void scanFile(String path, FileUploadTable fileUploadTableObject, String MIMEType, String Extension) {
        try {
            File inputFile = new File(path);
            if (!inputFile.exists() || !inputFile.isFile()) {
                log.error("Invalid file path: {}", path);
                return;
            }

            String fileExtension = getFileExtension(inputFile);
            String mimeType = MIMEType != null && !MIMEType.isEmpty() ? MIMEType : tika.detect(inputFile);
            String expectedMimeType = MimeType.getMimeTypeByExtension(fileExtension);
            String fileSignature = null;
            boolean isMatched;

            if (fileExtension.equals("txt")) {
                // txt 파일의 경우 시그니처가 없으므로 MIME 타입만으로 검증
                isMatched = mimeType.equals(expectedMimeType);
                addData(fileUploadTableObject, isMatched, mimeType, "unknown", fileExtension);
            } else {
                fileSignature = getFileSignature(inputFile, fileExtension);
                if (fileSignature == "unknown"){
                    // 확장자와 MIME타입만 검사함
                    isMatched = checkWithoutSignature(mimeType, expectedMimeType, fileExtension);
                    addData(fileUploadTableObject, isMatched, mimeType, "unknown", fileExtension);
                }
                if (fileSignature == null || fileSignature.isEmpty()) {
                    // 확장자와 MIME 타입만 존재하는 경우
                    isMatched = checkWithoutSignature(mimeType, expectedMimeType, fileExtension);
                    addData(fileUploadTableObject, isMatched, mimeType, "unknown", fileExtension);
                } else {
                    // MIME 타입, 확장자, 시그니처가 모두 존재하는 경우
                    isMatched = checkAllType(mimeType, fileExtension, fileSignature, expectedMimeType);
                    addData(fileUploadTableObject, isMatched, mimeType, fileSignature, fileExtension);
                }
            }
        } catch (IOException e) {
            log.error("Error scanning file", e);
        }
    }

    @Async
    @Transactional
    protected void addData(FileUploadTable fileUploadTableObject, boolean correct, String mimeType, String signature, String extension) {
        if (fileUploadTableObject == null || fileUploadTableObject.getId() == null) {
//            log.error("Invalid file upload object: {}, {}", fileUploadObject, fileUploadObject.getId());

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
            return null; // 기본값으로 "unknown"을 반환
        }

        int signatureLength = HeaderSignature.getSignatureLengthByExtension(extension);
        if (signatureLength == 0) {
            log.info("No signature length for extension: {}", extension);
            return "unknown";
        }

        byte[] bytes = new byte[signatureLength];

        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(bytes);
            if (bytesRead < signatureLength) {
                log.error("Could not read the complete file signature");
                return "unknown";
            }
        }

        StringBuilder sb = new StringBuilder(signatureLength * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }

        String signatureHex = sb.toString();
        String detectedExtension = HeaderSignature.getExtensionBySignature(signatureHex, extension);
        log.info("Detected extension for signature {}: {}", signatureHex, detectedExtension);

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

}
