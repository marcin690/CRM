#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.lib.colors import HexColor, white
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, HRFlowable,
)
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import os
from datetime import datetime

FONT_DIR = "/System/Library/Fonts/Supplemental"
for name, file in [
    ("TNR", "Times New Roman.ttf"), ("TNR-Bold", "Times New Roman Bold.ttf"),
    ("TNR-Italic", "Times New Roman Italic.ttf"), ("TNR-BoldItalic", "Times New Roman Bold Italic.ttf"),
    ("Arial", "Arial.ttf"), ("Arial-Bold", "Arial Bold.ttf"), ("Arial-Italic", "Arial Italic.ttf"),
]:
    pdfmetrics.registerFont(TTFont(name, os.path.join(FONT_DIR, file)))
pdfmetrics.registerFontFamily("TNR", normal="TNR", bold="TNR-Bold", italic="TNR-Italic", boldItalic="TNR-BoldItalic")
pdfmetrics.registerFontFamily("Arial", normal="Arial", bold="Arial-Bold", italic="Arial-Italic")

PRIMARY = HexColor("#1a365d")
SECONDARY = HexColor("#2c5282")
ACCENT = HexColor("#3182ce")
LIGHT_BG = HexColor("#f7fafc")
BORDER = HexColor("#cbd5e0")
TEXT_DARK = HexColor("#1a202c")
TEXT_MED = HexColor("#4a5568")

OUTPUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Dokumentacja_Techniczna_CRM.pdf")

doc = SimpleDocTemplate(
    OUTPUT, pagesize=A4,
    topMargin=22 * mm, bottomMargin=22 * mm, leftMargin=22 * mm, rightMargin=22 * mm,
    title="Dokumentacja Techniczna Systemu CRM", author="Hiberus Poland Sp. z o.o.",
)
W = A4[0] - 44 * mm

# ── Styles ──
s_h1 = ParagraphStyle("H1", fontName="TNR-Bold", fontSize=14, leading=19, textColor=PRIMARY,
                       spaceBefore=7*mm, spaceAfter=3*mm)
s_h2 = ParagraphStyle("H2", fontName="TNR-Bold", fontSize=11.5, leading=16, textColor=SECONDARY,
                       spaceBefore=5*mm, spaceAfter=2*mm)
s_h3 = ParagraphStyle("H3", fontName="TNR-Bold", fontSize=10, leading=14, textColor=TEXT_DARK,
                       spaceBefore=3*mm, spaceAfter=1.5*mm)
s_body = ParagraphStyle("Body", fontName="TNR", fontSize=10, leading=14, textColor=TEXT_DARK,
                         alignment=TA_JUSTIFY, spaceAfter=2*mm)
s_th = ParagraphStyle("TH", fontName="Arial-Bold", fontSize=8, leading=11, textColor=white)
s_td = ParagraphStyle("TD", fontName="Arial", fontSize=8, leading=11, textColor=TEXT_DARK)
s_cap = ParagraphStyle("Cap", fontName="TNR-Italic", fontSize=8, leading=11, textColor=TEXT_MED,
                        alignment=TA_CENTER, spaceBefore=1*mm, spaceAfter=3*mm)
s_toc = ParagraphStyle("TOC", fontName="TNR", fontSize=10, leading=14, textColor=TEXT_DARK,
                        spaceAfter=1*mm, leftIndent=4*mm)
s_toc_sub = ParagraphStyle("TOCSub", fontName="TNR", fontSize=9, leading=13, textColor=TEXT_MED,
                            spaceAfter=0.8*mm, leftIndent=10*mm)

def section_hr():
    return HRFlowable(width="100%", thickness=1, color=PRIMARY, spaceBefore=0.5*mm, spaceAfter=2*mm)

def hr():
    return HRFlowable(width="100%", thickness=0.4, color=BORDER, spaceBefore=1*mm, spaceAfter=1*mm)

