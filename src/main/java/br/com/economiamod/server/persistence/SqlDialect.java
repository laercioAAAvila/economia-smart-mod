package br.com.economiamod.server.persistence;

import java.util.regex.Pattern;

public final class SqlDialect {
    private static final Pattern FOR_UPDATE = Pattern.compile("\\s+FOR\\s+UPDATE(?:\\s+OF\\s+[A-Za-z0-9_.,\\s]+)?(?=\\s*(?:LIMIT\\s+\\d+)?\\s*$)", Pattern.CASE_INSENSITIVE);

    private SqlDialect() {
    }

    public static String adapt(String sql, DatabaseEngine engine) {
        if (engine != DatabaseEngine.SQLITE || sql == null || sql.isBlank()) {
            return sql;
        }
        String adapted = FOR_UPDATE.matcher(sql).replaceAll("");
        adapted = adapted.replaceAll("(?i)\\bLEAST\\s*\\(", "MIN(");
        adapted = adapted.replaceAll("(?i)\\bGREATEST\\s*\\(", "MAX(");
        return adapted;
    }
}
