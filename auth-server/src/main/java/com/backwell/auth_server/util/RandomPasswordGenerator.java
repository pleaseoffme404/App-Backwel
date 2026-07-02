package com.backwell.auth_server.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RandomPasswordGenerator {
    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBER = "0123456789";
    private static final String OTHER_CHAR = "!@#$%&*()_+-=[]|,./?><";

    private final SecureRandom random = new SecureRandom();

    public String generatePassword(int length) {
        return generatePassword(length, true, true, true);
    }

    public String generatePassword(int length, boolean numbers, boolean letters, boolean special) {
        if (length < 1) throw new IllegalArgumentException("La longitud debe ser al menos 1.");

        StringBuilder passwordBuilder = new StringBuilder();
        StringBuilder allowChars = new StringBuilder();

        List<Character> mandatoryChars = new ArrayList<>();

        if (letters) {
            allowChars.append(CHAR_LOWER).append(CHAR_UPPER);
            mandatoryChars.add(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));
            mandatoryChars.add(CHAR_UPPER.charAt(random.nextInt(CHAR_UPPER.length())));
        }
        if (numbers) {
            allowChars.append(NUMBER);
            mandatoryChars.add(NUMBER.charAt(random.nextInt(NUMBER.length())));
        }
        if (special) {
            allowChars.append(OTHER_CHAR);
            mandatoryChars.add(OTHER_CHAR.charAt(random.nextInt(OTHER_CHAR.length())));
        }

        if (allowChars.isEmpty()) {
            allowChars.append(CHAR_LOWER).append(CHAR_UPPER);
            mandatoryChars.add(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));
        }

        int remainingLength = length - mandatoryChars.size();
        for (int i = 0; i < remainingLength; i++) {
            int rndCharAt = random.nextInt(allowChars.length());
            passwordBuilder.append(allowChars.charAt(rndCharAt));
        }

        for (char c : mandatoryChars) {
            passwordBuilder.append(c);
        }

        List<Character> finalPasswordChars = new ArrayList<>();
        for (char c : passwordBuilder.toString().toCharArray()) {
            finalPasswordChars.add(c);
        }
        Collections.shuffle(finalPasswordChars, random);

        StringBuilder finalPassword = new StringBuilder();
        for (char c : finalPasswordChars) {
            finalPassword.append(c);
        }

        return finalPassword.toString();
    }
}