def tbl(headers, rows, widths=None):
    data = [[Paragraph(h, s_th) for h in headers]]
    for r in rows:
        data.append([Paragraph(str(c), s_td) for c in r])
    if not widths:
        widths = [W / len(headers)] * len(headers)
    t = Table(data, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0,0), (-1,0), PRIMARY), ("TEXTCOLOR", (0,0), (-1,0), white),
        ("GRID", (0,0), (-1,-1), 0.3, BORDER),
        ("ROWBACKGROUNDS", (0,1), (-1,-1), [white, LIGHT_BG]),
        ("VALIGN", (0,0), (-1,-1), "TOP"),
        ("TOPPADDING", (0,0), (-1,-1), 3), ("BOTTOMPADDING", (0,0), (-1,-1), 3),
        ("LEFTPADDING", (0,0), (-1,-1), 4), ("RIGHTPADDING", (0,0), (-1,-1), 4),
    ]))
    return t

page_num = [0]
def on_page(canvas, doc):
    page_num[0] += 1
    canvas.saveState()
    canvas.setStrokeColor(BORDER); canvas.setLineWidth(0.4)
    canvas.line(22*mm, 17*mm, A4[0]-22*mm, 17*mm)
    canvas.setFont("TNR", 7.5); canvas.setFillColor(TEXT_MED)
    canvas.drawString(22*mm, 13*mm, "Dokumentacja Techniczna — System CRM WH Plus")
    canvas.drawRightString(A4[0]-22*mm, 13*mm, f"Strona {page_num[0]}")
    canvas.line(22*mm, A4[1]-17*mm, A4[0]-22*mm, A4[1]-17*mm)
    canvas.setFont("TNR", 6.5)
    canvas.drawRightString(A4[0]-22*mm, A4[1]-15*mm, "Dokument poufny — Hiberus Poland Sp. z o.o.")
    canvas.restoreState()

def on_first_page(canvas, doc):
    pass

story = []

# ════════════════ STRONA TYTULOWA ════════════════
story.append(Spacer(1, 50*mm))
line = Table([[""]], colWidths=[W], rowHeights=[2])
line.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,-1),PRIMARY),("TOPPADDING",(0,0),(-1,-1),0),("BOTTOMPADDING",(0,0),(-1,-1),0)]))
story.append(line)
story.append(Spacer(1, 8*mm))
story.append(Paragraph("DOKUMENTACJA TECHNICZNA", ParagraphStyle("T1", fontName="TNR-Bold", fontSize=24, leading=30, textColor=PRIMARY, alignment=TA_CENTER, spaceAfter=4*mm)))
story.append(Paragraph("System CRM", ParagraphStyle("T2", fontName="TNR-Bold", fontSize=20, leading=26, textColor=ACCENT, alignment=TA_CENTER, spaceAfter=4*mm)))
story.append(Spacer(1, 3*mm))
story.append(line)
story.append(Spacer(1, 18*mm))
story.append(Paragraph("Wykonawca: Hiberus Poland Sp. z o.o.", ParagraphStyle("T3", fontName="TNR", fontSize=13, leading=18, textColor=SECONDARY, alignment=TA_CENTER, spaceAfter=3*mm)))
story.append(Paragraph("Zamawiający: WH Plus Sp. z o.o.", ParagraphStyle("T4", fontName="TNR", fontSize=13, leading=18, textColor=SECONDARY, alignment=TA_CENTER, spaceAfter=10*mm)))

meta = Table([
    ["Wersja dokumentu:", "1.0"],
    ["Data sporządzenia:", "grudzień 2025"],
    ["Klasyfikacja:", "Dokument poufny"],
    ["Wykonawca:", "Hiberus Poland Sp. z o.o."],
    ["NIP:", "8971893424"],
    ["KRS:", "0000909386"],
    ["Adres:", "pl. Teatralny 1/22, 50-051 Wrocław"],
], colWidths=[50*mm, 70*mm])
meta.setStyle(TableStyle([
    ("FONTNAME",(0,0),(0,-1),"TNR-Bold"), ("FONTNAME",(1,0),(1,-1),"TNR"),
    ("FONTSIZE",(0,0),(-1,-1),11), ("TEXTCOLOR",(0,0),(-1,-1),TEXT_DARK),
    ("TOPPADDING",(0,0),(-1,-1),2), ("BOTTOMPADDING",(0,0),(-1,-1),2),
    ("ALIGN",(0,0),(0,-1),"RIGHT"), ("LEFTPADDING",(1,0),(1,-1),8),
]))
story.append(meta)
story.append(PageBreak())

