package com.USWCicrcleLink.server.global.validation.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClubRoomNumberValidatorTest {

    private final ClubRoomNumberValidator validator = new ClubRoomNumberValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @Nested
    class isValid_테스트 {

        @Test
        void null이면_false를_반환한다() {
            boolean result = validator.isValid(null, context);

            assertThat(result).isFalse();
        }

        @Test
        void 빈_문자열이면_false를_반환한다() {
            boolean result = validator.isValid("   ", context);

            assertThat(result).isFalse();
        }

        @Test
        void 앞뒤_공백이_있어도_trim_후_유효하면_true를_반환한다() {
            boolean result = validator.isValid("  B101  ", context);

            assertThat(result).isTrue();
        }

        @Test
        void 유효한_B동_호실이면_true를_반환한다() {
            boolean result = validator.isValid("B101", context);

            assertThat(result).isTrue();
        }

        @Test
        void 유효한_일반_호실이면_true를_반환한다() {
            boolean result = validator.isValid("102", context);

            assertThat(result).isTrue();
        }

        @Test
        void VALID_ROOMS에_없는_B동_호실이면_false를_반환한다() {
            boolean result = validator.isValid("B999", context);

            assertThat(result).isFalse();
        }

        @Test
        void VALID_ROOMS에_없는_일반_호실이면_false를_반환한다() {
            boolean result = validator.isValid("999", context);

            assertThat(result).isFalse();
        }

        @Test
        void 형식은_맞지만_목록에_없는_호실이면_false를_반환한다() {
            boolean result = validator.isValid("B109", context);

            assertThat(result).isFalse();
        }
    }
}
