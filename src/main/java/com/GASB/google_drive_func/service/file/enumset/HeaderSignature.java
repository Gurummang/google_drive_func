package com.GASB.google_drive_func.service.file.enumset;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Getter
@Slf4j
public enum HeaderSignature {

    // Image files
    JPG("jpg", "FFD8", 4),
    PNG("png", "89504E47", 8),
    GIF("gif", "47494638", 6),
    BMP("bmp", "424D", 2),
    TIFF("tiff", "49492A00", 4),
    SVG("svg", "3C73", 4),

    // Document files
    PDF("pdf", "25504446", 4),
    DOC("doc", "D0CF11E0", 8),
    DOCX("docx", "504B0304", 4),
    XLS("xls", "D0CF11E0", 8),
    XLSX("xlsx", "504B0304", 4),
    PPT("ppt", "D0CF11E0", 8),
    PPTX("pptx", "504B0304", 4),
    HTML("html", "3C21646F637479706531", 4),

    // Executable files
    EXE("exe", "4D5A", 2),
    MSI("msi", "D0CF11E0", 8),
    APK("apk", "504B0304", 4),
    JAR("jar", "504B0304", 4),

    // Compressed files
    ZIP("zip", "504B0304", 4),
    RAR("rar", "52617221", 4),
    TAR("tar", "75737461", 4),
    GZ("gz", "1F8B", 2),
    SEVEN_ZIP("7z", "377ABC", 4);

    private final String extension;
    private final String signature;
    private final int signatureLength;

    private static final Map<String, HeaderSignature> extensionToEnumMap = new HashMap<>();
    private static final Map<String, HeaderSignature> signatureToEnumMap = new HashMap<>();

    static {
        for (HeaderSignature type : values()) {
            extensionToEnumMap.put(type.getExtension(), type);
            signatureToEnumMap.put(type.getSignature() + "|" + type.getExtension(), type);
        }
    }

    HeaderSignature(String extension, String signature, int signatureLength) {
        this.extension = extension;
        this.signature = signature;
        this.signatureLength = signatureLength;
    }

    public static String getSignatureByExtension(String extension) {
        HeaderSignature type = extensionToEnumMap.get(extension.toLowerCase());
        return type != null ? type.getSignature() : null;
    }

    public static int getSignatureLengthByExtension(String extension) {
        HeaderSignature type = extensionToEnumMap.get(extension.toLowerCase());
        return type != null ? type.getSignatureLength() : 0;
    }

    public static boolean signatureMatch(String signature, String extension) {
        HeaderSignature headerSignature = extensionToEnumMap.get(extension.toLowerCase());
        if (headerSignature == null) {
            return false;
        } else {
            return headerSignature.getSignature().contains(signature);
        }
    }

    public static String getExtensionBySignature(String signature, String extension) {
        HeaderSignature type = signatureToEnumMap.get(signature.toUpperCase() + "|" + extension.toLowerCase());
        return type != null ? type.getExtension() : null;
    }
}
