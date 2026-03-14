package com.USWCicrcleLink.server.global.validation.annotation;

import com.USWCicrcleLink.server.global.validation.validator.ClubRoomNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ClubRoomNumberValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidClubRoomNumber {
    String message() default "유효하지 않은 동아리방입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
