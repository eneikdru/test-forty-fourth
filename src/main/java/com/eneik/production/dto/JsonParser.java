package com.eneik.production.dto;

import java.util.HashMap;
import java.util.Map;

public class JsonParser {
    public static Map<String, String> parse(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }
        json = json.trim();

        int i = 0;
        int len = json.length();
        while (i < len) {
            // skip whitespace/commas
            while (i < len && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) {
                i++;
            }
            if (i >= len) break;

            // read key
            String key = null;
            if (json.charAt(i) == '"') {
                i++;
                int start = i;
                while (i < len && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < len) {
                        i += 2;
                    } else {
                        i++;
                    }
                }
                key = unescape(json.substring(start, i));
                if (i < len) i++; // skip ending '"'
            } else {
                // unquoted key
                int start = i;
                while (i < len && !Character.isWhitespace(json.charAt(i)) && json.charAt(i) != ':') {
                    i++;
                }
                key = json.substring(start, i);
            }

            // skip to colon
            while (i < len && json.charAt(i) != ':') {
                i++;
            }
            if (i < len) i++; // skip colon

            // skip whitespace
            while (i < len && Character.isWhitespace(json.charAt(i))) {
                i++;
            }

            // read value
            String value = null;
            if (i < len && json.charAt(i) == '"') {
                i++;
                int start = i;
                while (i < len && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < len) {
                        i += 2;
                    } else {
                        i++;
                    }
                }
                value = unescape(json.substring(start, i));
                if (i < len) i++; // skip ending '"'
            } else if (i < len && json.charAt(i) == 'n' && i + 3 < len && json.substring(i, i + 4).equals("null")) {
                value = null;
                i += 4;
            } else {
                // unquoted value (number, boolean, or null)
                int start = i;
                while (i < len && json.charAt(i) != ',' && !Character.isWhitespace(json.charAt(i)) && json.charAt(i) != '}') {
                    i++;
                }
                String rawVal = json.substring(start, i);
                if ("null".equals(rawVal)) {
                    value = null;
                } else {
                    value = rawVal;
                }
            }

            if (key != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 5 < len) {
                            String hex = s.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } else {
                            sb.append(next);
                        }
                        break;
                    default: sb.append(next);
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
