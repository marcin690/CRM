package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.dashboard.SalesAnalyticsDTO;
import wh.plus.crm.dto.dashboard.SalesAnalyticsDTO.*;
import wh.plus.crm.model.RejectionReason;
import wh.plus.crm.model.lead.ClientType;
import wh.plus.crm.repository.LeadRepository;
import wh.plus.crm.repository.OfferRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agregacja danych do cockpitu sprzedaży (/reports/cockpit).
 * teamId ogranicza widoczność do zespołu (null = brak ograniczenia, ADMIN).
 */
@Service
@RequiredArgsConstructor
public class SalesAnalyticsService {

    private final LeadRepository leadRepository;
    private final OfferRepository offerRepository;

    private static final String[] PL_MONTHS = {"sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru"};
    private static final int TOP_REASONS = 6;
    private static final int MAX_REPS = 10;

    public SalesAnalyticsDTO getAnalytics(LocalDate dateFrom, LocalDate dateTo,
                                          LocalDate compareFrom, LocalDate compareTo, Long teamId) {
        // Domyślnie: ostatnie 3 pełne miesiące łącznie z bieżącym
        if (dateFrom == null || dateTo == null) {
            LocalDate today = LocalDate.now();
            dateFrom = today.withDayOfMonth(1).minusMonths(2);
            dateTo = today;
        }
        LocalDateTime since = dateFrom.atStartOfDay();
        LocalDateTime until = dateTo.atTime(23, 59, 59);

        SalesAnalyticsDTO dto = new SalesAnalyticsDTO();
        dto.setKpi(buildKpi(since, until, teamId));
        dto.setFunnel(buildFunnel(since, until, teamId));
        dto.setSources(buildSources(since, until, teamId));
        dto.setReps(buildReps(since, until, teamId));
        dto.setClients(buildClients(since, until, teamId));
        dto.setIndustries(buildIndustries(since, until, teamId));
        dto.setTrend(buildTrend(since, until, teamId));
        dto.setReasonByIndustry(buildReasonByIndustry(since, until, teamId));
        dto.setReasonByRep(buildReasonByRep(since, until, teamId));

        if (compareFrom != null && compareTo != null) {
            dto.setKpiCompare(buildKpi(compareFrom.atStartOfDay(), compareTo.atTime(23, 59, 59), teamId));
        }
        return dto;
    }

