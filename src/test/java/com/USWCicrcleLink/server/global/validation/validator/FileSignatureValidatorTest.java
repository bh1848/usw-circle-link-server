package com.USWCicrcleLink.server.global.validation.validator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FileSignatureValidatorTest {

    private static final byte[] JPG_SIGNATURE = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0
    };
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] PNG_SHORT_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47
    };
    private static final byte[] XLSX_SIGNATURE = new byte[]{
            0x50, 0x4B, 0x03, 0x04
    };
    private static final byte[] INVALID_SIGNATURE = new byte[]{
            0x11, 0x22, 0x33, 0x44
    };

    private final FileSignatureValidator fileSignatureValidator = new FileSignatureValidator();

    @Nested
    class isValidFileType_테스트 {

        @Test
        void jpg_매직_바이트가_정상이면_true를_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(JPG_SIGNATURE);

            boolean result = fileSignatureValidator.isValidFileType(inputStream, "jpg");

            assertThat(result).isTrue();
        }

        @Test
        void jpeg_매직_바이트가_정상이면_true를_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(JPG_SIGNATURE);

            boolean result = fileSignatureValidator.isValidFileType(inputStream, "jpeg");

            assertThat(result).isTrue();
        }

        @Test
        void png_매직_바이트가_정상이면_true를_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(PNG_SIGNATURE);

            boolean result = fileSignatureValidator.isValidFileType(inputStream, "png");

            assertThat(result).isTrue();
        }

        @Test
        void png에_4바이트만_주면_false를_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(PNG_SHORT_SIGNATURE);

            boolean result = fileSignatureValidator.isValidFileType(inputStream, "png");

            assertThat(result).isFalse();
        }

        @Test
        void xlsx_매직_바이트가_정상이면_true를_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(XLSX_SIGNATURE);

            boolean result = fileSignatureValidator.isValidFileType(inputStream, "xlsx");

            assertThat(result).isTrue();
        }

        @Test
        void 지원하지_않는_확장자면_false를_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(JPG_SIGNATURE);

            boolean result = fileSignatureValidator.isValidFileType(inputStream, "pdf");

            assertThat(result).isFalse();
        }

        @Test
        void 변조된_파일이면_false를_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(INVALID_SIGNATURE);

            boolean result = fileSignatureValidator.isValidFileType(inputStream, "jpg");

            assertThat(result).isFalse();
        }
    }

    @Nested
    class getFileSignature_테스트 {

        @Test
        void 바이트_수보다_짧은_스트림이면_빈_문자열을_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(PNG_SHORT_SIGNATURE);

            String signature = fileSignatureValidator.getFileSignature(inputStream, 8);

            assertThat(signature).isEmpty();
        }

        @Test
        void 정상_바이트를_읽으면_16진수_문자열을_반환한다() throws IOException {
            InputStream inputStream = new ByteArrayInputStream(XLSX_SIGNATURE);

            String signature = fileSignatureValidator.getFileSignature(inputStream, 4);

            assertThat(signature).isEqualTo("504B0304");
        }
    }
}
