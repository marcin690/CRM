# Wytyczne do agentów Asystenta AI (Dify) — co zmienić

Dotyczy 3 aplikacji Dify używanych w module „Asystent AI" w CRM:
- `asystent-claude` (Claude)
- `asystent-chatgpt` (ChatGPT)
- `asystent-gemini` (Gemini)

CRM jest tylko proxy (izolacja, historia, zużycie). **Zachowanie i jakość odpowiedzi zależą od konfiguracji tych aplikacji w Dify** — poniżej lista zmian do wprowadzenia po stronie Dify.

---

## 1. Dostęp do aktualnych danych (najważniejsze)

**Problem:** na pytania typu „sytuacja na rynku hotelowym w Polsce w tym kwartale" agenci odpowiadają „nie mam dostępu do internetu / aktualnych danych / raportów branżowych". Dla użytkownika wygląda to jak bezużyteczny asystent.

**Do zrobienia (wybierz wariant):**
- **A. Dodać narzędzie wyszukiwania w sieci** do chatflow każdej aplikacji (Dify → Tools): np. Tavily Search, Google/Bing Search, Jina Reader. Wtedy model realnie sięgnie po aktualne dane i poda źródła. To rozwiązuje sedno.
- **B. Jeśli świadomie bez internetu** — zmienić prompt systemowy tak, by **nie zaczynał od wymówki**, tylko od wartości: dać ramy/analizę na bazie wiedzy modelu, wyraźnie oznaczyć „dane orientacyjne, zweryfikuj bieżące źródła", zaproponować konkretne źródła (GUS, STR, raporty). Zdanie „nie mam dostępu…" najwyżej jako krótkie zastrzeżenie na końcu, nie jako cała odpowiedź.

**Rekomendacja:** wariant A dla ChatGPT/Gemini (dobre do researchu), a dla Claude co najmniej wariant B.

---

## 2. Routing poziomów (Szybki / Standard / Zaawansowany)

CRM wysyła do Dify zmienną wejściową **`poziom`** o wartości `Szybki`, `Standard` lub `Zaawansowany` (przy pierwszej wiadomości rozmowy). Żeby przełącznik poziomu w UI realnie zmieniał model, każda aplikacja musi:

1. Mieć zdefiniowaną **zmienną wejściową `poziom`** (typ: tekst / select z wartościami: Szybki, Standard, Zaawansowany).
2. W chatflow **rozgałęzić model LLM** wg `poziom` (węzeł IF/ELSE lub parametr modelu) zgodnie z mapą:

| Czat | Szybki | Standard | Zaawansowany |
| --- | --- | --- | --- |
| Claude | claude-haiku-4-5 | claude-sonnet-4-6 | claude-opus-4-6 |
| ChatGPT | gpt-5.4-nano | gpt-5.4-mini | gpt-5.4 |
| Gemini | gemini-2.5-flash-lite | gemini-3-flash-preview | gemini-3.1-pro-preview |

> Ta mapa jest zduplikowana w CRM (`AiChatApp.modelFor`) tylko do raportu zużycia. **Po zmianie modeli w Dify zaktualizuj też mapę w CRM**, inaczej raport kosztów pokaże złą nazwę modelu.

Jeśli aplikacja NIE ma zmiennej `poziom` — dodatkowa zmienna jest ignorowana przez Dify (nic się nie psuje), ale poziom w UI nie zmieni modelu (będzie tylko zapisany w raporcie).

---

## 3. Prompt systemowy — ton i styl

- **Po polsku**, rzeczowo, bez lania wody i bez marketingowego „elevate/seamless".
- **Nie zaczynać od wymówek/zastrzeżeń** — najpierw konkret, zastrzeżenia na końcu i krótko.
- Kontekst: użytkownicy to zespół **WH‑Plus (producent mebli / wyposażenie hoteli)** i CRM — dostosować przykłady (oferty, umowy, maile do inwestorów, notatki ze spotkań, Excel/marże).
- Formatowanie: markdown (nagłówki, listy, tabele, bloki kodu). CRM renderuje markdown poprawnie.
- Nie ujawniać szczegółów technicznych (nazwy modeli, klucze, id) w treści.

---

## 4. Opening statement i suggested questions

CRM pokazuje w pustym stanie **własny, krótki opis** (nie `opening_statement` z Dify), ale **`suggested_questions` bierze z Dify** — więc:
- Ustaw 3–4 **sensowne, konkretne** pytania przykładowe per czat (widoczne jako klikalne w pustym stanie). Przykłady:
  - Claude: „Popraw ten mail do klienta…", „Wypunktuj warunki z tej umowy", „Zrób listę zadań z notatek".
  - ChatGPT: „Przetłumacz tę wiadomość na angielski", „Daj 5 pomysłów na post o realizacji hotelu", „Jaką formułą w Excelu policzę marżę?".
  - Gemini: research/zestawienia/obrazy.
- `opening_statement` może zostać, ale **nie musi** opisywać poziomów (poziomy są w UI CRM). Unikać surowego markdown w tym polu, jeśli gdziekolwiek jest pokazywany.

---

## 5. Pliki / obrazy

- Wszystkie 3 czaty przyjmują **obrazy** (PNG/JPG/WEBP/GIF, do 5/wiadomość, ~10 MB). Upewnij się, że w aplikacjach Dify włączona jest obsługa **image** w file upload.
- Jeśli ma działać analiza **PDF/dokumentów** — włączyć w Dify obsługę „document" (obecnie zakładamy tylko obrazy).

---

## 6. Pamięć rozmowy

- Węzeł LLM ma zwykle okno pamięci ~30 wiadomości. Historia w UI pokazuje całość, ale model „widzi" ostatnie N. Przy bardzo długich rozmowach warto założyć nową. (To ustawienie w Dify — dostosować wg potrzeb/kosztu.)

---

## 7. Checklista wdrożenia po stronie Dify

- [ ] (A) Dodane narzędzie web‑search LUB (B) poprawiony prompt bez wymówek — dla każdego czatu
- [ ] Zmienna wejściowa `poziom` + routing modelu wg mapy z pkt 2 — dla każdego czatu
- [ ] Prompt systemowy: polski, rzeczowy, kontekst WH‑Plus
- [ ] `suggested_questions` ustawione (3–4/czat)
- [ ] Obsługa obrazów włączona (ew. dokumentów)
- [ ] Po zmianie modeli — zaktualizowana mapa w CRM (`AiChatApp.modelFor`) i cennik do raportu

---

_Uwaga: te zmiany są po stronie Dify (nie w kodzie CRM), poza aktualizacją mapy modeli w `AiChatApp` przy zmianie modeli._
