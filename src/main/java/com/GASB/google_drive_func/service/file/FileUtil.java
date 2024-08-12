package com.GASB.google_drive_func.service.file;

import com.google.api.services.drive.Drive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@Service
public class FileUtil {


    public void DownloadFileMethod(String fileId, String filePath, Drive service) {
        try {
            com.google.api.services.drive.model.File file = service.files().get(fileId).execute();

            // 파일 경로의 디렉터리 부분을 추출
            Path parentDir = Paths.get(filePath).getParent();

            // 디렉터리가 존재하지 않으면 생성
            if (parentDir != null && !parentDir.toFile().exists()) {
                parentDir.toFile().mkdirs();
            }

            try (OutputStream outputStream = new FileOutputStream(filePath)){
                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                log.info("File downloaded successfully: {}", filePath);
            }

        } catch (Exception e){
            log.error("An error occurred while downloading the file: {}", e.getMessage(), e);
        }
    }
}