# ════════════════ SPIS TRESCI + WPROWADZENIE ════════════════
story.append(Paragraph("Spis treści", s_h1))
story.append(section_hr())
for num, title in [
    ("1.","Wprowadzenie"), ("2.","Architektura systemu"), ("3.","Model danych"),
    ("4.","Interfejs programistyczny (API REST)"), ("5.","Bezpieczeństwo i autoryzacja"),
    ("6.","Integracje zewnętrzne"), ("7.","Wymagania systemowe"),
]:
    story.append(Paragraph(f"<b>{num}</b>  {title}", s_toc))

story.append(Spacer(1, 6*mm))

# ── 1. WPROWADZENIE ──
story.append(Paragraph("1. Wprowadzenie", s_h1))
story.append(section_hr())
story.append(Paragraph(
    "Niniejszy dokument stanowi dokumentację techniczną systemu CRM zrealizowanego "
    "przez Hiberus Poland Sp. z o.o. na zlecenie firmy WH Plus Sp. z o.o. "
    "Opisuje architekturę, model danych, interfejs programistyczny "
    "oraz mechanizmy bezpieczeństwa zaimplementowane w aplikacji.", s_body))
story.append(Paragraph(
    "System CRM jest kompleksowym narzędziem do zarządzania procesem sprzedażowym w modelu B2B "
    "i B2C. Obejmuje pełny cykl życia klienta — od pozyskania leada, poprzez kwalifikację "
    "i konwersję, tworzenie ofert handlowych, aż po realizację projektów. Zapewnia śledzenie "
    "historii zmian (audit trail), zarządzanie zespołami sprzedażowymi oraz raportowanie "
    "statystyk ofertowych.", s_body))
story.append(Paragraph(
    "Aplikacja działa w architekturze klient-serwer: warstwa backendowa udostępnia REST API "
    "konsumowane przez aplikację frontendową SPA (Single Page Application). Komunikacja odbywa "
    "się za pośrednictwem protokołu HTTPS z uwierzytelnianiem tokenowym JWT.", s_body))

# ── 2. ARCHITEKTURA ──
story.append(Paragraph("2. Architektura systemu", s_h1))
story.append(section_hr())
story.append(Paragraph(
    "System zbudowano w oparciu o warstwową architekturę (Layered Architecture) zgodną "
    "z wzorcem MVC, z wyraźnym podziałem odpowiedzialności. Zastosowano warstwy DTO i Mapper "
    "dla separacji modelu domenowego od kontraktu API. Dodatkowo wykorzystano wzorce: "
    "Specification (dynamiczne zapytania filtrujące), Factory (generowanie danych testowych) "
    "oraz Observer (zdarzenia domenowe via ApplicationEventPublisher).", s_body))

story.append(tbl(["Warstwa", "Odpowiedzialność"], [
    ["Controller (prezentacja)", "Obsługa żądań HTTP, walidacja wejścia, serializacja JSON, kontrola dostępu"],
    ["Service (logika biznesowa)", "Reguły biznesowe, transakcje, orkiestracja operacji, obsługa zdarzeń"],
    ["Repository (dane)", "Abstrakcja dostępu do bazy danych (Spring Data JPA), wzorzec Specification"],
    ["Mapper", "Automatyczna konwersja encja ↔ DTO (MapStruct)"],
    ["Model (domena)", "Encje JPA, relacje, typy wyliczeniowe, mechanizmy audytu i wersjonowania"],
], [38*mm, W-38*mm]))
story.append(Paragraph("Tabela 1. Warstwy architektury systemu", s_cap))

