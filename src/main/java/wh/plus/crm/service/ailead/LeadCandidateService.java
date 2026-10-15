package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import wh.plus.crm.dto.lead.LeadCandidateDto;
import wh.plus.crm.dto.lead.LeadCandidateRunDto;
import wh.plus.crm.dto.lead.LeadDTO;
import wh.plus.crm.model.lead.*;
import wh.plus.crm.repository.LeadCandidateRepository;
import wh.plus.crm.repository.LeadCandidateRunRepository;
import wh.plus.crm.repository.LeadSourceRepository;
import wh.plus.crm.service.LeadService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Logika zakładki „Automatyczne leady z rynku": uruchamianie przebiegu agenta
 * (async), listowanie kandydatów oraz decyzje handlowca (Dodaj / Odrzuć).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeadCandidateService {

    private static final String LEAD_SOURCE_NAME = "Agent AI (rynek)";

    /** Rodzaje obiektów mapowane na typ klienta „Hotel sieciowy". */
    private static final Set<String> HOSPITALITY = Set.of(
            "hotel", "aparthotel", "pensjonat", "osrodek", "sanatorium",
            "akademik", "dom_opieki", "hostel", "kurort", "resort");

    private final LeadCandidateRepository candidateRepository;
    private final LeadCandidateRunRepository runRepository;
    private final LeadCandidateProcessor processor;
    private final LeadService leadService;
    private final LeadSourceRepository leadSourceRepository;
    private final ObjectMapper objectMapper;

    // --- Przebieg agenta ---

    @Transactional
    public LeadCandidateRunDto startRun(String tryb, Integer dniNewsy, Integer dniPrzetargi, String user) {
        LeadCandidateRun run = new LeadCandidateRun();
        run.setTryb(tryb == null || tryb.isBlank() ? "wszystko" : tryb.trim());
        run.setDniNewsy(dniNewsy == null ? 14 : dniNewsy);
        run.setDniPrzetargi(dniPrzetargi == null ? 7 : dniPrzetargi);
        run.setStatus(LeadCandidateRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setStartedBy(user);
        LeadCandidateRun saved = runRepository.save(run);

        Long runId = saved.getId();
        runAfterCommit(() -> processor.process(runId));
        return toRunDto(saved);
    }

    @Transactional(readOnly = true)
    public LeadCandidateRunDto latestRun() {
        return runRepository.findFirstByOrderByIdDesc().map(this::toRunDto).orElse(null);
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    // --- Kandydaci ---

    @Transactional(readOnly = true)
    public List<LeadCandidateDto> list(LeadCandidateStatus status) {
        LeadCandidateStatus s = status == null ? LeadCandidateStatus.NEW : status;
        return candidateRepository.findByStatusOrderByPunktyDescIdDesc(s).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countNew() {
        return candidateRepository.countByStatus(LeadCandidateStatus.NEW);
    }

    /** Dodaj: tworzy wersję roboczą leada z kandydata. @return id utworzonego leada. */
    @Transactional
    public Long accept(Long id, String user) {
        LeadCandidate c = candidateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Kandydat nie istnieje: " + id));
        if (c.getStatus() == LeadCandidateStatus.ACCEPTED && c.getAcceptedLeadId() != null) {
            return c.getAcceptedLeadId(); // idempotentnie
        }

        LeadDTO dto = new LeadDTO();
        dto.setName(c.getNazwa());
        dto.setClientType(mapClientType(c.getRodzaj()));
        dto.setClientCity(c.getMiasto());
        dto.setClientState(c.getWojewodztwo());
        dto.setClientCountry("Polska");
        Long rooms = parseRooms(c.getSzczegolyJson());
        if (rooms != null) dto.setRoomsQuantity(rooms);
        dto.setDescription(buildDescription(c));
        dto.setSourceUrl(c.getZrodloUrl() == null ? null
                : (c.getZrodloUrl().length() > 500 ? c.getZrodloUrl().substring(0, 500) : c.getZrodloUrl()));
        dto.setLeadSourceId(resolveLeadSource().getId());
        dto.setFinal(false);

        LeadDTO saved = leadService.save(dto);

        c.setStatus(LeadCandidateStatus.ACCEPTED);
        c.setDecidedAt(LocalDateTime.now());
        c.setDecidedBy(user);
        c.setAcceptedLeadId(saved.getId());
        candidateRepository.save(c);
        return saved.getId();
    }

    /** Odrzuć: oznacza kandydata jako odrzuconego (nie wróci w kolejnych przebiegach). */
    @Transactional
    public void reject(Long id, String note, String user) {
        LeadCandidate c = candidateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Kandydat nie istnieje: " + id));
        c.setStatus(LeadCandidateStatus.REJECTED);
        c.setDecidedAt(LocalDateTime.now());
        c.setDecidedBy(user);
        if (note != null && !note.isBlank()) {
            c.setRejectionNote(note.length() > 1000 ? note.substring(0, 1000) : note);
        }
        candidateRepository.save(c);
    }

    // --- pomocnicze ---

    private LeadSource resolveLeadSource() {
        return leadSourceRepository.findByName(LEAD_SOURCE_NAME).orElseGet(() -> {
            LeadSource ls = new LeadSource();
            ls.setName(LEAD_SOURCE_NAME);
            ls.setDescription("Lead wygenerowany automatycznie przez agenta AI (przetargi i sygnały z rynku).");
            return leadSourceRepository.save(ls);
        });
    }

    private ClientType mapClientType(String rodzaj) {
        if (rodzaj == null) return null;
        return HOSPITALITY.contains(rodzaj.toLowerCase().trim()) ? ClientType.HOTEL_CHAIN : null;
    }

    private Long parseRooms(String szczegolyJson) {
        try {
            JsonNode n = objectMapper.readTree(szczegolyJson == null ? "{}" : szczegolyJson);
            String raw = n.path("liczba_pokoi").asText("");
            String digits = raw.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? null : Long.valueOf(digits);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildDescription(LeadCandidate c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Automatyczny lead z rynku (agent AI).\n");
        sb.append("Rodzaj: ").append(nvl(c.getRodzaj(), "—"))
          .append(" · Punkty: ").append(c.getPunkty());
        if (c.getMiasto() != null && !c.getMiasto().isBlank()) {
            sb.append(" · ").append(c.getMiasto());
            if (c.getWojewodztwo() != null && !c.getWojewodztwo().isBlank()) {
                sb.append(", ").append(c.getWojewodztwo());
            }
        }

        try {
            JsonNode d = objectMapper.readTree(c.getSzczegolyJson() == null ? "{}" : c.getSzczegolyJson());
            appendDetail(sb, d, "inwestor", "Inwestor");
            appendDetail(sb, d, "operator", "Operator");
            appendDetail(sb, d, "wykonawca", "Wykonawca");
            appendDetail(sb, d, "termin_otwarcia", "Otwarcie");
            appendDetail(sb, d, "etap", "Etap");
        } catch (Exception ignore) { /* opis bez szczegółów */ }

        try {
            JsonNode sygnaly = objectMapper.readTree(c.getSygnalyJson() == null ? "[]" : c.getSygnalyJson());
            if (sygnaly.isArray() && sygnaly.size() > 0) {
                sb.append("\n\nSygnały:");
                for (JsonNode s : sygnaly) {
                    String data = s.path("data_publikacji").asText("");
                    String etykieta = s.path("etykieta").asText(s.path("typ").asText(""));
                    String opis = s.path("opis").asText("");
                    String url = s.path("url").asText("");
                    String termin = s.path("termin").asText("");
                    sb.append("\n- ");
                    if (!data.isBlank()) sb.append(data).append(" · ");
                    if (!etykieta.isBlank()) sb.append(etykieta).append(" · ");
                    sb.append(opis);
                    if (!termin.isBlank()) sb.append("  [Termin ofert: ").append(termin).append("]");
                    if (!url.isBlank()) sb.append(" (").append(url).append(")");
                }
            }
        } catch (Exception ignore) { /* opis bez sygnałów */ }

        return sb.toString();
    }

    private void appendDetail(StringBuilder sb, JsonNode d, String key, String label) {
        String v = d.path(key).asText("");
        if (!v.isBlank()) sb.append("\n").append(label).append(": ").append(v);
    }

    private String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private LeadCandidateDto toDto(LeadCandidate c) {
        return LeadCandidateDto.from(c);
    }

    private LeadCandidateRunDto toRunDto(LeadCandidateRun r) {
        if (r == null) return null;
        return new LeadCandidateRunDto(
                r.getId(), r.getStatus().name(), r.getTryb(), r.getDniNewsy(), r.getDniPrzetargi(),
                r.getStartedAt(), r.getFinishedAt(), r.getFound(), r.getMessage(), r.getStartedBy());
    }
}
