package com.polishbank.bank_a.seeder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.domain.user.UserRole;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.AmlHold;
import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.entity.KlikAlias;
import com.polishbank.bank_a.entity.KlikCode;
import com.polishbank.bank_a.entity.PendingApproval;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.AmlHoldRepository;
import com.polishbank.bank_a.repository.CardRepository;
import com.polishbank.bank_a.repository.KlikAliasRepository;
import com.polishbank.bank_a.repository.KlikCodeRepository;
import com.polishbank.bank_a.repository.PendingApprovalRepository;
import com.polishbank.bank_a.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final PendingApprovalRepository pendingApprovalRepository;
    private final KlikCodeRepository klikCodeRepository;
    private final KlikAliasRepository klikAliasRepository;
    private final AmlHoldRepository amlHoldRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            System.out.println("Baza danych zawiera już dane. Pomijam seedowanie.");
            return;
        }

        System.out.println("Rozpoczynam seedowanie danych testowych z POTĘŻNĄ ilością transakcji i BLIKów...");

        String defaultPassword = passwordEncoder.encode("password");

        String defaultPinHash = passwordEncoder.encode("1234");

        // 1. UŻYTKOWNICY
        User admin = userRepository.save(User.builder().customerNumber("99000001").firstName("Jan").lastName("Kowalski")
                .email("admin@banka.pl").phoneNumber("48111222333").dateOfBirth(LocalDate.of(1980, 1, 1))
                .passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.ADMIN).build());
        User parentEwa = userRepository.save(User.builder().customerNumber("84920183").firstName("Ewa")
                .lastName("Majewska").email("ewa.majewska@gmail.com").phoneNumber("48600100200")
                .dateOfBirth(LocalDate.of(1985, 5, 15)).passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.CUSTOMER).build());
        User juniorJanek = userRepository.save(User.builder().customerNumber("12847593").firstName("Jan")
                .lastName("Majewski").email("janek.m@gmail.com").phoneNumber("48600100201")
                .dateOfBirth(LocalDate.of(2015, 8, 20)).passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.CUSTOMER).build());
        User juniorZuzia = userRepository.save(User.builder().customerNumber("58392011").firstName("Zuzanna")
                .lastName("Majewska").email("zuzia.m@gmail.com").phoneNumber("48600100202")
                .dateOfBirth(LocalDate.of(2013, 11, 10)).passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.CUSTOMER).build());
        User parentAnna = userRepository.save(User.builder().customerNumber("67291028").firstName("Anna")
                .lastName("Nowak").email("anna.nowak@gmail.com").phoneNumber("48700300400")
                .dateOfBirth(LocalDate.of(1990, 7, 22)).passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.CUSTOMER).build());
        User juniorPiotr = userRepository.save(User.builder().customerNumber("48192038").firstName("Piotr")
                .lastName("Nowak").email("piotrek.junior@gmail.com").phoneNumber("48700300401")
                .dateOfBirth(LocalDate.of(2014, 4, 12)).passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.CUSTOMER).build());
        User michal = userRepository.save(User.builder().customerNumber("51829304").firstName("Michał")
                .lastName("Wiśniewski").email("michal.w@firma.pl").phoneNumber("48500600700")
                .dateOfBirth(LocalDate.of(1992, 9, 9)).passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.CUSTOMER).build());
        User katarzyna = userRepository.save(User.builder().customerNumber("39481726").firstName("Katarzyna")
                .lastName("Zielińska").email("kasia.z@korpo.com").phoneNumber("48800900100")
                .dateOfBirth(LocalDate.of(1988, 12, 1)).passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.CUSTOMER).build());
        User podejrzany = userRepository.save(User.builder().customerNumber("11847265").firstName("Olaf")
                .lastName("Kombinator").email("olaf.k@darknet.pl").phoneNumber("48999000999")
                .dateOfBirth(LocalDate.of(1975, 3, 15)).passwordHash(defaultPassword).pinHash(defaultPinHash).role(UserRole.CUSTOMER).build());

                
        // 2. KONTA BANKOWE (Identyfikator banku: 88880000)
        Account accEwa = accountRepository.save(Account.builder().user(parentEwa)
                .accountNumber("PL45888800001234567890123456").balance(new BigDecimal("450000.00"))
                .blockedFunds(new BigDecimal("150.00")).currency("PLN").type("STANDARD").build());
        Account accJanek = accountRepository.save(Account.builder().user(juniorJanek)
                .accountNumber("PL92888800009876543210987654").balance(new BigDecimal("120.00"))
                .blockedFunds(BigDecimal.ZERO).currency("PLN").type("JUNIOR").parentAccount(accEwa).build());
        Account accZuzia = accountRepository.save(Account.builder().user(juniorZuzia)
                .accountNumber("PL11888800005555666677778888").balance(new BigDecimal("350.50"))
                .blockedFunds(BigDecimal.ZERO).currency("PLN").type("JUNIOR").parentAccount(accEwa).build());
        Account accAnna = accountRepository.save(Account.builder().user(parentAnna)
                .accountNumber("PL33888800001122334455667788").balance(new BigDecimal("15000.00"))
                .blockedFunds(BigDecimal.ZERO).currency("PLN").type("STANDARD").build());
        Account accPiotr = accountRepository.save(Account.builder().user(juniorPiotr)
                .accountNumber("PL77888800009999000011112222").balance(new BigDecimal("150.50"))
                .blockedFunds(BigDecimal.ZERO).currency("PLN").type("JUNIOR").parentAccount(accAnna).build());
        Account accMichal = accountRepository.save(Account.builder().user(michal)
                .accountNumber("PL22888800004444888812345678").balance(new BigDecimal("8000.00"))
                .blockedFunds(BigDecimal.ZERO).currency("PLN").type("STANDARD").build());
        Account accKatarzyna = accountRepository.save(Account.builder().user(katarzyna)
                .accountNumber("PL66888800007777333319283746").balance(new BigDecimal("1500000.00"))
                .blockedFunds(new BigDecimal("500.00")).currency("PLN").type("STANDARD").build());
        Account accPodejrzany = accountRepository.save(Account.builder().user(podejrzany)
                .accountNumber("PL55888800006666222298761234").balance(new BigDecimal("0.00"))
                .blockedFunds(BigDecimal.ZERO).currency("PLN").type("STANDARD").build());

        /*        
        // 3. KARTY PŁATNICZE
        LocalDate cardExpiry = LocalDate.now().plusYears(3);
        cardRepository.saveAll(List.of(
                Card.builder().account(accEwa).cardNumber("4539817263549012")
                        .transactionLimit(new BigDecimal("10000.00")).currency("PLN").expiryDate(cardExpiry)
                        .type("DEBIT").isBlocked(false).build(),
                Card.builder().account(accJanek).cardNumber("4222192837465566")
                        .transactionLimit(new BigDecimal("100.00")).dailyLimit(new BigDecimal("300.00"))
                        .currency("PLN").expiryDate(cardExpiry).type("PREPAID").isBlocked(false).build(),
                Card.builder().account(accZuzia).cardNumber("4987654321098765")
                        .transactionLimit(new BigDecimal("150.00")).dailyLimit(new BigDecimal("500.00"))
                        .currency("PLN").expiryDate(cardExpiry).type("PREPAID").isBlocked(false).build(),
                Card.builder().account(accAnna).cardNumber("5312998877665544")
                        .transactionLimit(new BigDecimal("5000.00")).currency("PLN").expiryDate(cardExpiry)
                        .type("DEBIT").isBlocked(false).build(),
                Card.builder().account(accMichal).cardNumber("5411223344556677")
                        .transactionLimit(new BigDecimal("15000.00")).currency("PLN").expiryDate(cardExpiry)
                        .type("CREDIT").isBlocked(false).build()));
        */            

                        
        // 4. TRANSAKCJE
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> transactions = new ArrayList<>();

        // INTERNAL
        transactions.add(createTx(accEwa, accJanek, "Jan Majewski", "Kieszonkowe", "50.00", "INTERNAL", "COMPLETED",
                now.minusDays(10)));
        transactions.add(createTx(accEwa, accZuzia, "Zuzanna Majewska", "Na kino", "30.00", "INTERNAL", "COMPLETED",
                now.minusDays(8)));
        transactions.add(createTx(accJanek, accPiotr, "Piotr Nowak", "Oddaję za grę planszową", "20.00", "INTERNAL",
                "COMPLETED", now.minusDays(4)));
        transactions.add(createTx(accMichal, accEwa, "Ewa Majewska", "Zwrot za paliwo", "120.00", "INTERNAL",
                "COMPLETED", now.minusDays(2)));
        transactions.add(createTx(accEwa, accKatarzyna, "Katarzyna Zielińska", "Rozliczenie wyjazdu", "450.00",
                "INTERNAL", "COMPLETED", now.minusDays(1)));
        transactions.add(createTx(accAnna, accPiotr, "Piotr Nowak", "Na wycieczkę", "100.00", "INTERNAL", "COMPLETED",
                now.minusHours(5)));

        // ELIXIR
        transactions.add(createExternalTx(null, accEwa.getAccountNumber(), "Ewa Majewska", "Wynagrodzenie za Luty 2026",
                "8500.00", "PLN", "ELIXIR", "COMPLETED", "WYN-2026-02", now.minusDays(5)));
        transactions.add(
                createExternalTx(null, accMichal.getAccountNumber(), "Michał Wiśniewski", "Wynagrodzenie za Luty 2026",
                        "6200.00", "PLN", "ELIXIR", "COMPLETED", "WYN-2026-02", now.minusDays(5)));
        transactions.add(createExternalTx(accMichal, "PL12102010100000000012345678", "Spółdzielnia Mieszkaniowa",
                "Czynsz za Marzec", "850.00", "PLN", "ELIXIR", "COMPLETED", "EXT-EL-001", now.minusDays(3)));
        transactions.add(createExternalTx(accEwa, "PL88105010100000000087654321", "PGE Dystrybucja",
                "Faktura FV/03/2026", "230.50", "PLN", "ELIXIR", "COMPLETED", "EXT-EL-002", now.minusDays(2)));
        transactions.add(createExternalTx(accAnna, "PL55114020040000000011119999", "ZUS", "Składka zdrowotna 02/2026",
                "381.81", "PLN", "ELIXIR", "COMPLETED", "EXT-EL-003", now.minusDays(1)));
        transactions.add(createExternalTx(accKatarzyna, "PL99105014451000002200001111", "Urząd Skarbowy",
                "VAT-7 za 02/2026", "4500.00", "PLN", "ELIXIR", "PENDING", "EXT-EL-004", null));

        // EXPRESS_ELIXIR
        transactions.add(createExternalTx(accAnna, "PL45114020040000300201112222", "Michał Wiśniewski",
                "Zrzutka na prezent natychmiast", "150.00", "PLN", "EXPRESS_ELIXIR", "COMPLETED", "EXT-EXP-001",
                now.minusHours(12)));
        transactions.add(
                createExternalTx(accKatarzyna, "PL11124010531111000088889999", "Dom Maklerski", "Zasilenie konta IKE",
                        "15000.00", "PLN", "EXPRESS_ELIXIR", "COMPLETED", "EXT-EXP-002", now.minusHours(4)));

        // SORBNET
        transactions.add(createExternalTx(accKatarzyna, "PL99101010100000000011111111",
                "Kancelaria Notarialna Sp. z o.o.", "Zakup apartamentu - akt nr 123/2026", "1250000.00", "PLN",
                "SORBNET", "COMPLETED", "SRB-2026-991", now.minusDays(1)));

        // SWIFT
        Transaction swiftTx = createExternalTx(accMichal, "DE1234567890123456789012", "BMW München", "Zaliczka na auto",
                "5000.00", "EUR", "SWIFT", "PENDING", "SWIFT-XYZ-112", null);
        swiftTx.setReceiverBankBic("DEUTDEFF");
        transactions.add(swiftTx);

        // BLIK / KLIK
        transactions.add(createExternalTx(accAnna, "48500600700", "Michał Wiśniewski", "Oddaję za pizzę", "45.00",
                "PLN", "BLIK", "COMPLETED", "BLIK-P2P-001", now.minusHours(24)));
        transactions.add(createExternalTx(accEwa, "BLIK-MERCHANT-888", "Biedronka", "Zakupy spożywcze", "125.40", "PLN",
                "BLIK", "COMPLETED", "BLIK-POS-002", now.minusHours(5)));
        transactions.add(createExternalTx(accMichal, "BLIK-ATM-999", "Wypłata z bankomatu Euronet", "Wypłata gotówki",
                "200.00", "PLN", "BLIK", "COMPLETED", "BLIK-ATM-001", now.minusHours(2)));
        transactions.add(createExternalTx(accJanek, "BLIK-ONLINE-111", "Allegro Pay", "Zakup klocków LEGO", "89.99",
                "PLN", "BLIK", "COMPLETED", "BLIK-ONL-001", now.minusHours(1)));
        transactions.add(createExternalTx(accKatarzyna, "48600100200", "Ewa Majewska", "Płatność na telefon", "250.00",
                "PLN", "BLIK", "COMPLETED", "BLIK-P2P-002", now.minusMinutes(30)));

        // AML
        Transaction amlTx1 = createExternalTx(accPodejrzany, "CY1122334455667788990011", "DarkNet Offshore LLC",
                "Opłata manipulacyjna", "250000.00", "EUR", "SWIFT", "HELD_FOR_AML", "SWIFT-SUS-01", null);
        amlTx1.setReceiverBankBic("CYPRUS12");
        transactions.add(amlTx1);

        Transaction amlTx2 = createExternalTx(accPodejrzany, "RU9988776655443322110033", "Sancjonowana Firma",
                "Dostawa sprzętu", "50000.00", "USD", "SWIFT", "REJECTED_AML", "SWIFT-SUS-02", now);
        amlTx2.setReceiverBankBic("SBERRUMM");
        transactions.add(amlTx2);

        Transaction amlTx3 = createExternalTx(accMichal, "KY9911223344556677889900", "Kajmany Investment Fund",
                "Inwestycja ryzykowna", "80000.00", "USD", "SWIFT", "HELD_FOR_AML", "SWIFT-SUS-03", null);
        amlTx3.setReceiverBankBic("CAYMAN88");
        transactions.add(amlTx3);

        // PENDING (Junior)
        Transaction pendingTx1 = createExternalTx(accJanek, "PL44124010531111000088889999", "Media Expert",
                "PlayStation 5", "2500.00", "PLN", "EXPRESS_ELIXIR", "PENDING_APPROVAL", null, null);
        transactions.add(pendingTx1);
        Transaction pendingTx2 = createExternalTx(accZuzia, "PL11105014451000002255556666", "Roblox Corporation",
                "Zakup 2000 Robux", "150.00", "PLN", "BLIK", "PENDING_APPROVAL", null, null);
        transactions.add(pendingTx2);
        Transaction pendingTx3 = createExternalTx(accPiotr, "PL991140200400003333444455", "Sklep Rowerowy",
                "Nowy Rower BMX", "1200.00", "PLN", "ELIXIR", "PENDING_APPROVAL", null, null);
        transactions.add(pendingTx3);

        List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);

        
        // 5. SYSTEM AML
        amlHoldRepository.saveAll(List.of(
                AmlHold.builder().account(accPodejrzany).transaction(savedTransactions.get(20))
                        .reason("Nietypowy, bardzo duży przelew do kraju wysokiego ryzyka podatkowego (Cypr).")
                        .clientExplanation("To zapłata za legalne doradztwo biznesowe IT.")
                        .status("ACTIVE").createdBy("System_AML_Automatyczny").build(),
                AmlHold.builder().account(accPodejrzany).transaction(savedTransactions.get(21))
                        .reason("Blokada Krytyczna: Odbiorca znajduje się na liście podmiotów objętych międzynarodowymi sankcjami.")
                        .status("ACTIVE").createdBy("System_AML_Automatyczny").build(),
                AmlHold.builder().account(accMichal).transaction(savedTransactions.get(22))
                        .reason("Duży transfer środków do rajów podatkowych (Kajmany). Niezgodność z profilem klienta.")
                        .status("ACTIVE").createdBy("System_AML_Automatyczny").build()));


        // 6. PENDING APPROVALS
        pendingApprovalRepository.saveAll(List.of(
                PendingApproval.builder().juniorAccount(accJanek).parentUser(parentEwa)
                        .transaction(savedTransactions.get(23))
                        .amount(new BigDecimal("2500.00")).description("PlayStation 5").status("PENDING").build(),
                PendingApproval.builder().juniorAccount(accZuzia).parentUser(parentEwa)
                        .transaction(savedTransactions.get(24))
                        .amount(new BigDecimal("150.00")).description("Zakup 2000 Robux").status("PENDING").build(),
                PendingApproval.builder().juniorAccount(accPiotr).parentUser(parentAnna)
                        .transaction(savedTransactions.get(25))
                        .amount(new BigDecimal("1200.00")).description("Nowy Rower BMX").status("PENDING").build()));


        // 7. SYSTEM BLIK
        klikAliasRepository.saveAll(List.of(
                KlikAlias.builder().user(parentEwa).account(accEwa).alias(parentEwa.getPhoneNumber()).active(true)
                        .build(),
                KlikAlias.builder().user(michal).account(accMichal).alias(michal.getPhoneNumber()).active(true).build(),
                KlikAlias.builder().user(katarzyna).account(accKatarzyna).alias(katarzyna.getPhoneNumber()).active(true)
                        .build(),
                KlikAlias.builder().user(parentAnna).account(accAnna).alias(parentAnna.getPhoneNumber()).active(true)
                        .build()));

        klikCodeRepository.saveAll(List.of(
                KlikCode.builder().user(parentAnna).account(accAnna).code("123456").status("ACTIVE")
                        .expiresAt(now.plusMinutes(2)).build(),
                KlikCode.builder().user(michal).account(accMichal).code("881122").status("ACTIVE")
                        .expiresAt(now.plusMinutes(1)).build(),
                KlikCode.builder().user(juniorJanek).account(accJanek).code("556677").status("ACTIVE")
                        .expiresAt(now.plusSeconds(45)).build(),
                KlikCode.builder().user(katarzyna).account(accKatarzyna).code("990011").status("ACTIVE")
                        .expiresAt(now.plusMinutes(2)).build(),
                KlikCode.builder().user(parentEwa).account(accEwa).code("654321").amount(new BigDecimal("125.40"))
                        .status("USED").expiresAt(now.minusHours(4)).usedAt(now.minusHours(5)).build(),
                KlikCode.builder().user(michal).account(accMichal).code("111222").amount(new BigDecimal("200.00"))
                        .status("USED").expiresAt(now.minusHours(1)).usedAt(now.minusHours(2)).build(),
                KlikCode.builder().user(juniorJanek).account(accJanek).code("444555").amount(new BigDecimal("89.99"))
                        .status("USED").expiresAt(now.minusMinutes(30)).usedAt(now.minusHours(1)).build(),
                KlikCode.builder().user(juniorJanek).account(accJanek).code("000000").status("EXPIRED")
                        .expiresAt(now.minusDays(1)).build(),
                KlikCode.builder().user(parentAnna).account(accAnna).code("135790").status("EXPIRED")
                        .expiresAt(now.minusHours(2)).build(),
                KlikCode.builder().user(podejrzany).account(accPodejrzany).code("666999").status("EXPIRED")
                        .expiresAt(now.minusDays(3)).build()));

        System.out.println("ZAKOŃCZONO: Zbiór danych został zainicjowny w bazie.");
    }

    
    private Transaction createTx(Account sender, Account receiver, String recName, String title, String amount, String type, String status, LocalDateTime execDate) {
        return Transaction.builder()
                .senderAccount(sender)
                .senderAccountNumber(sender != null ? sender.getAccountNumber() : "SYSTEM")
                .receiverAccount(receiver)
                .receiverAccountNumber(receiver != null ? receiver.getAccountNumber() : "SYSTEM")
                .receiverName(recName).title(title)
                .amount(new BigDecimal(amount)).currency("PLN").status(status).type(type)
                .executionDate(execDate).build();
    }

    private Transaction createExternalTx(Account sender, String recAccNum, String recName, String title, String amount, String currency, String type, String status, String extId, LocalDateTime execDate) {
        return Transaction.builder()
                .senderAccount(sender)
                .senderAccountNumber(sender != null ? sender.getAccountNumber() : "SYSTEM_ZEWNETRZNY")
                .receiverAccountNumber(recAccNum)
                .receiverName(recName).title(title)
                .amount(new BigDecimal(amount)).currency(currency).status(status).type(type)
                .externalPaymentId(extId)
                .executionDate(execDate).build();
    }
}