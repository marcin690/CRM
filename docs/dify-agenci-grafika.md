# Obsługa grafiki (generowanie obrazów) przez agentów AI — wymagania

Uzupełnienie do `dify-agenci-asystent-wytyczne.md`. Dotyczy sytuacji: użytkownik prosi „wygeneruj grafikę / wizualizację pokoju hotelowego", a agent odpowiada, że **nie potrafi generować obrazów**.

## Stan obecny

- Czaty (Claude/ChatGPT/Gemini) w Dify **przyjmują obrazy na wejściu** (vision — analiza wgranego zdjęcia/rysunku), ale **NIE generują** nowych obrazów.
- To ograniczenie **modelu/konfiguracji Dify**, nie kodu CRM. Sam model tekstowy nie tworzy grafik — trzeba dodać narzędzie/model do generowania obrazów.

## Co trzeba włączyć w Dify (po stronie aplikacji agenta)

1. **Dodać narzędzie generowania obrazów** (Dify → Marketplace → Tools → zainstaluj), np.:
   - DALL·E 3 (OpenAI),
   - Google Imagen / Gemini image,
   - Stable Diffusion (Stability / dostawca),
   - lub inne dostępne w Twoim Dify.
2. **Podpiąć klucz dostawcy** obrazów w ustawieniach narzędzia.
3. **W chatflow** dodać węzeł, który wywołuje to narzędzie, gdy użytkownik prosi o grafikę:
   - wariant prosty: tryb **Agent** z dostępnym narzędziem image-gen (model sam decyduje, kiedy go użyć),
   - wariant sterowany: rozgałęzienie (IF „prośba o obraz") → węzeł Tool (image-gen) → zwrot wyniku.
4. **Format zwrotu:** narzędzie zwraca URL wygenerowanego obrazu; agent powinien wstawić go w odpowiedzi jako **markdown obrazu**: `![opis](URL)`. Wtedy CRM go wyświetli (patrz niżej).

## Który czat

- Rekomendacja: dedykować **jeden** czat do generowania grafik (np. Gemini albo ChatGPT/DALL·E) i tam włączyć narzędzie — zamiast we wszystkich (koszt + spójność).
- Jeśli dany czat NIE ma generować obrazów: popraw prompt, żeby zamiast „nie potrafię" zaproponował alternatywę (np. „grafiki generuje czat X" albo opis tekstowy do przekazania grafikowi).

## Strona CRM (co już działa / na co uważać)

- Renderer odpowiedzi (react-markdown) **domyślnie pokazuje obrazy** z `![](URL)` — więc wygenerowana grafika pojawi się w czacie bez zmian w kodzie.
- Zalecane drobne dopracowanie CSS (opcjonalne): ograniczyć szerokość obrazów w odpowiedzi — `.ai-md img { max-width:100%; height:auto; border-radius:8px; }` (plik `src/assets/ai-chat.css`), żeby duże grafiki nie rozpychały bąbla.
- URL obrazu pochodzi od zewnętrznego dostawcy (przez Dify) — CRM tylko wyświetla; nic nie przechowuje.

## Koszt i limity

- Generowanie obrazów jest **droższe** niż tekst. Rozważ:
  - ograniczenie do poziomu **Zaawansowany** lub konkretnego czatu,
  - limit liczby generacji (po stronie Dify lub polityki zespołu).

## Checklista

- [ ] Zainstalowane narzędzie image-gen w wybranej aplikacji Dify
- [ ] Podpięty klucz dostawcy obrazów
- [ ] Chatflow wywołuje narzędzie na prośbę o grafikę i zwraca `![](URL)`
- [ ] Prompt: gdy brak generowania — sensowna alternatywa zamiast „nie potrafię"
- [ ] (Opcjonalnie) CSS `.ai-md img { max-width:100% }` w CRM
- [ ] Ustalony limit/koszt generacji

_Uwaga: zmiany po stronie Dify (aplikacje agentów), nie w kodzie CRM — poza opcjonalnym stylem obrazu w `ai-chat.css`._

---

## Naprawa „Czat Gemini" — jak zmusić go do GENEROWANIA grafik (krok po kroku)

Stan wyjściowy (z pliku `Czat Gemini.yml`): chatflow `advanced-chat` — Start → wybór `poziom` → router if-else → 3 węzły LLM (Gemini 2.5 Flash-Lite / 3 Flash / 3.1 Pro) → Answer. Wszystkie LLM mają `vision.enabled: true` (PRZYJMUJĄ obrazy = analiza), ale to modele tekstowe → **nie tworzą** grafik. Trzeba dodać węzeł generujący + routing.

### Krok 1 — źródło grafiki (wybierz jedno)
- **A. Gemini natywnie (najbliżej tego czatu):** w Dify → edycja aplikacji „Czat Gemini" → dodaj węzeł LLM i w wyborze modelu pluginu `langgenius/gemini` poszukaj modelu obrazowego (np. „Gemini 2.5 Flash Image" / „Nano Banana" / Imagen). Jeśli plugin go udostępnia — użyj go w nowym węźle. Jeśli nie ma na liście → droga B.
- **B. Dedykowane narzędzie:** Dify → **Marketplace → Tools** → zainstaluj generator obrazów (DALL·E 3 / Stability / Google Imagen / ComfyUI) → wpnij **klucz API** dostawcy.

### Krok 2 — routing (kiedy generować)
- Prosto: w węźle **Start** do zmiennej `poziom` dodaj opcję **„Grafika"**; w węźle **Poziom modelu** (if-else) dodaj gałąź `poziom == Grafika` prowadzącą do węzła generującego (zamiast do LLM).
- Ambitniej: zamień na węzeł **Agent** z podpiętym narzędziem image-gen — sam wykryje „narysuj/wygeneruj" i użyje narzędzia; przy zwykłym pytaniu odpowie tekstem.

### Krok 3 — węzeł generujący
- Wejście: prompt użytkownika `{{#sys.query#}}` (ew. wzbogacony instrukcją stylu wnętrz hotelowych).
- Wyjście (URL/obraz) → do węzła **Answer**.

### Krok 4 — format odpowiedzi (WAŻNE)
Answer musi zwrócić obraz jako markdown: `![opis](URL)`. Front CRM (react-markdown) wyświetli go bez zmian w kodzie. Zalecany CSS: `.ai-md img{max-width:100%;height:auto;border-radius:8px}`.

### Checklista
- [ ] Model/narzędzie obrazowe dostępne (Gemini image albo tool z Marketplace) + klucz
- [ ] Nowa gałąź „Grafika" w routerze LUB węzeł Agent z narzędziem
- [ ] Węzeł generujący bierze `{{#sys.query#}}` i zwraca URL
- [ ] Answer wypisuje `![](URL)`
- [ ] Prompt: gdy nie da się wygenerować → sensowna alternatywa zamiast „nie potrafię"
- [ ] Test: „wygeneruj wizualizację pokoju hotelowego w ciepłych barwach" → wraca obraz

_Wszystko po stronie Dify (edytor chatflow + narzędzie + klucz). CRM już renderuje obrazy z markdown._
