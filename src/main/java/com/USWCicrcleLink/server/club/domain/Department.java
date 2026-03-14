package com.USWCicrcleLink.server.club.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.stream.Stream;

public enum Department {
    ACADEMIC("학술"),
    RELIGION("종교"),
    ART("예술"),
    SPORT("체육"),
    SHOW("공연"),
    VOLUNTEER("봉사");

    private final String value;

    Department(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Department from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("학부 값은 필수입니다.");
        }

        return Stream.of(Department.values())
                .filter(type -> type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 학부 값입니다."));
    }
}
