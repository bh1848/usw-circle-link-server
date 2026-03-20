package com.USWCicrcleLink.server.global.validation.validator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = FileSignatureValidatorPerformanceTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class FileSignatureValidatorPerformanceTest {

    private static final int WARMUP_COUNT = 100;
    private static final int MEASUREMENT_COUNT = 1000;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private static final byte[] JPG_MAGIC_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F'
    };
    private static final byte[] PNG_MAGIC_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00
    };

    @Autowired
    private FileSignatureValidator fileSignatureValidator;

    @Test
    void 확장자_검사와_매직_바이트_검사의_응답_시간을_비교한다() throws Exception {
        List<MockMultipartFile> files = List.of(
                createJpgFile(),
                createPngFile(),
                createSpoofedJpgFile()
        );

        assertValidationCases(files);

        double extensionOnlyAverage = measureAverageMillis(files, this::validateExtensionOnly);
        double extensionAndMagicAverage = measureAverageMillis(files, this::validateExtensionAndMagic);
        double overhead = extensionAndMagicAverage - extensionOnlyAverage;

        System.out.printf(Locale.US, "확장자 검사만: %.3f ms%n", extensionOnlyAverage);
        System.out.printf(Locale.US, "매직 바이트 검사 포함: %.3f ms%n", extensionAndMagicAverage);
        System.out.printf(Locale.US, "오버헤드: %.3f ms%n", overhead);
    }

    private void assertValidationCases(List<MockMultipartFile> files) throws IOException {
        MockMultipartFile validJpg = files.get(0);
        MockMultipartFile validPng = files.get(1);
        MockMultipartFile spoofedJpg = files.get(2);

        assertThat(validateExtensionOnly(validJpg)).isTrue();
        assertThat(validateExtensionAndMagic(validJpg)).isTrue();

        assertThat(validateExtensionOnly(validPng)).isTrue();
        assertThat(validateExtensionAndMagic(validPng)).isTrue();

        assertThat(validateExtensionOnly(spoofedJpg)).isTrue();
        assertThat(validateExtensionAndMagic(spoofedJpg)).isFalse();
    }

    private double measureAverageMillis(List<MockMultipartFile> files, CheckedFileValidator validator) throws Exception {
        runWarmup(files, validator);

        long totalNanos = 0L;
        int invocationCount = 0;
        int successCount = 0;

        for (int iteration = 0; iteration < MEASUREMENT_COUNT; iteration++) {
            for (MockMultipartFile file : files) {
                long start = System.nanoTime();
                boolean result = validator.validate(file);
                totalNanos += System.nanoTime() - start;
                invocationCount++;
                if (result) {
                    successCount++;
                }
            }
        }

        assertThat(successCount).isGreaterThan(0);
        return totalNanos / 1_000_000.0 / invocationCount;
    }

    private void runWarmup(List<MockMultipartFile> files, CheckedFileValidator validator) throws Exception {
        for (int iteration = 0; iteration < WARMUP_COUNT; iteration++) {
            for (MockMultipartFile file : files) {
                validator.validate(file);
            }
        }
    }

    private boolean validateExtensionOnly(MockMultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        return extension != null && ALLOWED_IMAGE_EXTENSIONS.contains(extension);
    }

    private boolean validateExtensionAndMagic(MockMultipartFile file) throws IOException {
        String extension = extractExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return false;
        }
        return fileSignatureValidator.isValidFileType(file.getInputStream(), extension);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return null;
        }

        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private MockMultipartFile createJpgFile() {
        return new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                JPG_MAGIC_BYTES
        );
    }

    private MockMultipartFile createPngFile() {
        return new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                PNG_MAGIC_BYTES
        );
    }

    private MockMultipartFile createSpoofedJpgFile() {
        return new MockMultipartFile(
                "file",
                "spoofed.jpg",
                "image/jpeg",
                PNG_MAGIC_BYTES
        );
    }

    @FunctionalInterface
    private interface CheckedFileValidator {
        boolean validate(MockMultipartFile file) throws Exception;
    }

    @SpringBootConfiguration
    @Import(FileSignatureValidator.class)
    static class TestConfiguration {
    }
}
