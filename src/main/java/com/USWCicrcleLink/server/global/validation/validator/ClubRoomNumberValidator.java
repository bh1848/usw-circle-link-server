package com.USWCicrcleLink.server.global.validation.validator;

import com.USWCicrcleLink.server.global.validation.annotation.ValidClubRoomNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class ClubRoomNumberValidator implements ConstraintValidator<ValidClubRoomNumber, String> {
    private static final Set<String> VALID_ROOMS = Set.of(
            "B101", "B102", "B103", "B104", "B105", "B106", "B107", "B108",
            "B110", "B111", "B112", "B113", "B114", "B115", "B116", "B117", "B118",
            "B119", "B120", "B121", "B122", "B123",
            "102", "103", "104", "105", "106", "107", "108", "109", "110", "112",
            "203", "205", "206", "207", "208", "209", "210"
    );
    private static final Pattern ROOM_PATTERN = Pattern.compile("^(B\\d{3}|\\d{3})$");

    @Override
    public boolean isValid(String clubRoomNumber, ConstraintValidatorContext context) {
        if (clubRoomNumber == null) {
            return false;
        }

        String normalizedClubRoomNumber = clubRoomNumber.trim();
        if (normalizedClubRoomNumber.isEmpty()) {
            return false;
        }

        return ROOM_PATTERN.matcher(normalizedClubRoomNumber).matches()
                && VALID_ROOMS.contains(normalizedClubRoomNumber);
    }
}
