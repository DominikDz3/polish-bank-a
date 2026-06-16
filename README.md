# Polski Bank A

Projekt grupowy z przedmiotu Aplikacje biznesowe moduł **Bank Polski / Bank A**, mający na celu stworzenie aplikacji webowej symulującej działanie polskiego banku. Głównym motywem jest integracja różnych modeli płatności.

## Spis treści

1. [Opis projektu](#opis-projektu)
2. [Zakres projektu](#zakres-projektu)
3. [Stos technologiczny](#stos-technologiczny)
4. [Wiedza domenowa](#wiedza-domenowa)
5. [Diagramy](#diagramy)
6. [Architektura](#architektura)
7. [Struktura projektu](#struktura-projektu)
8. [Uruchomienie](#uruchomienie)
9. [Zespół](#zespół)

## 1. Opis projektu
Polski Bank A to aplikacja bankowa umożliwiająca obsługę różnych typów przelewów: wewnętrznych, międzybankowych (ELIXIR/SEPA), natychmiastowych, SORBNET oraz międzybankowych SWIFT. APlikacja integruje się z zewnętrznymi systemami rozliczeniowymi i dostawcami kart płatniczych.

## 2. Zakres projektu

Zakres funkcjonalności projektu objemuje:
- **Przelewy wewnętrzene** - przelewy między kontami realizowane w obrębie tego banku
- **ELIXIR** - standardowe rozliczenie międzybankowe realizowane w sesjach dziennych
- **Express ElIXIR** - natychmiastowy przelew międzybankowy, umożliwiający transfer środków w kilka sekund w tybie 24/7/365
- **SORBNET3** - rozliczenie międzybankowe typu RTGS, służący do przetwarzania wysokokwotowych przelewów w czasie rzeczywistym
- **SWIFT** - globalny system do realizacji bezpiecznych przelewów zagranicznych
- **BLIK** - bezpieczne i błyskawiczne transakcje bez użycia karty lub gotówki, używając generowanego sześciocyfrowego kodu w aplikacji bankowej
- **Karty płatnicze** - integracja z płatnoścami za pośrednictwem kart, transakcje w PLN
- **Konto Junior (7-13lat)** - konto podpięte pod konto rodzica a wszystkie transakcje wymagają jego zatwierdzenia

## 3. Stos technologiczny
| Warstwa        | Technologia              |
|----------------|--------------------------|
| Backend        | Java 21 + Spring Boot    |
| Frontend       | React (TypeScript)+ Vite |
| Baza danych    | PostgreSQL               |
| Auth           | Spring Security          |
| Konteneryzacja | Docker + Docker Compose  |
| API docs       | Swagger                  |

## 4. Wiedza domenowa
Niniejsza sekcja dokumentacji gromadzi kluczową wiedzę biznesową i techniczną niezbędną do zaprojektowania oraz wdrożenia modułów transakcyjnych aplikacji bankowej. 

### 4.1 ELIXIR
Elixir jest to podstawowy ssystem elektronicznych rozliczeń międzybankowych w Polsce (zarządzany przez KIR). Odpowiada za masową obsługę standardowych przelewów krajowych w PLN. Działa w oparciu o sesje (zazwyczaj 3 razy dziennie w dni robocze), co oznacza że środki trafiają do odbiorcy w ciągu kilku godzin.

#### 4.1.1 Architektura

Opiera się na wymianie zaszyfrowanych paczek danych pomiędzy bankami a KIR najczęściej poprzerz protokół SFTP. Dane są szyfrowane a pliki podpisywane elektronicznie

System bankowy (BANK A) -> SFTP -> KIR -> SFTP -> System bankowy (BANK B)

#### 4.1.2 Sytuacje brzegowe
 1. Niewypłacalność lub brak płynności banku w trakcie sesji
  - **Sytuacja:** Bank nadawcy wysłał paczkę z przelewami ale w momencie rozrachunku sesji okazuje się, że nie ma wystarczających środków na swoim rachunku rezerwy obowiązkowej w NBP.
  - **Obsługa:** Uruchamiany jest mechanizm gwarancyjny. Kir posiada fundusz gwarancyjny z którego pokrywa ewentualne niedobory aby sesja mogła się odbyć dla reszty rynku. Natomiast jeżeli braki są drastyczne KIR może użyć tzw. unwinding - odkręcenie transakcji czyli wykluczenie z sesji przelewów od tego konkretnego banku.
    
2. Przelew na konto, które nie istnieje (lub jest zamknięte)
  - **Sytuacja:** Użytkownik wpisuje poprawny matematycznie numer NRB (suma kontrolna modulo 97 się zgadza, kod banku jest poprawny), ale rachunek w banku docelowym został zamknięty, zajęty i zablokowany lub nigdy nie istniał
  - **Obsługa:** Aplikacja wypuści przelew lecz bank odbiorcy ma obowiązek (najczęściej w kolejnej najbliższej sesji Elixir) odesłać te środki z powrotem za pomocą komunikatu zwrotu (return). System musi asynchronicznie nasłuchiwać komunikaty zwrotne z KIR. gdy taki nadejdzie, aplikacja musi atomatycznie rozpoznać oryginalną transakcję, zaksięgować środki zpowrotem na saldo kliena i wygenerować stosowne powiadomienie.
    
3. Twardy limit kwotowy
  - **Sytuacja** Użytkownik próbuje wysłać przelew na kwotę równą lub wyższą niż milion złotych.
  - **Obsługa** Regulamin Kir dla systemu Elixir odrzuca takie transakcje ponieważ limit to dokładnie 999 999,99 PLN. W takim wypadku walidacja musi nastąpić jeszcze zanim żądanie w ogóle trafi do kolejki wychodzącej.

### 4.2 ELIXIR
*TBD*

### 4.3 Express ELIXIR
*TBD*

### 4.4 SORBNET3
*TBD*

### 4.5 SWIFT
*TBD*

### 4.6 BLIK
*TBD*

### 4.7 Karty płatnicze
*TBD*

## 5. Diagramy

### 5.1 Diagram Przypadków Użycia (Use Case)

``` mermaid
flowchart LR

    %% AKTORZY
    s
    Junior["Junior"]
    Rodzic["Rodzic"]
    Klient["Klient indywidualny"]
    
    Junior ~~~ Rodzic
    Rodzic ~~~ Klient

    %% Dziedziczenie
    Rodzic -.->|dziedziczy| Klient

    Pracownik["Pracownik banku"]
    SystemAML["System AML (Zewn.)"]

    %% SYSTEM BANKOWY

    subgraph Bank["Bank Polski A - System Bankowy"]
        
        subgraph Modul_Konta_i_Kart ["Moduł Kont, Kart i Dostępu"]
            direction TB
            UC_Zalozenie(["Założenie konta"])
            UC_Logowanie(["Logowanie do bankowości"])
            UC_LogowanieInne(["Logowanie przez inny bank (Open Banking)"])
            UC_Blik(["Obsługa BLIK (Generowanie i płatność)"])
            UC_PlatnoscKarta(["Płatność kartą (Debet/Kredyt)"])
            UC_Blokada(["Blokada karty płatniczej"])
            UC_Limit(["Zmiana limitów transakcyjnych"])
        end
        
        subgraph Modul_Przelewow ["Moduł Przelewów i Bezpieczeństwa"]
            direction TB
            UC_PrzelewWewn(["Zlecenie przelewu wewnętrznego"])
            UC_PrzelewSwift(["Zlecenie przelewu SWIFT"])
            UC_PrzelewNatychmiast(["Zlecenie przelewu Express Elixir"])
            UC_PrzelewZwykly(["Zlecenie przelewu ELIXIR"])
            UC_Przelew(["Zlecenie przelewu (Baza)"])
            
            UC_WeryfikacjaAML(["Weryfikacja transakcji przez algorytmy"])
            UC_Wyjasnienia(["Złożenie wyjaśnień do wstrzymanego przelewu"])
        end
        
        subgraph Modul_Juniora ["Strefa Junior"]
            direction TB
            UC_KontoJunior(["Otwarcie subkonta Juniora"])
            UC_LimitJunior(["Ustalanie limitów dla Juniora"])
            UC_InicjacjaJunior(["Zainicjowanie transakcji (Junior)"])
            UC_PrepaidJunior(["Płatność kartą Prepaid (Junior)"])
            UC_ZatwierdzenieJunior(["Zatwierdzenie / Odrzucenie transakcji"])
        end
        
        subgraph Modul_Pracownika ["Moduł Backoffice (Pracowniczy)"]
            direction TB
            UC_Monitorowanie(["Monitorowanie stanów rachunków"])
            UC_DecyzjaAML(["Decyzja o uwolnieniu/blokadzie środków AML"])
            UC_Raporty(["Przeglądanie raportów rozliczeniowych"])
            UC_Alerty(["Zarządzanie alertami płynnościowymi"])
        end
        
    end

    %% RELACJE AKTORÓW
    
    %% Relacje: Klient
    Klient --- UC_Zalozenie
    Klient --- UC_Logowanie
    Klient --- UC_LogowanieInne
    Klient --- UC_Blik
    Klient --- UC_PlatnoscKarta
    Klient --- UC_Limit
    Klient --- UC_Blokada
    Klient --- UC_Przelew
    Klient --- UC_Wyjasnienia

    %% Relacje: Rodzic (dodatkowe)
    Rodzic --- UC_KontoJunior
    Rodzic --- UC_LimitJunior
    Rodzic --- UC_ZatwierdzenieJunior

    %% Relacje: Junior
    Junior --- UC_Logowanie
    Junior --- UC_Blik
    Junior --- UC_InicjacjaJunior
    Junior --- UC_PrepaidJunior

    %% Relacje: Pracownik i System
    UC_WeryfikacjaAML --- SystemAML
    UC_Monitorowanie --- Pracownik
    UC_DecyzjaAML --- Pracownik
    UC_Raporty --- Pracownik
    UC_Alerty --- Pracownik

    %% ZALEŻNOŚCI LOGICZNE
    
    UC_PrzelewWewn --> UC_Przelew
    UC_PrzelewSwift --> UC_Przelew
    UC_PrzelewNatychmiast --> UC_Przelew
    UC_PrzelewZwykly --> UC_Przelew
    
    UC_Przelew -.->|"<<include>>"| UC_WeryfikacjaAML
    UC_ZatwierdzenieJunior -.->|"<<extend>>"| UC_InicjacjaJunior
```

Diagram przypadków użycia modeluje interakcje pomiędzy aktorami, a funkcjonalnościami systemu bankowego.

####  Aktorzy i ich uprawnienia:
* **Klient indywidualny:** Główny aktor w systemie. Posiada pełny dostęp do standardowej bankowości: logowanie (w tym Open Banking), realizacja przelewów, obsługa BLIK, zarządzanie kartami płatniczymi oraz limitami transakcyjnymi.

* **Rodzic:** Aktor specyficzny, który dziedziczy wszystkie uprawnienia Klienta indywidualnego, a dodatkowo posiada rozszerzone przywileje: możliwość otwarcia subkonta Juniora, zarządzanie jego limitami oraz autoryzację (zatwierdzanie/odrzucanie) transakcji inicjowanych przez dziecko.

* **Junior (7-13 lat):** Aktor o mocno ograniczonym dostępie. Posiada własne dane do logowania, może realizować płatności kartą Prepaid oraz inicjować przelewy i transakcje BLIK. Nie może jednak samodzielnie sfinalizować przelewu – jego akcje trafiają do "poczekalni".

* **Pracownik banku:** Aktor wewnętrzny. Odpowiada za monitorowanie płynności finansowej, analizę raportów rozliczeniowych oraz ręczne podejmowanie decyzji o zwolnieniu środków zablokowanych przez filtry bezpieczeństwa.

* **System AML (Zewnętrzny):** Zautomatyzowany aktor algorytmiczny, który weryfikuje każdą wychodzącą transakcję pod kątem ryzyka prania brudnych pieniędzy (Anti-Money Laundering) i ewentualnych sankcji (np. przelewy SWIFT wysokiego ryzyka).



### 5.2 Diagram Związków Encji (ERD)

```mermaid
erDiagram
    %% ==========================================
    %% GŁÓWNE TABELE 
    %% ==========================================
    USERS {
        UUID id PK
        varchar customer_number "UNIQUE, 8 znaków"
        varchar first_name
        varchar last_name
        varchar email "UNIQUE"
        varchar password_hash
        varchar phone_number "UNIQUE"
        date date_of_birth
        varchar role "ADMIN, CUSTOMER, JUNIOR"
        varchar pin_hash "BCrypt, NULL = nieustawiony"
        int pin_failed_attempts "licznik nieudanych prób"
        timestamp pin_locked_until "blokada PIN do tego czasu"
    }

    ACCOUNTS {
        UUID id PK
        UUID user_id FK
        varchar account_number "UNIQUE"
        decimal balance
        decimal blocked_funds
        varchar currency
        varchar type "STANDARD, JUNIOR, BANK_INTERNAL"
        UUID parent_account_id FK "Może być NULL"
    }

    CARDS {
        UUID id PK
        UUID account_id FK
        varchar card_number "deprecated, używany maskowany"
        varchar masked_pan "4100 01** **** 5852"
        varchar provider_token "UNIQUE, klucz integracji z providerem"
        varchar provider_status "REQUESTED, PRODUCING, SHIPPED, ACTIVE, BLOCKED"
        varchar bin_prefix
        decimal transaction_limit
        decimal daily_limit "Limit dzienny (NULL = brak)"
        varchar currency
        date expiry_date
        varchar type "VIRTUAL, PHYSICAL, PREPAID"
        boolean is_blocked
    }

    CARD_AUTHORIZATIONS {
        UUID id PK
        varchar authorization_code "UNIQUE"
        varchar external_transaction_id "UNIQUE, idempotencja webhooków"
        UUID card_id FK
        UUID account_id FK
        decimal amount
        varchar currency
        varchar merchant_name
        varchar status "HELD, SETTLED, REFUNDED, EXPIRED"
        timestamp created_at
        timestamp settled_at
    }

    TRANSACTIONS {
        UUID id PK
        UUID sender_account_id FK "NULL jeśli z zewnątrz"
        varchar sender_account_number
        UUID receiver_account_id FK "NULL jeśli na zewnątrz"
        varchar receiver_account_number
        varchar receiver_bank_bic
        varchar receiver_name
        varchar title
        decimal amount
        varchar currency
        varchar status "COMPLETED, PENDING, PENDING_APPROVAL, REJECTED, HELD_FOR_AML"
        varchar type "INTERNAL, CARD_PAYMENT, CARD_PAYMENT_REJECTED, CARD_TOPUP, KLIK"
        varchar external_payment_id
        timestamp created_at
        timestamp execution_date
        UUID card_id FK "NULL jeśli nie płatność kartą"
    }

    %% ==========================================
    %% TABELE ROZSZERZAJĄCE 
    %% ==========================================
    PENDING_APPROVALS {
        UUID id PK
        UUID junior_account_id FK
        UUID parent_user_id FK
        UUID transaction_id FK
        decimal amount
        varchar description
        varchar status "PENDING, APPROVED, REJECTED"
        timestamp created_at
        timestamp resolved_at
    }

    KLIK_CODES {
        UUID id PK
        UUID user_id FK
        UUID account_id FK
        varchar code "6 cyfr"
        decimal amount
        varchar status "ACTIVE, USED, EXPIRED"
        timestamp created_at
        timestamp expires_at
        timestamp used_at
        varchar idempotency_key
    }

    KLIK_ALIASES {
        UUID id PK
        UUID user_id FK
        UUID account_id FK
        varchar alias "UNIQUE, numer telefonu"
        boolean active
        timestamp created_at
    }

    KLIK_AUTHORIZATIONS {
        UUID id PK
        UUID user_id FK
        UUID account_id FK
        varchar external_id "ID od dostawcy KLIK"
        decimal amount
        varchar currency
        varchar merchant
        varchar status "PENDING, CONFIRMED, REJECTED, EXPIRED"
        timestamp created_at
        timestamp resolved_at
    }

    AML_HOLDS {
        UUID id PK
        UUID account_id FK
        UUID transaction_id FK
        varchar reason
        text client_explanation
        varchar status "ACTIVE, RELEASED"
        varchar created_by
        timestamp created_at
        timestamp released_at
    }

    %% ==========================================
    %% RELACJE
    %% ==========================================
    USERS ||--o{ ACCOUNTS : "posiada"
    ACCOUNTS ||--o{ ACCOUNTS : "ma subkonta (Junior)"

    ACCOUNTS ||--o{ CARDS : "jest podpięte pod"
    CARDS ||--o{ CARD_AUTHORIZATIONS : "ma autoryzacje"
    ACCOUNTS ||--o{ CARD_AUTHORIZATIONS : "obciąża"

    ACCOUNTS ||--o{ TRANSACTIONS : "wysyła (sender)"
    ACCOUNTS ||--o{ TRANSACTIONS : "odbiera (receiver)"
    CARDS ||--o{ TRANSACTIONS : "obciąża"

    ACCOUNTS ||--o{ PENDING_APPROVALS : "wymaga akceptacji (Junior)"
    USERS ||--o{ PENDING_APPROVALS : "akceptuje (Rodzic)"
    TRANSACTIONS ||--o| PENDING_APPROVALS : "dotyczy (opcjonalnie)"

    USERS ||--o{ KLIK_CODES : "generuje"
    ACCOUNTS ||--o{ KLIK_CODES : "obciąża"
    USERS ||--o{ KLIK_ALIASES : "rejestruje"
    ACCOUNTS ||--o{ KLIK_ALIASES : "wskazuje na"
    USERS ||--o{ KLIK_AUTHORIZATIONS : "autoryzuje płatność"
    ACCOUNTS ||--o{ KLIK_AUTHORIZATIONS : "obciąża"

    ACCOUNTS ||--o{ AML_HOLDS : "ma zablokowane środki"
    TRANSACTIONS ||--o| AML_HOLDS : "jest zablokowana przez AML"


```
Schemat bazy danych (zaprojektowany dla PostgreSQL) stanowi fundament aplikacji. Architektura została w pełni znormalizowana i zoptymalizowana pod kątem bezpieczeństwa transakcyjnego oraz audytowalności operacji finansowych.

Zamiast standardowych identyfikatorów numerycznych, we wszystkich tabelach zastosowano klucze główne typu **UUID**. Jest to kluczowy mechanizm obronny zapobiegający atakom typu IDOR i uniemożliwiający wyliczanie (enumerację) wielkości bazy klientów przez osoby nieuprawnione.

Baza danych została podzielona na **6 logicznych podsystemów**:

#### 1. Rdzeń Systemu

* **USERS:** Przechowuje kluczowe dane autoryzacyjne oraz profilowe. Ograniczenia UNIQUE nałożone na `customer_number` (8-cyfrowy CIF) oraz `email` gwarantują spójność tożsamości klienta. Pole `date_of_birth` pozwala algorytmom na dynamiczną weryfikację wieku (niezbędne przy kontach Junior). Tabela posiada również pola obsługi kodu PIN: `pin_hash` (BCrypt), `pin_failed_attempts` (licznik nieudanych prób) i `pin_locked_until` (czasowa blokada po przekroczeniu limitu prób — ochrona przed brute-force).

* **ACCOUNTS:** Centralna tabela finansowa wykorzystująca typ `DECIMAL(15, 2)` dla absolutnej precyzji arytmetycznej. Wyróżnia się zastosowaniem **relacji rekurencyjnej** — klucz obcy `parent_account_id` pozwala na zagnieżdżanie subkont (np. kont dzieci) pod kontami głównymi rodziców. Tabela posiada również kolumnę `blocked_funds`, która odseparowuje środki dostępne od tych zamrożonych (np. przez nierozliczone autoryzacje kartowe lub blokady AML).

* **CARDS:** Powiązana z kontem relacją `ON DELETE CASCADE`. Definiuje wirtualne, fizyczne i prepaid nośniki płatnicze. Po integracji z zewnętrznym providerem kart tabela przechowuje **dane referencyjne** (`provider_token`, `provider_status`, `masked_pan`, `bin_prefix`), a **nigdy** pełnego numeru PAN ani CVV. Pełne dane karty są wyświetlane klientowi tylko jednorazowo, w momencie wydania (zgodnie z duchem PCI-DSS). Atrybuty `transaction_limit` i `daily_limit` umożliwiają egzekwowanie limitów po stronie banku przy każdym rozliczeniu.

#### 2. Silnik Rozliczeniowy

* **TRANSACTIONS:** Tabela zaprojektowana jako niezmienna **księga główna** (event log). Zastosowano tu kluczową zasadę audytu: klucze obce `sender_account_id` oraz `receiver_account_id` posiadają regułę `ON DELETE SET NULL`. Dzięki temu, nawet jeśli klient zamknie konto (a jego rekord zniknie z bazy), pełna historia jego przelewów pozostanie nienaruszona do celów kontroli skarbowej. Kolumna `external_payment_id` pozwala na integrację z systemami zewnętrznymi (NBP, SWIFT, provider kart). Rozszerzony enum `type` obsługuje wiele scenariuszy: standardowe przelewy (`INTERNAL`), płatności kartą (`CARD_PAYMENT`), audyt odrzuconych transakcji (`CARD_PAYMENT_REJECTED`), doładowania prepaid (`CARD_TOPUP`), płatności BLIK (`KLIK`).

#### 3. Moduł Nadzoru Autoryzacji (Junior)

* **PENDING_APPROVALS:** Dedykowana "poczekalnia" dla zleceń oczekujących. Wiąże ze sobą konto Juniora, identyfikator Rodzica oraz konkretną transakcję. Tabela obsługuje **maszynę stanów** z flagami `PENDING`, `APPROVED`, `REJECTED`, przechowując precyzyjne stemple czasowe (`resolved_at`) każdej decyzji podjętej przez opiekuna. Środki Juniora pozostają nienaruszone do momentu zatwierdzenia — to gwarantuje że bez zgody rodzica żadne pieniądze nie opuszczają konta dziecka.

#### 4. Ekosystem BLIK (KLIK)

* **KLIK_CODES:** Zarządza rygorystycznym cyklem życia kodów jednorazowych (6 cyfr). Poza statusami (`ACTIVE`, `USED`, `EXPIRED`) oraz stemplami czasowymi, posiada kluczową dla systemów rozproszonych kolumnę `idempotency_key`. Zabezpiecza ona przed podwójnym obciążeniem konta klienta w przypadku opóźnień sieciowych (tzw. retry attacks).

* **KLIK_ALIASES:** Rozwiązuje problem mapowania numeru telefonu klienta na jego `account_id`, umożliwiając realizację błyskawicznych przelewów P2P (Peer-to-Peer) w systemie Express Elixir.

* **KLIK_AUTHORIZATIONS:** Dedykowana tabela do obsługi **webhooków od dostawcy KLIK**. Przechowuje stan każdej żądanej autoryzacji płatności w cyklu `PENDING → CONFIRMED/REJECTED/EXPIRED`. Klient w aplikacji widzi listę oczekujących autoryzacji i jednym kliknięciem zatwierdza lub odrzuca każdą z nich. Pole `external_id` pozwala na **idempotentne** odbieranie wielokrotnych webhooków od dostawcy KLIK.

#### 5. System Anti-Money Laundering

* **AML_HOLDS:** Tabela prewencyjna dla transakcji i kont wysokiego ryzyka. Wiąże się bezpośrednio z podejrzaną transakcją lub całym kontem klienta. Umożliwia asynchroniczną komunikację na linii Bank-Klient poprzez kolumny `reason` (powód blokady nałożonej przez algorytm) oraz `client_explanation` (wyjaśnienia dostarczone z poziomu aplikacji klienckiej). Po przeglądnięciu wyjaśnień pracownik banku może zwolnić blokadę (`status: RELEASED`).

#### 6. Integracja z Providerem Kart Płatniczych

* **CARD_AUTHORIZATIONS:** Tabela kluczowa dla **asynchronicznej integracji** z zewnętrznym systemem kart płatniczych. Obsługuje pełen cykl rozliczenia transakcji kartowej: od pre-autoryzacji (`HELD`), przez settlement (`SETTLED`), aż po ewentualne zwroty (`REFUNDED`) lub wygasanie (`EXPIRED`). Kolumna `external_transaction_id` z ograniczeniem `UNIQUE` zapewnia **idempotencję webhooków** — wielokrotne wywołanie `/capture` z tym samym `transaction_id` przez providera (np. po niepowodzeniu sieciowym) zwraca tę samą decyzję bez podwójnego obciążenia konta. Pole `authorization_code` zapewnia unikalny identyfikator nadany przez bank, używany przez providera w późniejszych operacjach (settlement, refund).

---

**Migracje Flyway** zarządzają ewolucją schematu w sposób kontrolowany:
- `V1__init_schema.sql` — podstawowy schemat (USERS, ACCOUNTS, CARDS, TRANSACTIONS, PENDING_APPROVALS, KLIK_CODES, KLIK_ALIASES, AML_HOLDS)
- `V2__add_pin.sql` — kolumny obsługi kodu PIN w tabeli USERS
- `V3__junior_card_limits.sql` — dodanie `daily_limit` w CARDS oraz wsparcia limitów dziennych
- `V4__klik_authorizations.sql` — tabela KLIK_AUTHORIZATIONS (asynchroniczne webhooki BLIK)
- `V5__cards_provider_integration.sql` — kolumny providera w CARDS (`provider_token`, `provider_status`, `masked_pan`, `bin_prefix`) + tabela CARD_AUTHORIZATIONS


### 5.3 Diagram Klas (UML)

Sekcja prezentuje dwa diagramy klas o **różnym poziomie abstrakcji**, które razem opisują warstwę domenową aplikacji:

- **5.3.1 Model domeny** — odpowiednik diagramu klas dla analityka. Pokazuje **co** istnieje w systemie: encje biznesowe (klient, konto, karta, transakcja), ich atrybuty oraz wzajemne relacje. Stanowi mostek między ERD (sekcja 5.2), a kodem aplikacji.
- **5.3.2 Architektura warstwowa** — diagram dla programisty. Pokazuje **jak** komponenty współpracują w kodzie: które klasy są kontrolerami REST, które serwisami z logiką biznesową, które adapterami do systemów zewnętrznych. Zaprezentowana na przykładzie modułu kart (najbardziej rozbudowanego), ten sam schemat warstw stosowany jest w pozostałych modułach.

#### 5.3.1 Model domeny — encje główne

```mermaid
classDiagram
    class User {
        +UUID id
        +String customerNumber
        +String firstName
        +String lastName
        +String email
        +String passwordHash
        +String phoneNumber
        +LocalDate dateOfBirth
        +UserRole role
        +String pinHash
        +int pinFailedAttempts
        +LocalDateTime pinLockedUntil
    }

    class UserRole {
        <<enumeration>>
        ADMIN
        CUSTOMER
        JUNIOR
    }

    class Account {
        +UUID id
        +String accountNumber
        +BigDecimal balance
        +BigDecimal blockedFunds
        +String currency
        +String type
    }

    class Card {
        +UUID id
        +String cardNumber
        +String maskedPan
        +String providerToken
        +String providerStatus
        +String binPrefix
        +BigDecimal transactionLimit
        +BigDecimal dailyLimit
        +String currency
        +LocalDate expiryDate
        +String type
        +boolean isBlocked
    }

    class CardAuthorization {
        +UUID id
        +String authorizationCode
        +String externalTransactionId
        +BigDecimal amount
        +String currency
        +String merchantName
        +String status
        +LocalDateTime createdAt
        +LocalDateTime settledAt
    }

    class Transaction {
        +UUID id
        +String senderAccountNumber
        +String receiverAccountNumber
        +String receiverBankBic
        +String receiverName
        +String title
        +BigDecimal amount
        +String currency
        +String status
        +String type
        +String externalPaymentId
        +LocalDateTime createdAt
        +LocalDateTime executionDate
    }

    class PendingApproval {
        +UUID id
        +BigDecimal amount
        +String description
        +String status
        +LocalDateTime createdAt
        +LocalDateTime resolvedAt
    }

    class KlikCode {
        +UUID id
        +String code
        +BigDecimal amount
        +String status
        +LocalDateTime createdAt
        +LocalDateTime expiresAt
        +LocalDateTime usedAt
        +String idempotencyKey
    }

    class KlikAlias {
        +UUID id
        +String alias
        +boolean active
        +LocalDateTime createdAt
    }

    class KlikAuthorization {
        +UUID id
        +String externalId
        +BigDecimal amount
        +String currency
        +String merchant
        +String status
        +LocalDateTime createdAt
    }

    class AmlHold {
        +UUID id
        +String reason
        +String clientExplanation
        +String status
        +String createdBy
        +LocalDateTime createdAt
        +LocalDateTime releasedAt
    }

    User "1" -- "*" Account : posiada
    User --> UserRole
    Account "1" -- "0..*" Account : parentAccount (Junior)
    Account "1" -- "*" Card : ma karty
    Card "1" -- "*" CardAuthorization : ma autoryzacje
    Account "1" -- "*" CardAuthorization
    Account "1" -- "*" Transaction : sender/receiver
    Card "0..1" -- "*" Transaction : obciąża
    Account "1" -- "*" PendingApproval : Junior account
    User "1" -- "*" PendingApproval : parentUser
    Transaction "0..1" -- "0..1" PendingApproval
    User "1" -- "*" KlikCode
    Account "1" -- "*" KlikCode
    User "1" -- "*" KlikAlias
    Account "1" -- "*" KlikAlias
    User "1" -- "*" KlikAuthorization
    Account "1" -- "*" KlikAuthorization
    Account "1" -- "*" AmlHold
    Transaction "0..1" -- "0..1" AmlHold
```
Każdy obiekt domeny ma identyfikator typu UUID (zgodnie z konwencją z ERD), enkapsulując dane biznesowe związane z określoną odpowiedzialnością. Relacje 1-do-wielu (np. `User ←→ Account`) odzwierciedlają realny model bankowy: klient może mieć wiele kont, konto wiele kart, karta wiele autoryzacji. Relacja rekurencyjna `Account → Account (parentAccount)` modeluje hierarchię rodzic-Junior. Klasa `UserRole` jest enumeracją wyodrębnioną dla jasności typów.

#### 5.3.2 Architektura warstwowa (na przykładzie modułu kart)

Aplikacja stosuje klasyczny podział na warstwy: Controller → Service → Repository → Entity. Poniższy diagram pokazuje strukturę dla modułu kart, włącznie z integracją zewnętrzną.

```mermaid
classDiagram
    class CardController {
        <<RestController>>
        +listMyCards(Authentication) ResponseEntity
        +orderCard(OrderCardRequest) ResponseEntity
        +block(UUID) ResponseEntity
        +unblock(UUID) ResponseEntity
        +activate(UUID) ResponseEntity
        +topup(UUID, TopupCardRequest) ResponseEntity
        +deleteCard(UUID) ResponseEntity
        +devForceActivate(UUID) ResponseEntity
    }

    class CardOrderService {
        <<Service>>
        +orderForUser(email, type) OrderCardResponse
        +orderForJunior(juniorAccountId, parentEmail, type) OrderCardResponse
        +blockCard(cardId, email)
        +activateCard(cardId, email)
        +topupCard(cardId, email, amount) BigDecimal
        +deleteCard(cardId, email)
        +devForceActivate(cardId, email)
    }

    class CardPaymentService {
        <<Service>>
        +processPayment(CardPaymentRequest, email) PaymentResult
    }

    class CardsCallbackService {
        <<Service>>
        +authorize(AuthorizeWebhookRequest) AuthorizeWebhookResponse
        +capture(CaptureWebhookRequest) CaptureWebhookResponse
        +refund(RefundWebhookRequest) RefundWebhookResponse
        -validateCardLimits(card, amount, txId, merchant)
    }

    class CardsCallbackController {
        <<RestController>>
        +authorize() ResponseEntity
        +capture() ResponseEntity
        +refund() ResponseEntity
    }

    class CardTransactionAuditService {
        <<Service>>
        +saveRejectedCardPayment(card, amount, txId, merchant, reason)
    }

    class CardsProviderClient {
        <<Integration>>
        +issueCard(IssueCardRequest) IssueCardResponse
        +changeStatus(token, ChangeStatusRequest)
        +activateCard(token, ActivateCardRequest)
        +topupCard(token, TopupCardRequest) TopupCardResponse
        +updateLifecycle(token, LifecycleRequest)
        +getStatus(token) CardStatusResponse
    }

    class CardsProviderHmacSigner {
        <<Component>>
        +sign(secret, body) SignedRequest
    }

    class CardRepository {
        <<JpaRepository>>
        +findByProviderToken(token) Optional~Card~
    }

    class CardAuthorizationRepository {
        <<JpaRepository>>
        +findByAuthorizationCode(code) Optional
        +findByExternalTransactionId(id) Optional
    }

    class TransactionRepository {
        <<JpaRepository>>
    }

    class Card {
        <<Entity>>
    }

    class CardAuthorization {
        <<Entity>>
    }

    CardController --> CardOrderService
    CardController --> CardPaymentService
    CardController --> CardService
    CardsCallbackController --> CardsCallbackService
    CardOrderService --> CardsProviderClient
    CardOrderService --> CardRepository
    CardOrderService --> TransactionRepository
    CardsCallbackService --> CardRepository
    CardsCallbackService --> CardAuthorizationRepository
    CardsCallbackService --> TransactionRepository
    CardsCallbackService --> CardTransactionAuditService
    CardTransactionAuditService --> TransactionRepository
    CardsProviderClient --> CardsProviderHmacSigner
    CardRepository --> Card
    CardAuthorizationRepository --> CardAuthorization
```

Każde żądanie HTTP klienta przechodzi przez warstwy: **Controller** (walidacja, mapowanie DTO), **Service** (logika biznesowa, transakcje), **Repository** (dostęp do bazy), **Entity** (model danych). Integracja z zewnętrznym systemem kart jest enkapsulowana w warstwie `Integration` (`CardsProviderClient`), co realizuje wzorzec **Adapter**: zewnętrzny system jest abstrakcją, którą resztę aplikacji widzi jako lokalny serwis.

### 5.4 Diagramy Sekwencji

W odróżnieniu od diagramów klas (które pokazują **strukturę statyczną**), diagramy sekwencji pokazują **dynamiczne interakcje** w czasie. Każdy z poniższych przepływów obrazuje pełną podróż żądania od użytkownika do bazy danych — łącznie z systemami zewnętrznymi (provider kart). Wybrano cztery procesy najbardziej charakterystyczne dla aplikacji bankowej: dwa wymagające integracji zewnętrznej (5.4.1, 5.4.2) i dwa wewnętrzne ale z wieloma stanami (5.4.3, 5.4.4).

#### 5.4.1 Zamówienie karty u providera (HMAC-signed)

Pokazuje pełen flow zamówienia karty przez klienta, włącznie z podpisem HMAC i zapisem `provider_token` do lokalnej bazy banku.

```mermaid
sequenceDiagram
    actor Klient
    participant FE as Frontend (React)
    participant BE as Backend Bank (Spring)
    participant Signer as HmacSigner
    participant Provider as Cards Provider (FastAPI)
    participant DB as PostgreSQL

    Klient->>FE: Klika "Zamów kartę VIRTUAL"
    FE->>BE: POST /api/cards/order {cardType:VIRTUAL} + JWT
    BE->>DB: SELECT user, account
    DB-->>BE: User + Account
    BE->>BE: serializuj IssueCardRequest (snake_case)
    BE->>Signer: sign(hmacSecret, body)
    Signer-->>BE: SignedRequest {signature, timestamp, bodyJson}
    BE->>Provider: POST /api/v1/cards/issue
    Note over BE,Provider: Headers: X-API-Key, X-Signature, X-Timestamp
    Provider->>Provider: weryfikacja HMAC + timestamp (<30s)
    Provider->>Provider: generowanie PAN, CVV, token
    Provider-->>BE: 200 {card_token, full_pan, cvv, masked_pan, expiry_*}
    BE->>DB: INSERT INTO cards (provider_token, masked_pan, ...)
    DB-->>BE: Card ID
    BE-->>FE: 200 OrderCardResponse {fullPan, cvv, ...}
    FE-->>Klient: Modal "Karta wydana" (PAN/CVV jednorazowo)
```
Diagram pokazuje wydanie karty z perspektywy całego systemu. Kluczowa jest **walidacja autentyczności żądania** po stronie providera kart, oparta na podpisie HMAC-SHA256. Bank generuje podpis ze swojego sekretu HMAC, providera weryfikuje go używając tego samego klucza pobranego z bazy. Trzy nagłówki (`X-API-Key`, `X-Signature`, `X-Timestamp`) zapewniają: identyfikację banku, integralność body i ochronę przed atakami replay (timestamp musi być nie starszy niż 30 sekund). Po pomyślnym zwróceniu danych karty bank **trwale zapisuje** `provider_token` i `masked_pan`, ale pełen PAN i CVV przekazuje klientowi tylko **jednorazowo** w odpowiedzi — nigdy nie są zapisywane lokalnie (zgodnie z duchem PCI-DSS).

#### 5.4.2 Płatność kartą — od POS do settlement w banku

Najważniejszy flow biznesowy. Pokazuje rozróżnienie między autoryzacją (online u providera) a settlement (asynchroniczny webhook do banku z walidacją limitów).

```mermaid
sequenceDiagram
    actor Klient
    participant POS as POS Terminal
    participant Provider as Cards Provider
    participant ISO as Card Provider (ISO 8583)
    participant Settle as Settlement Batch
    participant Forward as ForwardFilter
    participant BE as Backend Bank
    participant DB as PostgreSQL

    Klient->>POS: Wpisuje PAN, CVV, expiry, kwotę
    POS->>Provider: POST /api/v1/payments/authorize
    Provider->>ISO: Forward ISO 8583
    ISO->>ISO: Walidacja (Luhn, status karty, expiry, CVV)
    ISO-->>Provider: APPROVED
    Provider-->>POS: APPROVED
    POS-->>Klient: "APPROVED" na ekranie

    Note over Provider,Settle: ~30s później — settlement batch
    Settle->>BE: POST /capture {transaction_id, card_token, amount, ...}
    BE->>Forward: dispatcher /capture
    Forward->>BE: forward → /api/webhooks/cards/capture
    BE->>DB: SELECT card BY provider_token
    DB-->>BE: Card

    alt Limit OK
        BE->>BE: validateCardLimits(card, amount)
        BE->>DB: UPDATE account SET balance -= amount
        BE->>DB: INSERT transaction (status=COMPLETED, type=CARD_PAYMENT)
        BE-->>Settle: 200 {status: SETTLED}
    else Limit przekroczony
        BE->>BE: validateCardLimits → IllegalStateException
        BE->>DB: INSERT transaction (status=REJECTED) [REQUIRES_NEW]
        Note over BE,DB: Audit trail — osobna transakcja, nie rollbackowana
        BE-->>Settle: 400 (Limit exceeded)
        Note over Settle: Provider widzi FAIL, transakcja nie księgowana
    end
```
Najważniejszy diagram dla zrozumienia rozdziału **autoryzacji** od **rozliczenia**. Provider kart sprawdza autoryzację natychmiast, lokalnie, w komunikacie ISO 8583 (status karty, CVV, expiry) — POS dostaje APPROVED w sekundach. Bank dowiaduje się o transakcji dopiero po stronie settlementu, ok. 30s później, gdy batch wywołuje webhook `/capture`. W tym momencie bank dokonuje **dodatkowej walidacji**: limitów ustalonych przez klienta (transakcji + dziennego) i statusu blokady. Jeśli walidacja się uda — saldo jest obciążone, transakcja zapisana jako `COMPLETED`. Jeśli przekroczono limit — bank zwraca 400, transakcja **nie jest księgowana**, ale w historii klienta zapisywany jest **wpis audytowy** ze statusem `REJECTED` (w osobnej transakcji bazodanowej dzięki `REQUIRES_NEW`, niezależnie od rollbacku głównej operacji). Klient widzi w aplikacji informację że bank odrzucił płatność z konkretnym powodem.

#### 5.4.3 Przelew z konta Junior — wymagana akceptacja rodzica

Pokazuje mechanizm dwustopniowej autoryzacji: dziecko inicjuje przelew, rodzic akceptuje (lub odrzuca).

```mermaid
sequenceDiagram
    actor Junior
    actor Rodzic
    participant FE as Frontend
    participant BE as Backend Bank
    participant DB as PostgreSQL

    Junior->>FE: Wypełnia formularz przelewu
    FE->>BE: POST /api/transactions/internal + JWT
    BE->>DB: SELECT junior account
    DB-->>BE: Account (type=JUNIOR, parentAccount)

    BE->>BE: Walidacja PIN, salda, limitu
    BE->>DB: INSERT transaction (status=PENDING_APPROVAL)
    BE->>DB: INSERT pending_approval (parentUserId, transactionId)
    Note over BE,DB: Środki niezablokowane — przelew "w poczekalni"
    BE-->>FE: 200 {status: PENDING_APPROVAL}
    FE-->>Junior: "Czeka na zatwierdzenie rodzica"

    Note over Rodzic,FE: Później — rodzic loguje się
    Rodzic->>FE: Wchodzi w "Oczekujące zatwierdzenia"
    FE->>BE: GET /api/junior/pending-approvals + JWT
    BE->>DB: SELECT pending WHERE parentUserId
    DB-->>BE: List PendingApproval
    BE-->>FE: 200 List
    FE-->>Rodzic: Lista oczekujących

    alt Rodzic akceptuje
        Rodzic->>FE: Klika "Zatwierdź"
        FE->>BE: POST /api/junior/pending-approvals/{id}/approve
        BE->>DB: UPDATE junior balance -= amount
        BE->>DB: UPDATE receiver balance += amount
        BE->>DB: UPDATE transaction SET status=COMPLETED
        BE->>DB: UPDATE pending_approval SET status=APPROVED, resolved_at=NOW
        BE-->>FE: 200 "Zatwierdzono"
    else Rodzic odrzuca
        Rodzic->>FE: Klika "Odrzuć"
        FE->>BE: POST /api/junior/pending-approvals/{id}/reject
        BE->>DB: UPDATE transaction SET status=REJECTED
        BE->>DB: UPDATE pending_approval SET status=REJECTED, resolved_at=NOW
        Note over BE,DB: Saldo nigdy nie zostało dotknięte
        BE-->>FE: 200 "Odrzucono"
    end
```
Mechanizm **dwustopniowej autoryzacji** dla nieletnich klientów. Junior może w aplikacji wypełnić formularz przelewu wewnętrznego, ale system **nie wykonuje go od razu** — zamiast tego umieszcza transakcję w "poczekalni" (`PENDING_APPROVAL`) i tworzy odpowiadający rekord `PendingApproval` widoczny dla rodzica. Środki **nie są blokowane** na koncie Juniora — saldo zostaje nienaruszone do momentu decyzji rodzica. Rodzic w swojej aplikacji widzi listę oczekujących transakcji dzieci i może je zatwierdzić lub odrzucić jednym kliknięciem. Dopiero w momencie zatwierdzenia odbywa się rzeczywisty transfer środków. To realizacja wymagania z PDF: *"wszystkie transakcje z konta Junior muszą być zatwierdzone przez rodzica"*.

#### 5.4.4 Doładowanie karty PREPAID Juniora

Najbardziej złożony flow w module Junior — łączy provider, własne księgowanie i utworzenie transakcji w historii.

```mermaid
sequenceDiagram
    actor Rodzic
    participant FE as Frontend (ManageJunior)
    participant BE as Backend Bank
    participant Provider as Cards Provider
    participant DB as PostgreSQL

    Rodzic->>FE: Klika "Doładuj" → wpisuje 100 PLN
    FE->>BE: POST /api/cards/{cardId}/topup {amount: 100}
    BE->>DB: SELECT card, junior account, parent account
    DB-->>BE: Card (type=PREPAID), Account (JUNIOR), Account (parent)

    BE->>BE: Walidacja (PREPAID? saldo rodzica?)
    BE->>Provider: POST /api/v1/cards/{token}/topup {amount: 100}
    Provider->>Provider: UPDATE card balance += 100
    Provider-->>BE: 200 {new_balance: 100}

    BE->>DB: UPDATE parent_account balance -= 100
    BE->>DB: UPDATE junior_account balance += 100
    BE->>DB: INSERT transaction (type=CARD_TOPUP, COMPLETED)
    Note over BE,DB: Sender = rodzic, Receiver = Junior
    BE-->>FE: 200 {newCardBalance: 100}
    FE-->>Rodzic: "Karta doładowana"

    Note over Rodzic,DB: W historii widać po obu stronach:
    Note over Rodzic,DB: Rodzic: -100 PLN "Doładowanie karty PREPAID (•••• 5091)"
    Note over Rodzic,DB: Junior: +100 PLN "Doładowanie karty PREPAID ← imię rodzica"
```
Doładowanie karty prepaid pokazuje **koordynację dwóch systemów** (bank + provider) podczas jednej operacji biznesowej. Środki muszą być spójnie zaktualizowane po obu stronach: provider podnosi balance karty (żeby POS akceptował przyszłe płatności do tej kwoty), a bank wykonuje wewnętrzny przelew między kontami rodzica i Juniora (żeby przy kolejnym webhook settlementu konto Juniora miało skąd zostać obciążone). Operacja generuje **jedną** transakcję bankową typu `CARD_TOPUP` widoczną z perspektywy obu kont — rodzic widzi jej w historii jako wypływ (`-100 PLN`), Junior jako wpływ (`+100 PLN`). Maskowany numer karty w tytule pomaga klientowi szybko zidentyfikować której karty dotyczy doładowanie.

## 6. Architektura
> **TODO:** wrzucenie pełnej architektury

## 7. Struktura projektu
> **TODO:** wrzucenie struktury projektu

## 8. Uruchomienie
Projekt został w pełni skonteneryzowany, co gwarantuje spójność środowiska uruchomieniowego. Do uruchomienia całej infrastruktury (Baza danych, Backend, Frontend) wymagany jest jedynie Docker oraz Docker Compose.

### Wymagania wstępne
* Zainstalowany [Docker Desktop](https://www.docker.com/products/docker-desktop/) (lub sam Docker Engine + Compose w systemach Linux)
* Zainstalowany system kontroli wersji `git`

### Krok po kroku

1. **Pobranie repozytorium**
   Otwórz terminal i sklonuj projekt na swój dysk:
   ```bash
   git clone <https://github.com/DominikDz3/polish-bank-a>
   cd polish-bank-a

2. **Uruchomienie kontenerów**
    W głównym folderze projektu (tam, gdzie znajduje się plik docker-compose.yml) wykonaj polecenie:

    ```
    docker compose up --build
    ```
    (Flaga --build wymusza świeżą kompilację kodu Javy i Reacta przed startem aplikacji).

3. **Dostęp do aplikacji**
    Po pojawieniu się w logach informacji o poprawnym uruchomieniu, usługi będą dostępne pod adresami:

    * Frontend (Aplikacja Webowa): http://localhost:5137
    * Backend (API Spring Boot): http://localhost:8080
    * Baza danych (PostgreSQL): localhost:5432

4. **Zatrzymanie aplikacji**
    Jeśli chcesz zatrzymać aplikację, użyj skrótu Ctrl + C w terminalu lub wpisz:

    ```
    docker compose down
    ```

    Jeśli potrzebujesz całkowicie zresetować bazę danych (np. przywrócić pierwotne dane testowe z Seedera), użyj komendy czyszczącej wolumeny:
    ```
    docker compose down -v
    docker compose up --build
    ```


## 9. Zespół
Osoby pracujące w zespole pracują w modelu fullstack (tworzą elementy widoków użytkownika, elementy związane z logiką bazodanową lub API)

Zadania wykonywane do tej pory

| Osoba            | Zadania                                                                                        |
|------------------|------------------------------------------------------------------------------------------------|
| Julia Chmura     | Stworzenie bazy danych, zaprojektowanie diagramów UML, utworzenie encji w backendzie           |
| Dominik Dziadosz | Tworzenie ogólnego zarysu widoków, tworzenie widoku strony głównej, logowania oraz rejestracji |

















