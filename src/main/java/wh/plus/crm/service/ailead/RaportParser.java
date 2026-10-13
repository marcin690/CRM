package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsuje Markdown z pola {@code raport} workflow Dify „WH PLUS Leady" na listę
 * obiektów w kształcie, którego oczekuje {@link LeadCandidateProcessor}
 * (nazwa, miasto, wojewodztwo, rodzaj, punkty, przetarg, sygnaly[], szczegoly{}).
 *
 * <p>Format jest deterministyczny — generuje go węzeł „Zbuduj raport":
 * <pre>
 * ## Zapytania ofertowe i przetargi
 * ### 1. Nazwa obiektu (Miasto, województwo)  ·  55.0 pkt
 * Typ: hotel · Inwestor: X · Operator: Y · Pokoje: 120
 *
 * - 2026-10-13 · Zapytanie ofertowe (Zwiadowca) · opis…  **Termin ofert: 2026-10-20** [zrodlo](url)
 * </pre>
 */
final class RaportParser {

    private static final String DOT = "·"; // „·" (middle dot U+00B7)

    private static final Pattern HEAD = Pattern.compile(
            "^###\\s+\\d+\\.\\s+(.*?)\\s+" + DOT + "\\s+([0-9]+(?:\\.[0-9]+)?)\\s+pkt\\s*$");
    private static final Pattern GDZIE = Pattern.compile("^(.*?)\\s+\\((.*?)\\)\\s*$");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)\\]\\(([^)]*)\\)\\s*$");
    private static final Pattern TERMIN = Pattern.compile("\\*\\*Termin ofert:\\s*(.*?)\\*\\*");
    private static final String SEP = "\\s+" + DOT + "\\s+";

    /** Etykieta w raporcie → klucz w szczegoly (zgodnie z POLA w węźle Dify). */
    private static final Map<String, String> DET_KEYS = Map.ofEntries(
            Map.entry("Inwestor", "inwestor"), Map.entry("Operator", "operator"),
            Map.entry("Wykonawca", "wykonawca"), Map.entry("Architekt", "architekt"),
            Map.entry("Pokoje", "liczba_pokoi"), Map.entry("Otwarcie", "termin_otwarcia"),
            Map.entry("Etap", "etap"), Map.entry("Nr", "numer"), Map.entry("Branże", "branze"),
            Map.entry("Koszt kontaktu (pkt)", "koszt_punktow"), Map.entry("Zleca", "zlecajacy"),
            Map.entry("Sektor", "sektor"), Map.entry("Prace od", "termin_prac"));

    private RaportParser() {}

    static List<JsonNode> parse(String markdown, ObjectMapper mapper) {
        List<JsonNode> out = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) return out;

        boolean sectionPrzetarg = true;
        ObjectNode current = null;
        boolean detailSeen = false;

        for (String raw : markdown.split("\n")) {
            String line = raw.strip();

            if (line.startsWith("## ")) {
                String low = line.toLowerCase();
                if (low.contains("inwestycj") || low.contains("sygna")) sectionPrzetarg = false;
                else if (low.contains("przetarg") || low.contains("zapytan")) sectionPrzetarg = true;
                current = null;
                continue;
            }
            if (line.startsWith("---") || line.startsWith("# ")) {
                current = null;
                continue;
            }

            Matcher h = HEAD.matcher(line);
            if (h.matches()) {
                current = mapper.createObjectNode();
                detailSeen = false;
                String namePart = h.group(1).trim();
                String miasto = "", woj = "";
                Matcher g = GDZIE.matcher(namePart);
                if (g.matches()) {
                    namePart = g.group(1).trim();
                    String[] gp = g.group(2).split(",");
                    miasto = gp.length > 0 ? gp[0].trim() : "";
                    woj = gp.length > 1 ? gp[1].trim() : "";
                }
                current.put("nazwa", namePart);
                current.put("miasto", miasto);
                current.put("wojewodztwo", woj);
                current.put("rodzaj", "");
                try { current.put("punkty", Double.parseDouble(h.group(2))); }
                catch (NumberFormatException e) { current.put("punkty", 0); }
                current.put("przetarg", sectionPrzetarg);
                current.putArray("sygnaly");
                current.putObject("szczegoly");
                out.add(current);
                continue;
            }

            if (current == null || line.isEmpty()) continue;

            if (line.startsWith("- ")) {
                ((ArrayNode) current.get("sygnaly")).add(parseSignal(line.substring(2).trim(), mapper));
            } else if (!detailSeen) {
                detailSeen = true;
                parseDetails(line, current);
            }
        }
        return out;
    }

    private static ObjectNode parseSignal(String s, ObjectMapper mapper) {
        ObjectNode sig = mapper.createObjectNode();
        String rest = s;

        Matcher lk = LINK.matcher(rest);
        if (lk.find()) {
            String zrodlo = lk.group(1).trim();
            if (!zrodlo.isEmpty()) sig.put("zrodlo", zrodlo);
            sig.put("url", lk.group(2).trim());
            rest = rest.substring(0, lk.start()).trim();
        }
        Matcher tm = TERMIN.matcher(rest);
        if (tm.find()) {
            sig.put("termin", tm.group(1).trim());
            rest = (rest.substring(0, tm.start()) + rest.substring(tm.end())).trim();
        }

        String[] parts = rest.split(SEP, 3);
        String data = parts.length > 0 ? parts[0].trim() : "";
        if (!data.isEmpty() && !data.equalsIgnoreCase("bez daty")) sig.put("data_publikacji", data);
        if (parts.length > 1) sig.put("etykieta", parts[1].trim());
        if (parts.length > 2) sig.put("opis", parts[2].trim());
        return sig;
    }

    private static void parseDetails(String line, ObjectNode current) {
        ObjectNode szcz = (ObjectNode) current.get("szczegoly");
        for (String piece : line.split(SEP)) {
            int idx = piece.indexOf(':');
            if (idx <= 0) continue;
            String label = piece.substring(0, idx).trim();
            String value = piece.substring(idx + 1).trim();
            if (value.isEmpty()) continue;
            if (label.equals("Typ")) current.put("rodzaj", value);
            else {
                String key = DET_KEYS.get(label);
                if (key != null) szcz.put(key, value);
            }
        }
    }
}