story.append(Paragraph("2.1. Stos technologiczny", s_h2))
story.append(tbl(["Komponent", "Technologia", "Wersja"], [
    ["Język programowania", "Java (OpenJDK)", "17 LTS"],
    ["Framework aplikacyjny", "Spring Boot", "3.3.0"],
    ["ORM / Audyt encji", "Hibernate / Spring Data JPA / Envers", "6.x"],
    ["Baza danych", "MySQL", "8.0"],
    ["Migracje schematu", "Flyway", "10.18.0"],
    ["Uwierzytelnianie", "Spring Security + JWT (JJWT)", "0.11.5"],
    ["Mapowanie DTO", "MapStruct", "1.5.3"],
    ["System budowania", "Apache Maven", "3.9.x"],
    ["Przechowywanie plików", "MinIO (S3-compatible)", "8.5.7"],
    ["Redukcja kodu", "Lombok", "1.18.32"],
], [40*mm, 58*mm, W-98*mm]))
story.append(Paragraph("Tabela 2. Wykorzystane technologie", s_cap))

story.append(Paragraph("2.2. Struktura pakietów", s_h2))
story.append(Paragraph(
    "Kod źródłowy jest zorganizowany w pakiecie <b>wh.plus.crm</b> zgodnie z konwencją Maven:", s_body))
story.append(tbl(["Pakiet", "Przeznaczenie"], [
    ["config/", "Konfiguracja Spring: Security, CORS, JPA Auditing, inicjalizacja danych"],
    ["controller/", "19 kontrolerów REST obsługujących endpointy API"],
    ["model/", "Encje JPA, typy wyliczeniowe, klasy bazowe audytu"],
    ["dto/ + mapper/", "Obiekty transferu danych i mappery MapStruct (encja ↔ DTO)"],
    ["service/", "Logika biznesowa i zarządzanie transakcjami"],
    ["repository/", "Interfejsy Spring Data JPA"],
    ["security/", "Filtr JWT, JwtUtil, konfiguracja uwierzytelniania"],
    ["specyfications/", "Specyfikacje JPA do dynamicznych zapytań filtrujących"],
    ["events/ + scheduler/", "Zdarzenia domenowe i zadania cykliczne"],
    ["exception/", "Globalna obsługa wyjątków"],
], [30*mm, W-30*mm]))
story.append(Paragraph("Tabela 3. Struktura pakietów aplikacji", s_cap))

# ── 3. MODEL DANYCH ──
story.append(Paragraph("3. Model danych", s_h1))
story.append(section_hr())
story.append(Paragraph(
    "System operuje na powiązanych encjach JPA odwzorowujących proces sprzedażowy, "
    "mapowanych na tabele MySQL przez Hibernate ORM.", s_body))

story.append(Paragraph("3.1. Encje główne", s_h2))
story.append(tbl(["Encja", "Opis", "Kluczowe atrybuty"], [
    ["Lead", "Potencjalny klient — dane kontaktowe, typ (biuro projektowe, klient prywatny, sieć hotelowa, firma budowlana), wartość, źródło pozyskania, status kwalifikacji",
     "clientGlobalId, clientType, fullName, leadValue, roomsQuantity, executionDate, isFinal"],
    ["Client", "Klient po konwersji z leada — dane firmowe, NIP, notatki. Centralny węzeł łączący kontakty, wydarzenia, oferty i projekty",
     "clientFullName, businessName, address, email, phone, vatNumber"],
    ["Offer", "Oferta handlowa — waluty PLN/EUR z kursem wymiany, statusy (szkic/wysłana/zaakceptowana/odrzucona/podpisana), poziom szansy sprzedażowej",
     "totalPrice, currency, offerStatus, investorType, objectType, salesOpportunityLevel"],
    ["OfferItem", "Pozycja oferty — kwota netto, ilość, stawka VAT (0/5/23%), automatyczne obliczanie brutto",
     "title, amount, quantity, tax, grossAmount, taxAmount"],
    ["Project", "Projekt realizacyjny — parametry techniczne: pokoje, piętra, typ ścian, dojazd, praca weekendowa/nocna",
     "name, roomQuantity, projectNetValue, floorCount, wallType, accommodationOption"],
    ["User", "Użytkownik systemu z rolą i zespołem sprzedażowym, implementuje UserDetails Spring Security",
     "username, email, fullname, isSalesRepresentative, roles"],
    ["Comment", "Komentarz do dowolnej encji z analizą sentymentu, możliwość generowania automatycznego",
     "content, commentSentiment, entityType, automatic"],
    ["Event", "Wydarzenie/zadanie kalendarzowe powiązane z klientem lub projektem, obsługuje cykliczność",
     "date, comment, entityType, cycleType"],
], [18*mm, 57*mm, W-75*mm]))
story.append(Paragraph("Tabela 4. Encje główne systemu CRM", s_cap))

