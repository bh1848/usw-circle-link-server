package com.USWCicrcleLink.server.global.validation.validator;

import com.USWCicrcleLink.server.global.validation.annotation.ValidClubRoomNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.regex.Pattern;

public class ClubRoomNumberValidator implements ConstraintValidator<ValidClubRoomNumber, String> {

    // 허용된 동아리방 목록 (B109, 111, 204 제외)
    private static final Set<String> VALID_ROOMS = Set.of(
            // 지하
            "B101", "B102", "B103", "B104", "B105", "B106", "B107", "B108",
            "B110", "B111", "B112", "B113", "B114", "B115", "B116", "B117", "B118",
            "B119", "B120", "B121", "B122", "B123",
            // 1층
            "102", "103", "104", "105", "106", "107", "108", "109", "110", "112",
            // 2층
            "203", "205", "206", "207", "208", "209", "210"
    );

    // B + 숫자 3자리 또는 숫자 3자리만 허용
    private static final Pattern ROOM_PATTERN = Pattern.compile("^(B\\d{3}|\\d{3})$");

    @Override
    public boolean isValid(String clubRoomNumber, ConstraintValidatorContext context) {
        if (clubRoomNumber == null || clubRoomNumber.trim().isEmpty()) {
            return false; // 필수 값 처리
        }

        // B000 또는 000 형태만 허용
        if (!ROOM_PATTERN.matcher(clubRoomNumber).matches()) {
            return false;
        }

        // 허용된 방 목록에 포함되는지 확인
        return VALID_ROOMS.contains(clubRoomNumber);
    }
}

