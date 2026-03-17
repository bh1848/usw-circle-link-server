package com.USWCicrcleLink.server.global.validation.validator;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class FileSignatureValidator {
    private static final int DEFAULT_SIGNATURE_BYTES = 4;
    private static final int PNG_SIGNATURE_BYTES = 8;
    private final Map<String, String> fileSignatures = Map.of(
            "jpg", "FFD8FF",
            "jpeg", "FFD8FF",
            "png", "89504E47",
            "xls", "D0CF11E0",
            "xlsx", "504B0304"
    );

    public String getFileSignature(InputStream inputStream, int bytesToRead) throws IOException {
        byte[] fileHeader = inputStream.readNBytes(bytesToRead);
        if (fileHeader.length < bytesToRead) {
            return "";
        }
        return bytesToHex(fileHeader);
    }

    public boolean isValidFileType(InputStream inputStream, String expectedExtension) throws IOException {
        String expectedSignature = fileSignatures.get(expectedExtension.toLowerCase());
        if (expectedSignature == null) {
            return false;
        }

        int bytesToRead = expectedExtension.equalsIgnoreCase("png") ? PNG_SIGNATURE_BYTES : DEFAULT_SIGNATURE_BYTES;
        String fileSignature = getFileSignature(inputStream, bytesToRead);
        return fileSignature.startsWith(expectedSignature);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte currentByte : bytes) {
            stringBuilder.append(String.format("%02X", currentByte));
        }
        return stringBuilder.toString();
    }
}