story.append(Paragraph("3.2. Relacje między encjami", s_h2))
story.append(tbl(["Relacja", "Typ", "Opis"], [
    ["Lead → User", "ManyToOne", "Przypisanie leada do opiekuna handlowego"],
    ["Lead → LeadStatus / LeadSource", "ManyToOne", "Status kwalifikacji i źródło pozyskania"],
    ["Client → Contact / Event / Offer / Project", "OneToMany", "Powiązania klienta z kontaktami, wydarzeniami, ofertami i projektami"],
    ["Offer → OfferItem", "OneToMany", "Pozycje składowe oferty (cascade, orphan removal)"],
    ["Offer → Client / Lead / Project / SalesTeam", "ManyToOne", "Powiązania oferty z encjami nadrzędnymi"],
    ["User → Role", "ManyToMany", "Role: USER, ADMIN, SALESPERSON, ADMINISTRATION"],
    ["User → SalesTeam", "ManyToOne", "Przynależność do zespołu sprzedażowego"],
], [42*mm, 20*mm, W-62*mm]))
story.append(Paragraph("Tabela 5. Relacje między encjami", s_cap))

story.append(Paragraph("3.3. Mechanizm audytu i wersjonowania", s_h2))
story.append(Paragraph(
    "Wszystkie główne encje dziedziczą po klasie <b>Auditable&lt;String&gt;</b>, automatycznie "
    "rejestrując: <i>createdBy</i>, <i>creationDate</i>, <i>lastModifiedBy</i>, "
    "<i>lastModifiedDate</i> (Spring Data JPA Auditing z AuditorAware). "
    "Encja Lead jest dodatkowo opatrzona adnotacją <b>@Audited</b> (Hibernate Envers), "
    "co zapewnia pełne śledzenie historii zmian w dedykowanych tabelach rewizji. "
    "Blokowanie optymistyczne (<b>@Version</b>) chroni przed jednoczesną modyfikacją "
    "rekordu przez wielu użytkowników.", s_body))

# ── 4. API REST ──
story.append(Paragraph("4. Interfejs programistyczny (API REST)", s_h1))
story.append(section_hr())
story.append(Paragraph(
    "System udostępnia REST API zgodne z konwencją RESTful. Wszystkie endpointy "
    "(oprócz uwierzytelniania) wymagają tokena JWT w nagłówku <i>Authorization</i>. "
    "Odpowiedzi w formacie JSON.", s_body))

story.append(Paragraph("4.1. Moduły biznesowe", s_h2))
story.append(tbl(["Metoda", "Endpoint", "Opis"], [
    ["GET", "/leads", "Leady z paginacją i filtrami (data, pracownik, status, wyszukiwanie)"],
    ["POST", "/leads", "Utworzenie leada (wymaga roli USER lub ADMIN)"],
    ["PATCH", "/leads/{id}", "Częściowa aktualizacja leada"],
    ["POST", "/leads/{id}/convert", "Konwersja leada do klienta"],
    ["DELETE", "/leads", "Usunięcie leadów (operacja zbiorcza)"],
    ["GET", "/clients", "Lista klientów z paginacją"],
    ["GET", "/clients/search", "Wyszukiwanie klientów (nazwa, email, telefon, NIP)"],
    ["GET", "/clients/{id}", "Klient ze szczegółami (projekty, oferty)"],
    ["POST / PUT / DELETE", "/clients[/{id}]", "CRUD operacje na kliencie"],
    ["GET", "/offers", "Lista ofert z paginacją"],
    ["GET", "/offers/search", "Filtrowanie ofert (typ, status, użytkownik, daty, szansa sprzedażowa)"],
    ["POST / PATCH", "/offers[/{id}]", "Tworzenie i aktualizacja ofert"],
    ["PUT", "/offers/{id}/status", "Zmiana statusu oferty"],
    ["GET", "/offers/statistics", "Statystyki ofertowe (opcjonalny zakres dat)"],
    ["GET", "/projects", "Lista projektów z paginacją"],
    ["GET", "/projects/search", "Wyszukiwanie projektów"],
    ["POST / PATCH / DELETE", "/projects[/{id}]", "CRUD operacje na projekcie"],
], [28*mm, 38*mm, W-66*mm]))
story.append(Paragraph("Tabela 6. Endpointy modułów biznesowych", s_cap))

