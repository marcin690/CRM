# Stan projektu CRM — 2026-07-30 (wznowić po urlopie)

Zapis stanu na koniec sesji. Dwa repo: `crm` (backend, Spring Boot) i `crm-frontend` (React/CRA).
Prod na Railway (backend `api.w-h.pl`, front `crm.w-h.pl`), oba deployują z **`main`**.

---

## 1. Co jest już NA PRODZIE (main, wdrożone)

Kolejność chronologiczna hotfixów:

| Fix | Repo / commit na main | Efekt |
|---|---|---|
| Autoryzacja leadów: `hasRole` → `hasAuthority` | backend `f050f6c` (PR #27) | naprawa 401/403 po `@EnableMethodSecurity` |
| Hardening JWT + czytelne 401/403 | backend (w f050f6c) | `JwtAuthenticationFilter` łapie wyjątki, `SecurityConfig` zwraca konkretny powód + `accessDeniedHandler` |
| CORS: domeny WH z wildcardem subdomen (www) | backend `56b35c7` → merge `32e3603` | `www.w-h.pl` i inne subdomeny zawsze dozwolone (potwierdzone w logu: 10 wzorców) |
| **Fix 403 handlowców** | backend `3b60753` → merge `7aba891` | `/leads` = `isAuthenticated()`; SALESPERSON/ADMINISTRATION znów tworzą/edytują leady |
| Ybug (widget zgłaszania błędów) w React | front `2fb2b9d` → merge `c149c2f` | hook `useYbug` wstrzykiwany w `App.js` |

**Tip main:** backend `7aba891`, frontend `c149c2f`.

### Do zweryfikowania po deployu (gdyby ktoś jeszcze zgłaszał)
- Handlowiec (rola SALESPERSON) dodaje leada → 201, nie 403.
- Ybug: twarde odświeżenie CRM → przycisk zgłaszania w rogu.
- Gdyby 403 wracał: sprawdzić czy Railway dokończył deploy (`Started CrmApplication` z commitem ≥ `7aba891`) i jaką rolę ma user.

### Diagnoza (kontekst, gdyby wróciło)
- „Nie mogą zapisać leada" = **403**, nie 404/CORS. Response `{"error":"forbidden",...}` (66 B) = `accessDeniedHandler`.
- Root cause: `@PreAuthorize` za wąskie (`USER`/`ADMIN`) blokowało handlowców. Szczegóły w pamięci `crm-security-role-prefix.md`.
- Pełny postmortem pierwszego incydentu: `docs/incydenty/2026-07-27-401-leads-autoryzacja.md`.

---

## 2. Co CZEKA (gotowe, ale NIE na prodzie) — moduł powiadomień

Kompletny, przetestowany, **nie zmergowany do main**. Do wznowienia po urlopie.

**Branche (oba repo): `feature/notification-triggers`**
- backend: commit `0f19d12` (od `main`)
- frontend: commit `47de62a` (od `main`)

### Zakres
- Rozszerzalny **silnik triggerów** powiadomień (email + in-app).
- Pierwszy trigger: **`NEW_LEAD`** — tylko dla leadów automatycznych z formularza (`PublicLeadController`), NIE dla ręcznych.
- Odbiorcy = konkretni userzy CRM; konfiguracja per trigger (on/off, kanały, odbiorcy) z panelu admina.

### Backend (nowe/zmienione)
- `model/notification/NotificationTrigger.java` (+ `NotificationTriggerType`, `trigger_type` = VARCHAR dla łatwej rozbudowy)
- `repository/NotificationTriggerRepository.java`
- `service/notification/NotificationTriggerService.java` (+ `TriggerContext`) — `fire()` @Async, nie rzuca
- `controller/NotificationTriggerController.java` — `GET/PUT /notifications/triggers`, `hasAuthority('ADMIN')`
- `dto/notification/NotificationTriggerDTO.java`
- `service/EmailService.java` — odkomentowany `@Service` + `@ConditionalOnProperty(spring.mail.host)`
- `CrmApplication.java` — usunięte wykluczenie `MailSenderAutoConfiguration` + `@EnableAsync`
- `templates/newLeadNotification.html` — szablon maila
- `NotificationController.java` — link do leada w dzwonku
- `application-prod.properties` — sekcja SMTP (env) + `app.crm.base-url`
- test: `NotificationTriggerServiceTest` (4/4 pass)

### Frontend (nowe/zmienione)
- `pages/admin/NotificationSettingsPage.js` — panel „Ustawienia powiadomień"
- `App.js` — route `/admin/notifications`
- `layouts/Nav.js` — link w menu (tylko ADMIN, pod „Leady")

### Zweryfikowane
- Migracja bezpieczna: przetestowany start na czystym **MySQL 9.4** (jak prod) z `ddl-auto=update` — Hibernate tworzy `notification_triggers` + `notification_trigger_recipients`, app startuje czysto. **Brak migracji Flyway** (świadomie — tabele dokłada ddl-auto).

### CZEGO BRAKUJE do uruchomienia (po urlopie)
1. **Env SMTP w Railway** (bez tego maile nie wychodzą, in-app działa):
   `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`,
   opcjonalnie `MAIL_FROM` (np. `powiadomienia@whplus.com.pl`), `CRM_BASE_URL`.
2. Merge `feature/notification-triggers` → `main` w OBU repo.
3. Po deployu: panel „Ustawienia powiadomień" → włączyć `NEW_LEAD`, kanały, wskazać odbiorców.

---

## 3. Sprzątanie branchy (opcjonalne)

Zmergowane do main, można usunąć na GitHub:
- backend: `hotfix/flyway-v12-repair` (PR #27), `hotfix/cors-www`, `hotfix/lead-role-403`
- frontend: `hotfix/ybug`, `hotfix/ybug-react`

Zostawić: `feature/notification-triggers` (oba repo) — do dokończenia.

---

## 4. Uwagi techniczne / dług

- **Testy pod Javą 25:** lokalnie JDK 25, projekt celuje w Java 17 → Mockito wymaga `-Dnet.bytebuddy.experimental=true`. Na CI/prod (Java 21) niepotrzebne.
- **`ddl-auto=update` + Flyway razem na prodzie** — ryzykowna kombinacja (źródło wcześniejszych awarii V12/V13). Nowe tabele idą przez ddl-auto (addytywnie, OK), ale docelowo warto uporządkować.
- **CORS_ALLOWED_ORIGINS** (env Railway) — od teraz nawet niekompletny nie zablokuje domen WH (są wbudowane w `CorsConfig`).
- Frontend: kilka komponentów importuje `axios` bezpośrednio zamiast klienta `api` (`utils/axiosConfig`) — potencjalne źródło rozjazdu baseURL; do przeglądu.

---

## 5. Jak wznowić (skrót)

```
# backend
cd crm && git checkout feature/notification-triggers
# frontend
cd crm-frontend && git checkout feature/notification-triggers
```
Potem: ustawić env SMTP → merge do main (oba) → skonfigurować odbiorców w panelu.
