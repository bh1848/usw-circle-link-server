package com.USWCicrcleLink.server.global.validation.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FileSignatureValidator {

    // 파일 시그니처 정의 (확장)
    private static final Map<String, String> FILE_SIGNATURES = new HashMap<>();

    static {
        FILE_SIGNATURES.put("jpg", "FFD8FF");      // JPG
        FILE_SIGNATURES.put("jpeg", "FFD8FF");     // JPEG
        FILE_SIGNATURES.put("png", "89504E47");    // PNG (8바이트가 필요)
        FILE_SIGNATURES.put("xls", "D0CF11E0");       // XLS (Excel 97-2003)
        FILE_SIGNATURES.put("xlsx", "504B0304");      // XLSX (Excel 2007 이상, ZIP 기반)
    }

    public static String getFileSignature(InputStream inputStream, int bytesToRead) throws IOException {
        byte[] fileHeader = new byte[bytesToRead];
        inputStream.read(fileHeader, 0, bytesToRead);
        return bytesToHex(fileHeader);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static boolean isValidFileType(InputStream inputStream, String expectedExtension) throws IOException {
        String expectedSignature = FILE_SIGNATURES.get(expectedExtension.toLowerCase());

        if (expectedSignature == null) {
            return false;
        }

        // 파일 시그니처가 확장자에 따라 필요한 바이트 수에 맞게 읽어옴
        int bytesToRead = expectedExtension.equalsIgnoreCase("png") ? 8 : 4;  // PNG는 8바이트가 필요함
        String fileSignature = getFileSignature(inputStream, bytesToRead);

        // 파일 시그니처가 시작 부분만 일치해도 허용
        return fileSignature.startsWith(expectedSignature);
    }
}