story.append(Paragraph("4.2. Pozostałe endpointy", s_h2))
story.append(tbl(["Metoda", "Endpoint", "Opis"], [
    ["POST", "/auth/login", "Uwierzytelnienie i wydanie tokena JWT"],
    ["POST", "/auth/register", "Rejestracja użytkownika"],
    ["POST", "/auth/reset-password", "Reset hasła"],
    ["GET", "/users", "Lista użytkowników systemu"],
    ["GET", "/users/sellers", "Lista przedstawicieli handlowych"],
    ["GET/POST/PUT/DEL", "/comments[/{id}]", "Zarządzanie komentarzami (CRUD, filtrowanie wg encji)"],
    ["GET/POST/PATCH/DEL", "/events[/{id}]", "Zarządzanie wydarzeniami i kalendarzem"],
    ["GET", "/contacts/client/{id}", "Osoby kontaktowe klienta"],
    ["GET", "/sales-team", "Lista zespołów sprzedażowych"],
    ["POST", "/uploads", "Przesyłanie plików do MinIO (pojedynczo lub zbiorowo)"],
    ["POST", "/api/import/leads", "Import leadów z systemu zewnętrznego"],
    ["GET", "/lead-status", "Słownik statusów leadów"],
], [28*mm, 38*mm, W-66*mm]))
story.append(Paragraph("Tabela 7. Endpointy pomocnicze i systemowe", s_cap))

# ── 5. BEZPIECZENSTWO ──
story.append(Paragraph("5. Bezpieczeństwo i autoryzacja", s_h1))
story.append(section_hr())
story.append(Paragraph(
    "System bezpieczeństwa oparto na Spring Security z mechanizmem uwierzytelniania "
    "bezstanowego (stateless) — tokeny JWT (JSON Web Token). Użytkownik przesyła dane "
    "logowania na endpoint <i>/auth/login</i>, serwer weryfikuje hasło (BCrypt) "
    "i generuje token JWT (algorytm HS256, ważność 10 godzin). Token jest wymagany "
    "w nagłówku <i>Authorization: Bearer &lt;token&gt;</i> każdego kolejnego żądania.", s_body))

story.append(Paragraph(
    "System implementuje model RBAC (Role-Based Access Control) z czterema rolami:", s_body))
story.append(tbl(["Rola", "Zakres uprawnień"], [
    ["USER", "Podstawowy dostęp, tworzenie i edycja leadów"],
    ["ADMIN", "Pełny dostęp administracyjny, zarządzanie użytkownikami i konfiguracją"],
    ["SALESPERSON", "Dostęp do modułów sprzedażowych dedykowany dla handlowców"],
    ["ADMINISTRATION", "Dostęp do raportów, statystyk, funkcji administracyjno-biurowych"],
], [30*mm, W-30*mm]))
story.append(Paragraph("Tabela 8. Role systemowe", s_cap))

story.append(Paragraph(
    "Konfiguracja CORS ogranicza dostęp do zaufanych domen: środowisko produkcyjne "
    "(crm.w-h.pl, whplus.com.pl), stagingowe (Azure Static Web Apps) oraz deweloperskie "
    "(localhost:3000). Hasła przechowywane w postaci skrótu kryptograficznego BCrypt.", s_body))

# ── 6. INTEGRACJE ──
story.append(Paragraph("6. Integracje zewnętrzne", s_h1))
story.append(section_hr())
story.append(tbl(["Integracja", "Opis"], [
    ["MinIO (magazyn plików)", "Serwer obiektowy kompatybilny z S3. Zasobnik crm-uploads na media.w-h.pl. Limit: 10 MB/plik, 50 MB/żądanie."],
    ["Flyway (migracje bazy)", "Automatyczne wykonywanie skryptów SQL przy uruchomieniu z katalogu db/migration. Wersjonowanie schematu."],
    ["Import danych", "Dedykowane kontrolery (LeadImport, ClientImport, OfferImport) do migracji z poprzedniego systemu z walidacją i mapowaniem pól."],
    ["E-mail / SMS / Powiadomienia", "EmailService (SMTP), SmsService, NotificationService z szablonami i harmonogramem (BatchNotificationScheduler)."],
], [35*mm, W-35*mm]))
story.append(Paragraph("Tabela 9. Integracje zewnętrzne", s_cap))

