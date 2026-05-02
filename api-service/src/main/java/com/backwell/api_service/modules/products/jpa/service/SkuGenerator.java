package com.backwell.api_service.modules.products.jpa.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

@Component
public class SkuGenerator {

    public String generateSku(String productName, Map<String, String> attributes) {
        String[] words = productName.split("\\s+");
        StringBuilder namePart = new StringBuilder();

        for (int i = 0; i < Math.min(words.length, 3); i++) {
            String word = words[i].toUpperCase().replaceAll("[^A-Z]", "");

            if (i == 0) {
                // First word: Just take up to 5 chars for context
                namePart.append(word, 0, Math.min(word.length(), 5));
            } else {
                // Second and Third words: Apply CVC pattern
                namePart.append("-").append(extractCvc(word));
            }
        }

        String attrString = attributes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getValue().toUpperCase().replaceAll("[^A-Z0-9]", ""))
                .collect(Collectors.joining("-"));

        String checksum = getChecksum(productName + attributes.toString());

        return String.format("%s-%s-%s", namePart, attrString, checksum);
    }

    private String extractCvc(String word) {
        Pattern cvcPattern = Pattern.compile("[BCDFGHJKLMNPQRSTVWXYZ][AEIOU][BCDFGHJKLMNPQRSTVWXYZ]");
        Matcher matcher = cvcPattern.matcher(word);

        if (matcher.find()) {
            return matcher.group();
        }
        return word.substring(0, Math.min(word.length(), 3));
    }

    private String getChecksum(String input) {
        CRC32 crc = new CRC32();
        crc.update(input.getBytes());
        return Long.toHexString(crc.getValue()).toUpperCase();
    }
}