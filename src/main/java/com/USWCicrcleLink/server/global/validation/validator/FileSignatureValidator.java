package com.USWCicrcleLink.server.global.validation.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class FileSignatureValidator {
    private static final int DEFAULT_SIGNATURE_BYTES = 4;
    private static final int PNG_SIGNATURE_BYTES = 8;
    private static final Map<String, String> FILE_SIGNATURES = Map.of(
            "jpg", "FFD8FF",
            "jpeg", "FFD8FF",
            "png", "89504E47",
            "xls", "D0CF11E0",
            "xlsx", "504B0304"
    );

    private FileSignatureValidator() {
    }

    public static String getFileSignature(InputStream inputStream, int bytesToRead) throws IOException {
        byte[] fileHeader = inputStream.readNBytes(bytesToRead);
        if (fileHeader.length < bytesToRead) {
            return "";
        }
        return bytesToHex(fileHeader);
    }

    public static boolean isValidFileType(InputStream inputStream, String expectedExtension) throws IOException {
        String expectedSignature = FILE_SIGNATURES.get(expectedExtension.toLowerCase());
        if (expectedSignature == null) {
            return false;
        }

        int bytesToRead = expectedExtension.equalsIgnoreCase("png") ? PNG_SIGNATURE_BYTES : DEFAULT_SIGNATURE_BYTES;
        String fileSignature = getFileSignature(inputStream, bytesToRead);
        return fileSignature.startsWith(expectedSignature);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte currentByte : bytes) {
            stringBuilder.append(String.format("%02X", currentByte));
        }
        return stringBuilder.toString();
    }
}
