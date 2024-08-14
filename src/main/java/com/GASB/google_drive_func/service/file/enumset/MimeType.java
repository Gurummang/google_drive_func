package com.GASB.google_drive_func.service.file.enumset;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Getter
@Slf4j
public enum MimeType {
    // Image files
    JPG("jpg", "image/jpeg"),
    JPEG("jpeg", "image/jpeg"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    BMP("bmp", "image/bmp"),
    SVG("svg", "image/svg+xml"),
    ICO("ico", "image/vnd.microsoft.icon"),
    TIFF("tiff", "image/tiff"),
    WEBP("webp", "image/webp"),

    // Document files
    PDF("pdf", "application/pdf"),
    DOC("doc", "application/msword"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLS("xls", "application/vnd.ms-excel"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPT("ppt", "application/vnd.ms-powerpoint"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    TXT("txt", "text/plain"),
    HTML("html", "text/html"),
    CSV("csv", "text/csv"),
    XML("xml", "application/xml"),
    JSON("json", "application/json"),
    LOG("log", "text/plain"),

    // Executable files
    EXE("exe", "application/vnd.microsoft.portable-executable"),
    MSI("msi", "application/x-msi"),
    APK("apk", "application/vnd.android.package-archive"),
    JAR("jar", "application/java-archive"),
    BIN("bin", "application/octet-stream"),
    SH("sh", "application/x-sh"),

    // Compressed files
    ZIP("zip", "application/zip"),
    RAR("rar", "application/vnd.rar"),
    TAR("tar", "application/x-tar"),
    GZ("gz", "application/gzip"),
    SEVEN_ZIP("7z", "application/x-7z-compressed"),
    SLACK_CANVAS("quip", "application/vnd.slack-docs"),

    TEXT("text", "text/plain");

    private final String extension;
    private final String mimeType;

    private static final Map<String, MimeType> extensionToEnumMap = new HashMap<>();
    private static final Map<String, MimeType> mimeTypeToEnumMap = new HashMap<>();

    static {
        for (MimeType type : values()) {
            extensionToEnumMap.put(type.getExtension(), type);
            mimeTypeToEnumMap.put(type.getMimeType(), type);
        }
    }

    MimeType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public static String getMimeTypeByExtension(String extension) {
        MimeType type = extensionToEnumMap.get(extension.toLowerCase());
        return type != null ? type.getMimeType() : "application/octet-stream"; // 기본값
    } // 함수 설명 : 확장자에 따른 MIME 타입을 반환

    public static String getExtensionByMimeType(String mimeType) {
        MimeType type = mimeTypeToEnumMap.get(mimeType.toLowerCase());
        return type != null ? type.getExtension() : ""; // 기본값
    } // 함수 설명 : MIME 타입에 따른 확장자를 반환

    public static boolean mimeMatch(String mimeType, String extension) {
        String expectedMimeType = getMimeTypeByExtension(extension);
//        log.info("MIME 타입 : " + mimeType + " / 기대 MIME 타입 : " + expectedMimeType);
        return mimeType.equals(expectedMimeType);
    } // 함수 설명 : MIME 타입과 확장자가 일치하는지 확인



}