    // ---------- KPI ----------
    private Kpi buildKpi(LocalDateTime since, LocalDateTime until, Long teamId) {
        Object[] leadT = first(leadRepository.analyticsLeadTotals(since, until, teamId), new Object[]{0L, BigDecimal.ZERO});
        Object[] offerT = first(offerRepository.analyticsOfferTotals(since, until, teamId), new Object[]{0L, 0L, 0L, BigDecimal.ZERO});

        long leads = lng(leadT[0]);
        BigDecimal pipeline = bd(leadT[1]);
        long offers = lng(offerT[0]);
        long signed = lng(offerT[2]);
        BigDecimal signedValue = bd(offerT[3]);

        double conversion = leads > 0 ? (signed * 100.0) / leads : 0.0;
        BigDecimal avgContract = signed > 0
                ? signedValue.divide(BigDecimal.valueOf(signed), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new Kpi(leads, offers, signed, round1(conversion), pipeline, avgContract);
    }

    // ---------- Lejek ----------
    private Funnel buildFunnel(LocalDateTime since, LocalDateTime until, Long teamId) {
        Object[] leadT = first(leadRepository.analyticsLeadTotals(since, until, teamId), new Object[]{0L, BigDecimal.ZERO});
        Object[] offerT = first(offerRepository.analyticsOfferTotals(since, until, teamId), new Object[]{0L, 0L, 0L, BigDecimal.ZERO});
        return new Funnel(lng(leadT[0]), lng(offerT[0]), lng(offerT[1]), lng(offerT[2]));
    }

    // ---------- Źródła ----------
    private List<SourceRow> buildSources(LocalDateTime since, LocalDateTime until, Long teamId) {
        Map<String, Long> signedBySource = new HashMap<>();
        for (Object[] r : offerRepository.analyticsSignedBySource(since, until, teamId)) {
            signedBySource.put(str(r[0]), lng(r[1]));
        }
        List<SourceRow> out = new ArrayList<>();
        for (Object[] r : leadRepository.analyticsLeadsBySource(since, until, teamId)) {
            String name = str(r[0]);
            long leads = lng(r[1]);
            BigDecimal value = bd(r[2]);
            long signed = signedBySource.getOrDefault(name, 0L);
            double conv = leads > 0 ? (signed * 100.0) / leads : 0.0;
            out.add(new SourceRow(name, leads, value, round1(conv), signed));
        }
        out.sort(Comparator.comparing(SourceRow::getValue).reversed());
        return out;
    }

    // ---------- Handlowcy ----------
    private List<RepRow> buildReps(LocalDateTime since, LocalDateTime until, Long teamId) {
        Map<Long, String> mainSource = topByGroup(leadRepository.analyticsRepSourceCounts(since, until, teamId));
        Map<Long, RejectionReason> topReasonEnum = topReasonByGroup(leadRepository.analyticsRepReasonCounts(since, until, teamId));
        Map<Long, Long> signedByRep = new HashMap<>();
        for (Object[] r : offerRepository.analyticsSignedByRep(since, until, teamId)) {
            signedByRep.put(lng(r[0]), lng(r[1]));
        }

        List<RepRow> out = new ArrayList<>();
        for (Object[] r : leadRepository.analyticsLeadsByRep(since, until, teamId)) {
            Long userId = lng(r[0]);
            String name = str(r[1]);
            long leads = lng(r[2]);
            BigDecimal value = bd(r[3]);
            long signed = signedByRep.getOrDefault(userId, 0L);
            double conv = leads > 0 ? (signed * 100.0) / leads : 0.0;
            RejectionReason reason = topReasonEnum.get(userId);
            out.add(new RepRow(
                    name,
                    mainSource.getOrDefault(userId, "—"),
                    leads,
                    value,
                    round1(conv),
                    reason != null ? reason.getDescription() : "—"
            ));
        }
        out.sort(Comparator.comparing(RepRow::getValue).reversed());
        return out.size() > MAX_REPS ? out.subList(0, MAX_REPS) : out;
    }

    // ---------- Klienci nowi vs powracający ----------
    private Clients buildClients(LocalDateTime since, LocalDateTime until, Long teamId) {
        long newC = 0, ret = 0;
        for (Object[] r : leadRepository.analyticsClientsNewVsReturning(since, until, teamId)) {
            Boolean returning = (Boolean) r[0];
            long cnt = lng(r[1]);
            if (Boolean.TRUE.equals(returning)) ret += cnt;
            else newC += cnt;
        }
        return new Clients(newC, ret);
    }

    // ---------- Branża wg wartości ----------
    private List<IndustryRow> buildIndustries(LocalDateTime since, LocalDateTime until, Long teamId) {
        List<IndustryRow> out = new ArrayList<>();
        for (Object[] r : leadRepository.analyticsIndustry(since, until, teamId)) {
            ClientType ct = (ClientType) r[0];
            long cnt = lng(r[1]);
            BigDecimal avg = bd(r[2]).setScale(0, RoundingMode.HALF_UP);
            out.add(new IndustryRow(ct != null ? ct.getDescription() : "—", avg, cnt));
        }
        out.sort(Comparator.comparing(IndustryRow::getAvgLeadValue).reversed());
        return out;
    }

    // ---------- Trend narastająco ----------
    private List<TrendPoint> buildTrend(LocalDateTime since, LocalDateTime until, Long teamId) {
        Map<YearMonth, Long> leadsM = monthMap(leadRepository.analyticsLeadsByMonth(since, until, teamId));
        Map<YearMonth, Long> offersM = monthMap(offerRepository.analyticsOffersByMonth(since, until, teamId));
        Map<YearMonth, Long> signedM = monthMap(offerRepository.analyticsSignedByMonth(since, until, teamId));

        List<TrendPoint> out = new ArrayList<>();
        YearMonth m = YearMonth.from(since);
        YearMonth end = YearMonth.from(until);
        long cumL = 0, cumO = 0, cumS = 0;
        int guard = 0;
        while (!m.isAfter(end) && guard++ < 60) {
            cumL += leadsM.getOrDefault(m, 0L);
            cumO += offersM.getOrDefault(m, 0L);
            cumS += signedM.getOrDefault(m, 0L);
            out.add(new TrendPoint(PL_MONTHS[m.getMonthValue() - 1] + " " + m.getYear(), cumL, cumO, cumS));
            m = m.plusMonths(1);
        }
        return out;
    }

    // ---------- Heatmapa: powód × branża ----------
    private Heatmap buildReasonByIndustry(LocalDateTime since, LocalDateTime until, Long teamId) {
        List<RejectionReason> topReasons = topReasons(since, until, teamId);
        Map<ClientType, Map<RejectionReason, Long>> grid = new LinkedHashMap<>();
        for (Object[] r : leadRepository.analyticsReasonByIndustry(since, until, teamId)) {
            ClientType ct = (ClientType) r[0];
            RejectionReason rr = (RejectionReason) r[1];
            long cnt = lng(r[2]);
            grid.computeIfAbsent(ct, k -> new HashMap<>()).merge(rr, cnt, Long::sum);
        }
        List<HeatRow> rows = new ArrayList<>();
        for (Map.Entry<ClientType, Map<RejectionReason, Long>> e : grid.entrySet()) {
            Map<RejectionReason, Long> byReason = e.getValue();
            List<Long> counts = topReasons.stream().map(rr -> byReason.getOrDefault(rr, 0L)).collect(Collectors.toList());
            long sum = byReason.values().stream().mapToLong(Long::longValue).sum();
            rows.add(new HeatRow(e.getKey() != null ? e.getKey().getDescription() : "—", counts, sum));
        }
        rows.sort(Comparator.comparingLong(HeatRow::getSum).reversed());
        return new Heatmap(reasonLabels(topReasons), rows);
    }

    // ---------- Heatmapa: powód × handlowiec ----------
    private Heatmap buildReasonByRep(LocalDateTime since, LocalDateTime until, Long teamId) {
        List<RejectionReason> topReasons = topReasons(since, until, teamId);
        Map<Long, String> names = new HashMap<>();
        Map<Long, Map<RejectionReason, Long>> grid = new LinkedHashMap<>();
        for (Object[] r : leadRepository.analyticsReasonByRep(since, until, teamId)) {
            Long userId = lng(r[0]);
            names.putIfAbsent(userId, str(r[1]));
            RejectionReason rr = (RejectionReason) r[2];
            long cnt = lng(r[3]);
            grid.computeIfAbsent(userId, k -> new HashMap<>()).merge(rr, cnt, Long::sum);
        }
        List<HeatRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Map<RejectionReason, Long>> e : grid.entrySet()) {
            Map<RejectionReason, Long> byReason = e.getValue();
            List<Long> counts = topReasons.stream().map(rr -> byReason.getOrDefault(rr, 0L)).collect(Collectors.toList());
            long sum = byReason.values().stream().mapToLong(Long::longValue).sum();
            rows.add(new HeatRow(names.getOrDefault(e.getKey(), "—"), counts, sum));
        }
        rows.sort(Comparator.comparingLong(HeatRow::getSum).reversed());
        if (rows.size() > MAX_REPS) rows = rows.subList(0, MAX_REPS);
        return new Heatmap(reasonLabels(topReasons), rows);
    }

