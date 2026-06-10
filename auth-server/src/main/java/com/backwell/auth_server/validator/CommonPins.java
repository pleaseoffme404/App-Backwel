package com.backwell.auth_server.validator;

import java.util.Set;

public final class CommonPins {
    private CommonPins() {}

    public static final Set<String> LIST = Set.of(
            // Fechas y patrones de teclado típicos
            "000000", "123456", "654321", "112233",
            "121212", "123123", "111222", "000111",
            "010101", "020202", "030303", "101010",
            // Años frecuentes como PIN
            "199000", "200000", "199500", "198000",
            // Patrones de teclado numérico
            "147258", "258369", "159357", "753951",
            "963852", "741852", "456789", "789456"
    );
}
