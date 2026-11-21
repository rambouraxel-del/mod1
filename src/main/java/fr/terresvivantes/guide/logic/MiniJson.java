package fr.terresvivantes.guide.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecteur JSON minimal, en Java pur (aucune bibliothèque), pour que la logique du guide reste testable hors Minecraft.
 * Renvoie {@link Map} (objets), {@link List} (tableaux), {@link String}, {@link Double}, {@link Boolean} ou {@code null}.
 */
final class MiniJson {

    private final String text;
    private int pos;

    private MiniJson(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        MiniJson parser = new MiniJson(text);
        parser.skipWhitespace();
        Object value = parser.value();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("texte après la valeur");
        }
        return value;
    }

    private Object value() {
        skipWhitespace();
        if (pos >= text.length()) {
            throw error("valeur attendue");
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipWhitespace();
        if (peekIs('}')) {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (!peekIs('"')) {
                throw error("clé attendue");
            }
            String key = string();
            skipWhitespace();
            expect(':');
            map.put(key, value());
            skipWhitespace();
            if (peekIs(',')) {
                pos++;
            } else {
                expect('}');
                return map;
            }
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (peekIs(']')) {
            pos++;
            return list;
        }
        while (true) {
            list.add(value());
            skipWhitespace();
            if (peekIs(',')) {
                pos++;
            } else {
                expect(']');
                return list;
            }
        }
    }

    private String string() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw error("guillemet fermant manquant");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (pos >= text.length()) {
                throw error("échappement incomplet");
            }
            char e = text.charAt(pos++);
            switch (e) {
                case '"', '\\', '/' -> out.append(e);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw error("\\u incomplet");
                    }
                    out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw error("échappement inconnu \\" + e);
            }
        }
    }

    private Double number() {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        if (start == pos) {
            throw error("valeur inattendue");
        }
        try {
            return Double.parseDouble(text.substring(start, pos));
        } catch (NumberFormatException badNumber) {
            throw error("nombre invalide");
        }
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, pos)) {
            throw error("valeur inattendue");
        }
        pos += word.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private boolean peekIs(char expected) {
        return pos < text.length() && text.charAt(pos) == expected;
    }

    private void expect(char expected) {
        if (!peekIs(expected)) {
            throw error("« " + expected + " » attendu");
        }
        pos++;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON invalide à la position " + pos + " : " + message);
    }
}
