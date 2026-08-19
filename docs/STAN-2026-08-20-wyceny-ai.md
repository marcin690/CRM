# Stan projektu — Moduł Wyceny AI (handoff 2026-08-20)

Dokument przekazania po długiej sesji. Wszystko wdrożone na produkcję. Sekrety NIE są tu zapisane — pobierz je z Railway (zmienne serwisów) albo z Dify (app-key każdego agenta).

## STATUS: WDROŻONE NA PROD
- Wypchnięte na `main` (oba repo), Railway auto-deploy.
  - backend `marcin690/CRM` commit `757b4ca`
  - front `marcin690/CRM-front` commit `18aaeac`
- Klucze Dify NIE są w repo (puste `${ENV:}` defaulty) — muszą być zmiennymi środowiskowymi na serwisie CRM (inaczej agenci są ukryci; `listAgents` filtruje po obecności klucza).

## ZMIENNE ŚRODOWISKOWE (per serwis Railway) — wartości w Railway/Dify
### Serwis CRM (backend, api.w-h.pl)
- `DIFY_MEBLE_WSPOLNE_KEY` (app-key agenta „Wycena części wspólnych")
- `DIFY_MEBLE_SKRZYNIOWE_KEY` (app-key agenta „Wycena mebli skrzyniowych"; docelowo v3)
- `DIFY_POROWNANIE_OFERT_KEY` (app-key agenta porównawczego)
- `DIFY_BASE_URL` = https://api.agent.w-h.pl/v1 (publiczny; opcjonalnie internal `http://dify-api.railway.internal:<port>/v1`)
- MinIO (już powinny być): `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET=crm-uploads`, `MINIO_PUBLIC_URL=https://media.w-h.pl`
- Profil: prod uruchamiany jak dotąd (SPRING_PROFILES_ACTIVE zależnie od konfiguracji Railway).
### Serwis CRM-Front (crm.w-h.pl)
- `REACT_APP_CENNIK_URL` = link SharePoint do cennika (opcjonalne — jest fallback w kodzie; env wczytywany przy BUILD, więc po zmianie rebuild).
- `REACT_APP_API_BASE_URL` = https://api.w-h.pl (w .env.production, commit).
### Serwisy dify-api + dify-worker
- `QDRANT_URL` = https://ccec60d3-6eb8-4220-8a19-37f760811fff.eu-central-1-0.aws.cloud.qdrant.io:6333
- `QDRANT_API_KEY` (klucz nowego klastra Qdrant Cloud)
- `VECTOR_STORE=qdrant`

## CO ZBUDOWANO
### Moduł Narzędzia → Wyceny AI (backend Spring + front React/CRA)
- Kolejka async (2–3 równolegle), statusy: QUEUED/PROCESSING/PRICED/ACTION_REQUIRED/FAILED.
- Nowa wycena: wybór agenta, KLASA materiału (ekonomiczna/standard/premium z opisem), ILOŚĆ, plik LUB sam opis (bez rysunku), uwagi/materiały.
- Szczegół: wynik markdown (spis treści + zwijane sekcje), Pobierz Excel, Otwórz plik źródłowy, czat z agentem (async), ocena 👍/👎 + komentarz (feedback do Dify API), panel „Porównanie z ofertą dostawcy" (admin, 1:wiele).
- Koszt widoczny tylko dla admina; filtr po osobach na liście; przycisk „Cennik źródłowy".
- Globalny badge „w toku" w menu (endpoint /ai-valuations/active-count) + puls „Nowa odpowiedź" na szczególe.
### Integracja Dify (backend)
- `service/aivaluation/DifyClient` (chat-messages blocking, upload plików `/files/upload`, feedback `/messages/{id}/feedbacks`), `ValuationJobProcessor` (@Async, long-timeout 360s), `ValuationService`.
- `config/DifyConfig` (DifyProperties: base-url + agent-keys map; bean difyRestTemplate 360s; executor valuationExecutor 2-3), `AiAgentInitializer` (seed agentów meble-wspolne, meble-skrzyniowe).
- Encje: `AiAgent`, `ValuationJob` (+ materialClass, quantity, comparisonConversationId, comparisonResult), `ValuationMessage` (+ difyMessageId).
- Kontroler `AiValuationController`: GET /ai-valuations[, /agents, /{id}, /active-count]; POST / (multipart agentCode,note,materialClass,quantity,files), /{id}/messages, /{id}/feedback, /{id}/compare.
### Unormowanie UI (Bootstrap 5) — całe CRM
- Wspólne `components/common/PageContainer` + `PageHeader`; globalny `assets/crm-ui.css` (kontenery, tytuły kolor #344c72, tabele, inputy, karty, header, accordion/ToC, chat-pulse).
- Przerobione ~20 podstron (Leady, Oferty, Projekty, Klienci, Dostawcy, Ekipy, Zamówienia, Raporty, admin).
- `utils/miniMarkdown.js` (renderer md ze spisem treści + accordion), `utils/valuationStatus.js` (globalny licznik w toku).

## AGENCI DIFY
- meble-wspolne: „Wycena części wspólnych beta" (chatflow, RAG cennik + generacja Excela przez wh-flask; link Excel w treści answer).
- meble-skrzyniowe: v2 w Dify; przygotowany v3 (`~/Downloads/Wycena mebli skrzyniowych v3.yml`, także w historii rozmowy) — 3 klasy, JEDNA cena zamiast przedziału, BEZ robocizny, pyta o ilość + pełna partia + cena jednostkowa, spójne grupy A-D. DO IMPORTU + Publish + nowy klucz.
- porownanie-ofert: „Agent porównawczy" (opublikowany) — audyt wyceny (odchylenia/nielogiczności/za drogo) + porównanie z ofertą dostawcy. v1 bez własnego cennika (cennik wchodzi przez samą wycenę). DSL v2 z cennikiem: `~/Downloads/Agent porownawczy ofert v2 (z cennikiem).yml`.

## CENNIK (baza wiedzy Dify)
- Master: Excel (team edytuje) `cennik_materialow_v3.xlsx` (19 arkuszy = kategorie, 400 pozycji).
- Do Dify jako TXT (RAG-friendly, 1 pozycja/linia z prefiksem [Kategoria]): `~/Downloads/cennik_materialow_v3.txt`. Konwerter: `~/Downloads/cennik_xlsx_to_txt.py` (openpyxl).
- NOWA baza wiedzy (klasyczna, nie „potok wiedzy!"): dataset_id `6bf175ef-ecca-46da-af57-458ab47c87a9`.
- Ustawienia indeksowania: General, separator `\n`, max 1024, nakładka 50, preprocessing „zastąp spacje" ODZNACZONE, embedding OpenAI `text-embedding-3-small` (Jina free ma limit 2 równoległe), rerank `jina-reranker-v3`, TopK 8-10, próg off.
- Vector DB: Qdrant Cloud, NOWY klaster (stary `629d4a29…` USUNIĘTY — darmowy tier kasuje po nieaktywności; to był powód błędów „404 page not found" i „[SZACUNEK]" w wycenach).

## DO ZROBIENIA (pending — po stronie Dify/usera)
1. Import agenta skrzyniowego v3 → przepiąć 5 węzłów Knowledge Retrieval na cennik `6bf175ef` → Publish → ustawić `DIFY_MEBLE_SKRZYNIOWE_KEY` na nowy klucz.
2. Agent „meble wspólne": przepiąć 7 węzłów Knowledge Retrieval na cennik `6bf175ef` (stary dataset martwy) — inaczej wyceny dają [SZACUNEK] zamiast [CENNIK]. Weryfikacja: DTDL 18mm biała ma być 180–260 zł, nie ~120.
3. Docelowo Qdrant self-host na Railway (darmowy Cloud znów się skasuje po nieaktywności).
4. Klucze Dify → z committed-default na zmienne Railway (zrobione: puste defaulty; ustawić env).

## URUCHOMIENIE LOKALNE (po resecie)
- Backend (IntelliJ): profil `dev` (SPRING_PROFILES_ACTIVE=dev). Sekrety w `crm/local.properties` (GITIGNORED — odtworzyć po wipe): minio.access-key, minio.secret-key oraz dify.agent-keys.{meble-wspolne,meble-skrzyniowe,porownanie-ofert} = odpowiednie app-key (skopiuj z Railway/Dify).
- Front: `cd crm-frontend && npm run start:dev` (NIE `npm start` — to serwuje stary build produkcyjny). Dev :3000 → backend :8080.
- Nowe tabele/kolumny (ai_agent, valuation_job, valuation_message, comparison_*) powstają przez ddl-auto=update.

## ZNANE OGRANICZENIA / TECH TODO
- FFD (rozkrój) liczony przez LLM = niedeterministyczny mimo temp 0; jeśli wyceny „skaczą" → przejść na prosty wzór pole/arkusz + naddatek %.
- Detekcja statusu wycena vs pytanie = heurystyka (link xlsx / „RAZEM|PLN netto|Ważność"), nie streaming SSE.
- Panel porównania: synchroniczny (agent ~58s); difyRestTemplate read-timeout 360s.
- Wersja mobilna + menu mobilne: świadomie ODŁOŻONE na osobny task.
- W UI unikać żargonu (Dify/RAG/dataset/endpoint) — usunięto user-facing „Dify".
