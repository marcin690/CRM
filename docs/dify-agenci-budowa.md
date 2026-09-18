# Budowa agentów AI (Dify) — internet, grafika, zero wymówek

Skonsolidowane wytyczne „jak zbudować" każdą z 3 aplikacji czatu w Dify (`asystent-claude`, `asystent-chatgpt`, `asystent-gemini`), żeby:
1. miały **dostęp do internetu** (aktualne dane) — nie odpowiadały „nie mam dostępu",
2. potrafiły **generować grafikę**,
3. **nie zaczynały od wymówek** („nie wiem / nie mam dostępu / jestem tylko modelem").

CRM jest tylko proxy (izolacja, historia, zużycie) — poniższe zmiany robi się **w Dify**, w konfiguracji każdej aplikacji.

---

## 1. Dostęp do internetu (aktualne dane)

Przyczyna „nie mam dostępu do bieżących danych": agent nie ma narzędzia wyszukiwania. Dodaj je.

**Kroki w Dify (dla każdej aplikacji):**
1. Marketplace → **Tools** → zainstaluj narzędzie web-search: **Tavily Search** (rekomendowane) lub Google/Bing Search, ewentualnie Jina Reader do czytania stron.
2. Wpnij **klucz API** dostawcy wyszukiwarki w ustawieniach narzędzia.
3. W chatflow ustaw węzeł **Agent** (tryb agentowy) z dostępnym narzędziem search — model sam użyje go, gdy pytanie wymaga aktualnych danych. (Alternatywnie: rozgałęzienie „pytanie o bieżące dane" → węzeł Tool → wynik do LLM.)
4. Poproś w prompcie, by **cytował źródła** (linki) przy danych z sieci.

Efekt: na „sytuacja na rynku hotelowym w tym kwartale" agent wyszuka i poda dane + źródła, zamiast odmawiać.

---

## 2. Generowanie grafiki

Agenci **przyjmują** obrazy (vision), ale nie **generują**. Dodaj narzędzie image-gen.

**Kroki w Dify:**
1. Marketplace → **Tools** → zainstaluj generator obrazów: **DALL·E 3**, **Google Imagen/Gemini image** lub **Stable Diffusion**.
2. Wpnij klucz dostawcy.
3. W chatflow (tryb Agent lub rozgałęzienie) udostępnij narzędzie image-gen na prośby typu „wygeneruj / narysuj / wizualizacja".
4. Agent ma zwrócić obraz jako **markdown**: `![opis](URL)` — CRM to wyświetli (react-markdown renderuje obrazy; zalecane `.ai-md img{max-width:100%}`).

**Rekomendacja:** dedykuj generowanie grafik jednemu czatowi (np. Gemini/ChatGPT) — koszt + spójność.

---

## 3. Prompt systemowy — zero wymówek

Zasady do wklejenia w prompt każdej aplikacji:
- **Najpierw działaj, potem ewentualne zastrzeżenia.** Zanim napiszesz „nie mam dostępu" — **użyj narzędzia** (search/image). Wymówka jest dozwolona najwyżej jako krótkie zdanie na końcu, nigdy jako cała odpowiedź.
- Gdy pytanie dotyczy bieżących danych → **użyj wyszukiwarki**, podaj wynik + źródła.
- Gdy prośba o grafikę → **użyj image-gen**, zwróć `![](url)`.
- Jeśli naprawdę czegoś nie da się zrobić → zaproponuj **konkret** (co możesz zrobić / jakie źródło sprawdzić), nie samo „nie potrafię".
- Po **polsku**, rzeczowo, bez marketingowego lania wody. Kontekst: zespół **WH-Plus (wyposażenie hoteli / producent mebli)** i CRM.
- Nie ujawniaj nazw modeli, kluczy, id.

---

## 4. Poziomy modeli (routing)

CRM wysyła zmienną wejściową **`poziom`** = `Szybki` / `Standard` / `Zaawansowany`. Aby przełącznik w UI realnie zmieniał model, każda aplikacja musi mieć zmienną `poziom` + rozgałęzienie modelu:

| Czat | Szybki | Standard | Zaawansowany |
| --- | --- | --- | --- |
| Claude | claude-haiku-4-5 | claude-sonnet-4-6 | claude-opus-4-6 |
| ChatGPT | gpt-5.4-nano | gpt-5.4-mini | gpt-5.4 |
| Gemini | gemini-2.5-flash-lite | gemini-3-flash-preview | gemini-3.1-pro-preview |

Po zmianie modeli zaktualizuj mapę w CRM (`AiChatApp.modelFor`) — inaczej raport zużycia pokaże złą nazwę.

---

## 5. Pliki / obrazy na wejściu i pamięć

- Włącz obsługę **image** (i ew. **document/PDF**) w file upload aplikacji.
- Okno pamięci LLM ~30 wiadomości (dostosuj wg kosztu).

---

## Checklista budowy (per aplikacja)

- [ ] Narzędzie **web-search** zainstalowane + klucz (internet/aktualne dane)
- [ ] Narzędzie **image-gen** zainstalowane + klucz (grafika) — min. w 1 czacie
- [ ] Tryb **Agent** (lub rozgałęzienia) faktycznie wywołuje te narzędzia
- [ ] Prompt: **zero wymówek**, najpierw narzędzia, po polsku, kontekst WH-Plus
- [ ] Zmienna **`poziom`** + routing modelu wg tabeli
- [ ] `suggested_questions` ustawione (3–4)
- [ ] Obsługa obrazów (ew. PDF) w upload
- [ ] Test: „sytuacja na rynku hotelowym w tym kwartale" → wyszukuje + źródła; „wygeneruj grafikę pokoju" → zwraca obraz

---

_Uwaga: to zmiany po stronie Dify (aplikacje agentów), nie w kodzie CRM — poza aktualizacją mapy modeli w `AiChatApp` i opcjonalnym stylem obrazu w `ai-chat.css`. Powiązane: `dify-agenci-asystent-wytyczne.md`, `dify-agenci-grafika.md`._