    // ==================== helpery ====================

    private List<RejectionReason> topReasons(LocalDateTime since, LocalDateTime until, Long teamId) {
        return leadRepository.analyticsReasonTotals(since, until, teamId).stream()
                .sorted((a, b) -> Long.compare(lng(b[1]), lng(a[1])))
                .limit(TOP_REASONS)
                .map(r -> (RejectionReason) r[0])
                .collect(Collectors.toList());
    }

    private List<String> reasonLabels(List<RejectionReason> reasons) {
        return reasons.stream().map(RejectionReason::getDescription).collect(Collectors.toList());
    }

    private Map<Long, String> topByGroup(List<Object[]> rows) {
        Map<Long, String> best = new HashMap<>();
        Map<Long, Long> bestCount = new HashMap<>();
        for (Object[] r : rows) {
            Long g = lng(r[0]);
            String key = str(r[1]);
            long cnt = lng(r[2]);
            if (cnt > bestCount.getOrDefault(g, -1L)) {
                bestCount.put(g, cnt);
                best.put(g, key);
            }
        }
        return best;
    }

    private Map<Long, RejectionReason> topReasonByGroup(List<Object[]> rows) {
        Map<Long, RejectionReason> best = new HashMap<>();
        Map<Long, Long> bestCount = new HashMap<>();
        for (Object[] r : rows) {
            Long g = lng(r[0]);
            RejectionReason key = (RejectionReason) r[1];
            long cnt = lng(r[2]);
            if (cnt > bestCount.getOrDefault(g, -1L)) {
                bestCount.put(g, cnt);
                best.put(g, key);
            }
        }
        return best;
    }

    private Map<YearMonth, Long> monthMap(List<Object[]> rows) {
        Map<YearMonth, Long> map = new HashMap<>();
        for (Object[] r : rows) {
            int y = ((Number) r[0]).intValue();
            int mo = ((Number) r[1]).intValue();
            map.put(YearMonth.of(y, mo), lng(r[2]));
        }
        return map;
    }

    private static Object[] first(List<Object[]> rows, Object[] fallback) {
        return (rows == null || rows.isEmpty() || rows.get(0) == null) ? fallback : rows.get(0);
    }

    private static long lng(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static BigDecimal bd(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }

    private static String str(Object o) {
        return o == null ? "—" : o.toString();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