# ── 7. WYMAGANIA ──
story.append(Paragraph("7. Wymagania systemowe", s_h1))
story.append(section_hr())

story.append(Paragraph("Wymagania sprzętowe serwera", s_h3))
story.append(tbl(["Parametr", "Wymaganie"], [
    ["Procesor", "Min. 2 rdzenie (zalecane 4)"],
    ["Pamięć RAM", "Min. 4 GB (zalecane 8 GB)"],
    ["Dysk", "Min. 20 GB SSD (bez plików MinIO)"],
    ["System operacyjny", "Linux (Ubuntu 22.04 LTS / CentOS 8+) lub Windows Server 2019+"],
    ["Sieć", "Stałe połączenie, certyfikat SSL/TLS"],
], [30*mm, W-30*mm]))
story.append(Paragraph("Tabela 10. Wymagania sprzętowe", s_cap))

story.append(Paragraph("Wymagania programowe", s_h3))
story.append(tbl(["Komponent", "Wersja"], [
    ["Java Runtime", "OpenJDK 17 LTS lub nowszy"],
    ["MySQL", "8.0 lub nowszy"],
    ["MinIO", "Najnowsza stabilna"],
    ["Docker + Docker Compose", "24.x (opcjonalnie)"],
    ["Reverse proxy", "Nginx 1.24+ lub Apache 2.4+"],
], [30*mm, W-30*mm]))
story.append(Paragraph("Tabela 11. Wymagania programowe", s_cap))

story.append(Paragraph("Środowiska wdrożeniowe", s_h3))
story.append(Paragraph(
    "Aplikacja obsługuje trzy środowiska zarządzane profilami Spring Boot: "
    "<b>deweloperskie</b> (lokalna baza MySQL, logowanie SQL, localhost:3000), "
    "<b>produkcyjne</b> (dedykowany serwer MySQL, HTTPS, domena crm.w-h.pl, MinIO na media.w-h.pl) "
    "oraz <b>stagingowe</b> (Azure Static Web Apps).", s_body))

# ── STOPKA DOKUMENTU ──
story.append(Spacer(1, 8*mm))
story.append(hr())
story.append(Spacer(1, 3*mm))
story.append(Paragraph(
    "Niniejszy dokument został opracowany przez Hiberus Poland Sp. z o.o. na zlecenie "
    "WH Plus Sp. z o.o. i podlega ochronie prawnej. Kopiowanie lub udostępnianie "
    "osobom trzecim bez pisemnej zgody jest zabronione.",
    ParagraphStyle("Disc", fontName="TNR-Italic", fontSize=8, leading=11, textColor=TEXT_MED, alignment=TA_CENTER, spaceAfter=3*mm)))

mf = Table([
    ["Sporządził:", "Hiberus Poland Sp. z o.o."],
    ["NIP:", "8971893424"],
    ["Adres:", "pl. Teatralny 1/22, 50-051 Wrocław"],
    ["Data:", "grudzień 2025"],
    ["Wersja:", "1.0"],
], colWidths=[28*mm, 65*mm])
mf.setStyle(TableStyle([
    ("FONTNAME",(0,0),(0,-1),"TNR-Bold"), ("FONTNAME",(1,0),(1,-1),"TNR"),
    ("FONTSIZE",(0,0),(-1,-1),8.5), ("TEXTCOLOR",(0,0),(-1,-1),TEXT_MED),
    ("TOPPADDING",(0,0),(-1,-1),1.5), ("BOTTOMPADDING",(0,0),(-1,-1),1.5),
    ("LEFTPADDING",(1,0),(1,-1),6),
]))
story.append(mf)

doc.build(story, onFirstPage=on_first_page, onLaterPages=on_page)
print(f"PDF wygenerowany: {OUTPUT}")
