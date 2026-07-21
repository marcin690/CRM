# Wdrożenie na produkcję (Railway)

## Zmienne środowiskowe (Railway → Settings → Variables)

Baza (MySQL — wstrzykiwane przez Railway plugin):
- `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
- `PORT` (Railway ustawia automatycznie)

Sekrety aplikacji (USTAW ręcznie — nie polegaj na defaultach z repo):
- `SMSAPI_TOKEN` — token SMS API. **Zrotuj obecny klucz** (był zahardkodowany w repo) i ustaw nowy tutaj.
- `SMSAPI_SENDER` — nazwa nadawcy SMS.
- `RECAPTCHA_SECRET_KEY` — klucz reCAPTCHA v3 (bez niego weryfikacja pomijana; honeypot + rate-limit działają).
- `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`, `MINIO_PUBLIC_URL` — storage plików.

Integracje (opcjonalne — bez nich moduł jest wyłączony i zwraca jasny komunikat):
- Fakturownia: `FAKTUROWNIA_SYNC_CRON`, `FAKTUROWNIA_SYNC_PERIOD` (this_month|this_year|last_year|all), `FAKTUROWNIA_BASE_URL_TEMPLATE`.
- SharePoint: `SHAREPOINT_TENANT_ID`, `SHAREPOINT_CLIENT_ID`, `SHAREPOINT_CLIENT_SECRET`, `SHAREPOINT_SITE_URL`.

CORS: `CORS_ALLOWED_ORIGINS` — CSV originów frontendu (bez `*`, bo `allowCredentials=true`).

## Baza danych / migracje

- Profil prod: `spring.flyway.enabled=true`, `baseline-on-migrate=true`, `ddl-auto=update`.
- Migracje Flyway: `src/main/resources/db/migration` (obecnie do **V12**). Wdrożenie uruchomi je automatycznie.
- **V12** naprawia regułę `ON DELETE SET NULL` dla `invoice.binding_id` (usunięcie wiązania Fakturowni odpina faktury zamiast wysypać FK). Migracja jest odporna na losową nazwę FK i zakłada, że tabele `invoice` i `project_fakturownia_binding` już istnieją (tworzy je `ddl-auto`).

### Dług do domknięcia (osobny task, wymaga stagingu)
Nowe tabele (etapy, montaże, ekipy, zamówienia, meble, faktury, dostawcy, dziennik, powiadomienia)
powstają dziś przez `ddl-auto=update`, a NIE przez migracje Flyway. Docelowo:
1. Zrzucić aktualny schemat prod jako baseline SQL.
2. Dopisać migracje dla przyszłych zmian.
3. Przełączyć prod na `ddl-auto=validate`.
**Nie przełączaj na `validate` bez testu na kopii bazy prod** — Hibernate validate na MySQL jest bardzo czuły
(bool/bit, długości TEXT, enumy, tabele Envers `_AUD`) i może zablokować start aplikacji.

## Build

- Backend: `mvn -o -DskipTests package` → `target/crm-0.0.1-SNAPSHOT.jar` (bootowalny).
- Frontend: `CI=false npm run build` → katalog `build/` gotowy do wdrożenia (serwowany przez `serve`).
