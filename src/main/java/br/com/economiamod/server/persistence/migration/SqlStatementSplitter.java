package br.com.economiamod.server.persistence.migration;

import java.util.ArrayList;
import java.util.List;

/** Splits migration scripts without breaking semicolons inside quoted literals/comments/dollar blocks. */
final class SqlStatementSplitter {
    private SqlStatementSplitter() {
    }

    static List<String> split(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean single = false;
        boolean quotedIdentifier = false;
        boolean lineComment = false;
        boolean blockComment = false;
        String dollarTag = null;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (lineComment) {
                current.append(ch);
                if (ch == '\n') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                current.append(ch);
                if (ch == '*' && next == '/') {
                    current.append(next);
                    i++;
                    blockComment = false;
                }
                continue;
            }
            if (dollarTag != null) {
                if (sql.startsWith(dollarTag, i)) {
                    current.append(dollarTag);
                    i += dollarTag.length() - 1;
                    dollarTag = null;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (!single && !quotedIdentifier && ch == '-' && next == '-') {
                current.append(ch).append(next);
                i++;
                lineComment = true;
                continue;
            }
            if (!single && !quotedIdentifier && ch == '/' && next == '*') {
                current.append(ch).append(next);
                i++;
                blockComment = true;
                continue;
            }
            if (!single && !quotedIdentifier && ch == '$') {
                int end = sql.indexOf('$', i + 1);
                if (end >= 0) {
                    String candidate = sql.substring(i, end + 1);
                    if (candidate.matches("\\$[A-Za-z_][A-Za-z0-9_]*\\$|\\$\\$")) {
                        dollarTag = candidate;
                        current.append(candidate);
                        i = end;
                        continue;
                    }
                }
            }
            if (!quotedIdentifier && ch == '\'' && (i == 0 || sql.charAt(i - 1) != '\\')) {
                if (single && next == '\'') {
                    current.append(ch).append(next);
                    i++;
                    continue;
                }
                single = !single;
                current.append(ch);
                continue;
            }
            if (!single && ch == '"') {
                if (quotedIdentifier && next == '"') {
                    current.append(ch).append(next);
                    i++;
                    continue;
                }
                quotedIdentifier = !quotedIdentifier;
                current.append(ch);
                continue;
            }
            if (!single && !quotedIdentifier && ch == ';') {
                add(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        add(statements, current);
        return List.copyOf(statements);
    }

    private static void add(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }
}
