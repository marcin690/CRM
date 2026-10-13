# Incydent 2026-07-27 — 401 przy tworzeniu leada na prodzie

Log sesji debugowania i naprawy. Branch: `hotfix/flyway-v12-repair`. Commit naprawczy: `f050f6c`.

---

## 1. Zgłoszenie

Po ostatnim deployu na prod (Railway) **nie da się utworzyć leada**.

- `POST https://api.w-h.pl/leads` → **401 Unauthorized**
- Body odpowiedzi: `Token wygasł lub jest nieprawidłowy.`
- W żądaniu był ważny `Authorization: Bearer <jwt>`.
- W terminalu Railway **brak widocznych błędów** — stąd podejrzenie, że komunikat jest mylący.
- Log podesłany przez usera (`logs.1785137266345.log`) był z **24.07** (inny incydent), nie z momentu awarii 27.07.

Kontekst zmian: merge `f18b813` (przebudowa modułu projektu) + zmiana struktury bazy + hotfixy V13 (AUTO_INCREMENT).

## 2. Diagnoza — krok po kroku

1. **Token nie był wygasły.** Zdekodowany JWT: `sub=marcinpohl`, `iat=07:20:16Z`, `exp=17:20:16Z`, żądanie o `07:25:50Z`. Ważny jeszcze ~10 h → komunikat „Token wygasł" **mylący**.
2. **`ExpiredJwtException` z logu 24.07 to inny incydent** (realnie wygasły token o 19:46 tamtego dnia). Odrzucony jako nieistotny dla 27.07.
3. **Login działał** (`/auth/login` → `AuthenticationManager` → `loadUserByUsername`), więc user `marcinpohl` istnieje w bazie i hasło się zgadza.
4. **Diff bezpieczeństwa w mergu:** jedyna zmiana w kodzie security to dodanie **`@EnableMethodSecurity`** w `SecurityConfig`.
5. **Skutek:** dotąd ignorowane adnotacje `@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")` na `LeadController` **ożyły**.
6. **Sedno błędu:** `hasRole('ADMIN')` wymaga authority `ROLE_ADMIN`, a `Role.getAuthority()` zwraca gołą nazwę enuma `"ADMIN"` (bez prefiksu `ROLE_`). Nowe kontrolery Fakturowni używają poprawnie `hasAuthority('ADMIN')` — stary `LeadController` używał `hasRole` → odmowa dla każdego.

## 3. Naprawa

| Plik | Zmiana |
|---|---|
| `controller/LeadController.java` | 3× `hasRole('USER') or hasRole('ADMIN')` → `hasAuthority('USER') or hasAuthority('ADMIN')` (**fix właściwy**) |
| `security/JwtAuthenticationFilter.java` | try/catch na `ExpiredJwtException`/`JwtException`/`UsernameNotFoundException` + logi WARN; wygasły token nie leci już jako **500**, tylko czysty 401 z powodem w atrybucie requestu |
| `config/SecurityConfig.java` | `authenticationEntryPoint` zwraca JSON z konkretnym powodem (`expired`/`invalid`/`userNotFound`) zamiast jednego mylącego zdania; dodany jawny `accessDeniedHandler` (403) |
| `test/.../LeadSecurityIntegrationTest.java` | nowy test regresyjny na realnym łańcuchu bezpieczeństwa |

## 4. Weryfikacja (deterministyczna, bez proda)

`@WebMvcTest(LeadController)` z prawdziwym `SecurityConfig` + filtrem JWT, mocki serwisów. Wynik: **5/5 PASS**.

| Test | Status | Dowodzi |
|---|---|---|
| `adminCanCreateLead` | 201 | authority `"ADMIN"` + `hasAuthority('ADMIN')` → fix działa |
| `userRoleCanCreateLead` | 201 | rola `USER` przechodzi |
| `authenticatedButWrongRoleGets403` | 403 | zła rola zalogowanego = **403**, nie 401 |
| `noTokenGets401` | 401 | brak tokenu → czytelny JSON |
| `malformedTokenGets401NotServerError` | 401 | zły token → 401, nie 500 |

**Rozstrzygnięcie 401 vs 403:** odmowa roli daje 403, więc snapshot 401 z ważnym tokenem pochodził z innego momentu/requestu. Niezależnie od tego fix odblokowuje leady, a hardening ujawni każdy przyszły powód 401 w logu.

## 5. Ustalenia techniczne

- Projekt celuje w **Java 17** (`pom.xml`), prod na 21. Lokalnie tylko JDK 25 i 17 → Mockito nie mockuje pod Javą 25; test uruchamiany z `-Dnet.bytebuddy.experimental=true`. Na CI/build ≤21 flaga niepotrzebna.
- `application-prod.properties`: `ddl-auto=update` **razem** z Flyway (`enabled=true`) — potencjalnie ryzykowne, do przeglądu osobno.
- Migracje V12/V13 **nie dotykały** tabel `users`/`roles`.

## 6. Do zrobienia osobno (poza zakresem hotfixa)

- Dependabot: 4 podatności na domyślnym branchu (2 high, 2 moderate).
- Frontend (`crm-frontend`) nie robi auto-wylogowania przy 401 — brak response interceptora; user widzi błędy zamiast redirectu na login.
- Rozważyć `hasAuthority` jako konwencję i ewentualny lint/test pilnujący braku `hasRole` w kodzie.

## 7. Git

- Commit: `f050f6c` — „Hotfix: naprawa autoryzacji leadow po wlaczeniu @EnableMethodSecurity"
- Push `9db9a93..f050f6c` na `origin/hotfix/flyway-v12-repair` — dopchnięto też 3 wiszące lokalnie hotfixy V13 (AUTO_INCREMENT).
- Bez atrybucji Claude (zgodnie z preferencją projektu).
