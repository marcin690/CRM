package wh.plus.crm.model.lead;

/** Status kandydata na leada wygenerowanego przez agenta AI. */
public enum LeadCandidateStatus {
    /** Nowy — czeka na decyzję handlowca. */
    NEW,
    /** Zaakceptowany — utworzono wersję roboczą leada. */
    ACCEPTED,
    /** Odrzucony — nie wraca w kolejnych przebiegach. */
    REJECTED
}
