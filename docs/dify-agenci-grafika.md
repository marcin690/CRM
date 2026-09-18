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
