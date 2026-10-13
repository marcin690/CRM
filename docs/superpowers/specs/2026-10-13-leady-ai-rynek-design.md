# Automatyczne leady z rynku (Dify) — spec

**Data:** 2026-10-13
**Branch:** `feat/leady-ai-rynek` (backend), `feat/leady-ai-rynek` (crm-frontend)
**Status:** zatwierdzony kierunek (makieta + decyzje), implementacja w toku

## Cel

Nowa zakładka w module Leady: **„Automatyczne z rynku"**. Agent Dify
przeszukuje rynek (przetargi BZP/TED, sygnały inwestycyjne z Tavily,
zapytania ofertowe ze Zwiadowcy) i generuje **kandydatów na leady**.
Handlowiec przegląda ich kartami w stylu swipe (jak Tinder) i decyduje:
**Dodaj** (tworzy wersję roboczą leada) albo **Odrzuć**.

## Decyzje (zatwierdzone)

1. **Akcja „Dodaj"** → tworzy **wersję roboczą leada** w CRM (`isFinal=false`,
   bez przypisanego handlowca — „czeka na przypisanie"), z polami wypełnionymi
   z kandydata. Kandydat → status `ACCEPTED`.
2. **Persystencja** → nowa tabela `lead_candidate` w CRM (jedyne źródło prawdy).
   Bez zewnętrznego PostgREST. Odrzucone/zaakceptowane nie wracają w kolejnych
   przebiegach (dedup).
