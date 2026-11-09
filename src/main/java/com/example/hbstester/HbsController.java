package com.example.hbstester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RestController
public class HbsController {

    private static final String WORKING_DIR   = System.getProperty("user.dir");
    private static final String TEMPLATE_FILE = WORKING_DIR + File.separator + "template.html";
    private static final String PAYLOAD_FILE  = WORKING_DIR + File.separator + "payload.json";
    private static final String HELPERS_FILE  = WORKING_DIR + File.separator + "customhandlebar.js";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    @GetMapping("/api/load")
    public ResponseEntity<Map<String, String>> loadFiles() {
        Map<String, String> result = new HashMap<>();
        result.put("template", readFile(TEMPLATE_FILE, readResource("samples/template.html")));
        result.put("payload",  readFile(PAYLOAD_FILE,  readResource("samples/payload.json")));
        result.put("helpers",  readFile(HELPERS_FILE,  readResource("samples/customhandlebar.js")));
        return ResponseEntity.ok(result);
    }

    /** Built-in example template, payload and helpers (used by the "Load Sample" button). */
    @GetMapping("/api/sample")
    public ResponseEntity<Map<String, String>> sample() {
        Map<String, String> result = new HashMap<>();
        result.put("template", readResource("samples/template.html"));
        result.put("payload",  readResource("samples/payload.json"));
        result.put("helpers",  readResource("samples/customhandlebar.js"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/save")
    public ResponseEntity<Map<String, String>> saveFiles(@RequestBody RenderRequest req) {
        Map<String, String> result = new HashMap<>();
        try {
            writeFile(TEMPLATE_FILE, req.getTemplate());
            writeFile(PAYLOAD_FILE,  req.getPayload());
            writeFile(HELPERS_FILE,  req.getHelpers());
            result.put("status", "saved");
        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/render")
    public ResponseEntity<RenderResponse> render(@RequestBody RenderRequest req) {
        String cleanedPayload = null;
        try {
            PayloadResult pr = processPayload(req.getPayload());
            cleanedPayload = pr.cleanedJson;

            Handlebars handlebars = buildHandlebars(req.getHelpers());
            Template template = handlebars.compileInline(req.getTemplate());
            String rendered = template.apply(pr.context);

            return ResponseEntity.ok(RenderResponse.ok(rendered, cleanedPayload));

        } catch (JsonProcessingException e) {
            int line   = e.getLocation() != null ? e.getLocation().getLineNr()   : -1;
            int column = e.getLocation() != null ? e.getLocation().getColumnNr() : -1;
            String msg = "Invalid payload JSON: " + e.getOriginalMessage();
            return ResponseEntity.ok(RenderResponse.error(msg, line, column, null).withSource("payload"));

        } catch (PayloadFormatException e) {
            return ResponseEntity.ok(RenderResponse.error(e.getMessage(), -1, -1, null).withSource("payload"));

        } catch (HandlebarsException e) {
            int line   = e.getError() != null ? e.getError().line   : -1;
            int column = e.getError() != null ? e.getError().column : -1;
            // jknack formats errors as "inline:line:col: message" — extract the message part
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            String[] parts = msg.split(":", 4);
            if (parts.length == 4) {
                msg = parts[3].trim();
                if (line < 0) { try { line   = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) {} }
                if (column < 0){ try { column = Integer.parseInt(parts[2].trim()); } catch (NumberFormatException ignored) {} }
            }
            return ResponseEntity.ok(RenderResponse.error(msg, line, column, cleanedPayload));

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            return ResponseEntity.ok(RenderResponse.error(msg, -1, -1, cleanedPayload));
        }
    }

    // ── Payload Processing ────────────────────────────────────────────────────

    private static class PayloadResult {
        Map<String, Object> context;
        String cleanedJson;
    }

    private static class PayloadFormatException extends Exception {
        PayloadFormatException(String message) { super(message); }
    }

    /**
     * 1. Strip comments / trailing commas (cleanJson)
     * 2. Validate that the result is a JSON object (Jackson reports line/column on failure)
     * 3. Expand any string value that holds a JSON array or object ("[{...}]") into real data
     */
    @SuppressWarnings("unchecked")
    private PayloadResult processPayload(String raw) throws Exception {
        String cleaned = cleanJson(raw == null ? "" : raw);

        Object parsed = objectMapper.readValue(cleaned, Object.class);
        if (!(parsed instanceof Map)) {
            throw new PayloadFormatException("Payload must be a JSON object ({ ... }) at the top level.");
        }

        Map<String, Object> context = (Map<String, Object>) destringify(parsed);

        PayloadResult pr = new PayloadResult();
        pr.context     = context;
        pr.cleanedJson = objectMapper.writeValueAsString(context);
        return pr;
    }

    /**
     * Walks the parsed payload. A string that starts with "[" and ends with "]"
     * (or starts with "{" and ends with "}") is parsed into a real array/object.
     * Anything that does not parse - HTML snippets, plain text, "{{notes}}" - is left untouched.
     */
    private Object destringify(Object value) {
        if (value instanceof String) {
            String s = ((String) value).trim();
            boolean looksJson = (s.startsWith("[") && s.endsWith("]"))
                             || (s.startsWith("{") && s.endsWith("}"));
            if (looksJson) {
                try {
                    return destringify(objectMapper.readValue(s, Object.class));
                } catch (Exception ignored) {
                    // not valid JSON - keep the original string
                }
            }
            return value;
        }
        if (value instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                out.put(String.valueOf(e.getKey()), destringify(e.getValue()));
            }
            return out;
        }
        if (value instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) out.add(destringify(item));
            return out;
        }
        return value;
    }

    // ── Handlebars Setup ──────────────────────────────────────────────────────

    private Handlebars buildHandlebars(String helpersJs) throws Exception {
        Handlebars handlebars = new Handlebars();
        handlebars.registerHelpers(ConditionalHelpers.class);
        StringHelpers.register(handlebars);
        registerJavaHelpers(handlebars);
        if (helpersJs != null && !helpersJs.trim().isEmpty()) {
            handlebars.registerHelpers("customhandlebar.js", helpersJs);
        }
        return handlebars;
    }

    private void registerJavaHelpers(Handlebars handlebars) {
        handlebars.registerHelper("dateFormat", (Helper<Object>) (context, options) -> {
            if (context == null) return "";
            try {
                String fmt = options.param(0, "yyyy-MM-dd");
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt);
                if (context instanceof java.util.Date) {
                    return sdf.format((java.util.Date) context);
                }
                return sdf.format(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(context.toString()));
            } catch (Exception e) { return context.toString(); }
        });

        handlebars.registerHelper("lower", (Helper<Object>) (context, options) ->
            context != null ? context.toString().toLowerCase() : "");

        handlebars.registerHelper("upper", (Helper<Object>) (context, options) ->
            context != null ? context.toString().toUpperCase() : "");

        handlebars.registerHelper("base64Decode", (Helper<Object>) (context, options) -> {
            if (context == null) return "";
            try {
                return new String(Base64.getDecoder().decode(context.toString()), StandardCharsets.UTF_8);
            } catch (Exception e) { return context.toString(); }
        });
    }

    // ── JSON Cleaning ─────────────────────────────────────────────────────────

    private String cleanJson(String src) {
        StringBuilder out = new StringBuilder();
        int i = 0, len = src.length();
        while (i < len) {
            char ch = src.charAt(i);
            if (ch == '"') {
                out.append(ch); i++;
                while (i < len) {
                    char sc = src.charAt(i);
                    if (sc == '\\') {
                        out.append(sc);
                        if (i + 1 < len) { out.append(src.charAt(i + 1)); i++; }
                        i++; continue;
                    }
                    if (sc == '"') { out.append(sc); i++; break; }
                    out.append(sc); i++;
                }
                continue;
            }
            if (ch == '/' && i + 1 < len && src.charAt(i + 1) == '/') {
                while (i < len && src.charAt(i) != '\n') i++;
                continue;
            }
            if (ch == '/' && i + 1 < len && src.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < len && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) {
                    if (src.charAt(i) == '\n') out.append('\n');
                    i++;
                }
                i += 2; continue;
            }
            if (ch == ',') {
                int j = i + 1;
                while (j < len && " \t\n\r".indexOf(src.charAt(j)) >= 0) j++;
                if (j < len && (src.charAt(j) == '}' || src.charAt(j) == ']')) { i++; continue; }
            }
            out.append(ch); i++;
        }
        return out.toString();
    }

    // ── File I/O ──────────────────────────────────────────────────────────────

    private String readResource(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { return ""; }
    }

    private String readFile(String path, String defaultValue) {
        try {
            File f = new File(path);
            if (!f.exists()) return defaultValue;
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) { return defaultValue; }
    }

    private void writeFile(String path, String content) throws IOException {
        if (content == null) return;
        Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
    }
}