3. **Wyzwalanie** → tylko ręcznie (przycisk „Uruchom agenta" + parametry).
4. **Wyjście Dify** → workflow dostaje nowe wyjście `leady_json`
   (ustrukturyzowana lista) obok istniejącego `raport` (Markdown).

## Dify

Wdrożona aplikacja: **workflow** „WH PLUS Leady (raport)",
`https://api.agent.w-h.pl/v1`, klucz `app-T8Bab43sPwIJERYNtTX7v5tx`.

- Wywołanie: `POST {baseUrl}/workflows/run`
  body: `{"inputs":{"tryb":"wszystko","dni_newsy":14,"dni_przetargi":7},
  "response_mode":"blocking","user":"<prefix>:system"}`
- Odpowiedź: `{"data":{"outputs":{"leady_json":"[...]","raport":"...",
  "obiektow":N,...},"status":"succeeded"}}`
- Klucz w configu: `dify.agent-keys.leady-rynek=${DIFY_KEY_LEADY_RYNEK:}`
  (env na Railway: `DIFY_KEY_LEADY_RYNEK=app-T8Bab43sPwIJERYNtTX7v5tx`).

### Zmiana po stronie Dify (robi użytkownik)
W node **End** dodać wyjście `leady_json` = `lista` obiektów (już istnieje w
workflow przed zbudowaniem Markdown) zserializowana `json.dumps(...)`.
**Kontrakt JSON (tablica obiektów):**

```json
[{
  "nazwa": "Hotel Gołębiewski — rozbudowa",
  "miasto": "Pobierowo",
  "wojewodztwo": "zachodniopomorskie",
  "rodzaj": "hotel",
  "punkty": 85,
  "przetarg": true,
  "sygnaly": [{
    "typ": "przetarg_meble",
    "etykieta": "Przetarg na meble",
    "data_publikacji": "2026-10-09",
    "termin": "2026-11-14",
    "opis": "Postępowanie na wyposażenie meblowe nowego skrzydła…",
    "zrodlo": "BZP 2026/BZP-00531",
    "url": "https://ezamowienia.gov.pl/…"
  }],
  "szczegoly": {"inwestor":"…","operator":"…","wykonawca":"…",
    "architekt":"…","liczba_pokoi":"400","termin_otwarcia":"2027","etap":"…"}
}]
```

Pola opcjonalne mogą być puste/pominięte; backend ignoruje nieznane klucze.
Do czasu dodania `leady_json` backend ma **fallback**: jeśli brak
`leady_json`, loguje ostrzeżenie i zwraca 0 kandydatów (nie parsuje Markdown).

## Backend (Spring Boot)

Nowy pakiet `wh.plus.crm.service.ailead` + `controller` + `model.lead`.

### Encje
- **`LeadCandidate`** (`lead_candidate`):
  `id`, `dedupKey` (unique), `nazwa`, `miasto`, `wojewodztwo`, `rodzaj`,
  `punkty` (int), `przetarg` (bool), `status` enum `NEW|ACCEPTED|REJECTED`,
  `sygnalyJson` TEXT, `szczegolyJson` TEXT, `zrodloUrl`,
  `generatedAt`, `decidedAt`, `decidedBy`, `acceptedLeadId`, `rejectionNote`.
- **`LeadCandidateRun`** (`lead_candidate_run`): `id`, `status`
  `RUNNING|DONE|FAILED`, `tryb`, `dniNewsy`, `dniPrzetargi`,
  `startedAt`, `finishedAt`, `found` (ile nowych), `message`, `startedBy`.

`dedupKey` = normalizacja `nazwa|miasto` (lowercase, bez znaków
diakrytycznych, bez skrótów typu „hotel/sp/z/oo") — analogicznie do funkcji
`klucz()` w workflow. Upsert: `NEW` → odśwież dane; `ACCEPTED/REJECTED` →
pomiń (nie wskrzeszaj).

Tworzenie tabel: **ddl-auto=update** (dev + prod) tworzy obie tabele.
Brak migracji Flyway dla nowych tabel (Flyway tylko do zmian istniejących
kolumn — vide V17). [[crm-phase0-2026-07]]

### Klient Dify
- **`DifyLeadsClient`** — wołanie `POST /workflows/run` (blocking),
  `@Qualifier("difyRestTemplate")` (read-timeout 360s). Czyta
  `data.outputs.leady_json`, deserializuje do listy DTO (Jackson,
  `@JsonIgnoreProperties(ignoreUnknown=true)`).

### Serwis + async
- **`LeadCandidateService`**:
  - `startRun(tryb, dniNewsy, dniPrzetargi, user)` → tworzy `RUNNING` run,
    `enqueueAfterCommit` → `processor.process(runId)` (wzorzec z wycen).
  - `process(runId)` `@Async("valuationExecutor")` → `DifyLeadsClient` →
    upsert kandydatów → `DONE`/`FAILED`.
  - `listCandidates(status)` (domyślnie `NEW`, sort: punkty desc).
  - `accept(id, user)` → buduje `LeadDTO`, `leadService.save(dto)`,
    kandydat `ACCEPTED` + `acceptedLeadId`.
  - `reject(id, note, user)` → kandydat `REJECTED`.
  - `latestRun()`.

### Mapowanie „Dodaj" → Lead (wersja robocza)
| Lead | Źródło |
|---|---|
| `name` | `nazwa` |
| `clientType` | `rodzaj` → `HOTEL_CHAIN` dla obiektów hotelowych/sanatoryjnych; inaczej null |
| `clientCity` | `miasto` |
| `clientState` | `wojewodztwo` |
| `clientCountry` | „Polska" |
| `roomsQuantity` | `szczegoly.liczba_pokoi` jeśli liczbowe |
| `description` | złożony opis: rodzaj, punkty, lista sygnałów (data · etykieta · opis · url) |
| `sourceUrl` | pierwszy `url` z sygnałów |
| `leadSource` | find-or-create „Agent AI (rynek)" |
| `leadStatus` | null (handlowiec ustawia) |
| `user` | null (nieprzypisany) |
| `isFinal` | false |

`createdBy` (Auditable) = akceptujący handlowiec.

### Endpointy (`/lead-candidates`, `@PreAuthorize("isAuthenticated()")`)
- `POST /lead-candidates/generate` — body `{tryb,dniNewsy,dniPrzetargi}` → run DTO (RUNNING)
- `GET  /lead-candidates/run/latest` — status ostatniego przebiegu (poll)
- `GET  /lead-candidates?status=NEW` — lista kandydatów
- `POST /lead-candidates/{id}/accept` — → `{leadId}`
- `POST /lead-candidates/{id}/reject` — body `{note?}`

## Frontend (React / CRA, Bootstrap 5)

- **Nav** (`src/layouts/Nav.js`): do „Leady" dodać dziecko
  `{ to:"/leads/auto", label:"Automatyczne z rynku" }`.
- **Route** (`src/App.js`): `/leads/auto` → `AutoLeadsPage`.
- **Strona** `src/pages/leads/AutoLeadsPage.js`: `PageContainer` + `PageHeader`
  (breadcrumb Leady / Automatyczne z rynku). Pasek kontekstu (tryb, zakres,
  ostatni przebieg) + „Uruchom agenta" (modal z `tryb/dniNewsy/dniPrzetargi`).
  Stos kart swipe (port makiety): przeciąganie + ←/→ + ✕/✓, badge punktów wg
  progu (granat/bursztyn/szary), chipy typów, siatka szczegółów, oś czasu
  sygnałów ze źródłami. „Dodaj" → `accept` (toast + opcjonalny link do leada),
  „Odrzuć" → `reject`. Po pustej liście — ekran podsumowania.
  Polling `run/latest` w trakcie `RUNNING` (wzorzec z `AiValuationsPage`).
- Styl 1:1 z `crm-ui.css` (paleta, badge, radius). Makieta referencyjna:
  artefakt v6 (zweryfikowany w przeglądarce).

## Poza zakresem (YAGNI v1)
- Harmonogram/cron (tylko ręcznie).
- Parsowanie Markdown (tylko `leady_json`).
- Edycja pól kandydata przed akceptacją (handlowiec dopracowuje leada roboczego).
- Przywracanie odrzuconych (dedup je blokuje; ewentualnie później).

## Powiązania
[[crm-ai-valuation-dify]] (wzorzec kolejki/klienta), [[crm-stack-and-modules]],
[[crm-phase0-2026-07]] (ddl-auto vs Flyway).
