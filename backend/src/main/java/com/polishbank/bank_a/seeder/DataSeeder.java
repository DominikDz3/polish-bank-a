package com.polishbank.bank_a.seeder;

import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.entity.User;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.CardRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import com.polishbank.bank_a.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            System.out.println("Baza danych zawiera już dane. Pomijam seedowanie.");
            return;
        }

        System.out.println("Rozpoczynam masowe seedowanie danych testowych...");

        String defaultPassword = passwordEncoder.encode("password");

        // ==========================================
        // 1. UŻYTKOWNICY
        // ==========================================
        User admin = userRepository.save(User.builder().firstName("Jan").lastName("Kowalski").email("admin@banka.pl").phoneNumber("48111222333").passwordHash(defaultPassword).role("BANK_EMPLOYEE").build());
        User parentEwa = userRepository.save(User.builder().firstName("Ewa").lastName("Majewska").email("ewa.majewska@gmail.com").phoneNumber("48123123123").passwordHash(defaultPassword).role("CUSTOMER").build());
        User juniorJanek = userRepository.save(User.builder().firstName("Jan").lastName("Majewski").email("janek.m@gmail.com").phoneNumber("48100200300").passwordHash(defaultPassword).role("CUSTOMER").build());
        User juniorZuzia = userRepository.save(User.builder().firstName("Zuzanna").lastName("Majewska").email("zuzia.m@gmail.com").phoneNumber("48100200400").passwordHash(defaultPassword).role("CUSTOMER").build());
        User juniorAntek = userRepository.save(User.builder().firstName("Antoni").lastName("Majewski").email("antek.m@gmail.com").phoneNumber("48100200500").passwordHash(defaultPassword).role("CUSTOMER").build());
        User parentAnna = userRepository.save(User.builder().firstName("Anna").lastName("Nowak").email("anna.nowak@gmail.com").phoneNumber("48999888777").passwordHash(defaultPassword).role("CUSTOMER").build());
        User juniorPiotr = userRepository.save(User.builder().firstName("Piotr").lastName("Nowak").email("piotrek.junior@gmail.com").phoneNumber("48555444333").passwordHash(defaultPassword).role("CUSTOMER").build());
        User michal = userRepository.save(User.builder().firstName("Michał").lastName("Wiśniewski").email("michal.w@firma.pl").phoneNumber("48777666555").passwordHash(defaultPassword).role("CUSTOMER").build());
        User katarzyna = userRepository.save(User.builder().firstName("Katarzyna").lastName("Zielińska").email("kasia.z@korpo.com").phoneNumber("48333222111").passwordHash(defaultPassword).role("CUSTOMER").build());
        User podejrzany = userRepository.save(User.builder().firstName("Olaf").lastName("Kombinator").email("olaf.k@darknet.pl").phoneNumber("48999000999").passwordHash(defaultPassword).role("CUSTOMER").build());

        // ==========================================
        // 2. KONTA BANKOWE
        // ==========================================
        Account accEwa = accountRepository.save(Account.builder().user(parentEwa).accountNumber("PL10000000100000001000000010").balance(new BigDecimal("25000.00")).currency("PLN").type("STANDARD").build());
        Account accJanek = accountRepository.save(Account.builder().user(juniorJanek).accountNumber("PL10000000100000001000000011").balance(new BigDecimal("120.00")).currency("PLN").type("JUNIOR").parentAccount(accEwa).build());
        Account accZuzia = accountRepository.save(Account.builder().user(juniorZuzia).accountNumber("PL10000000100000001000000012").balance(new BigDecimal("350.50")).currency("PLN").type("JUNIOR").parentAccount(accEwa).build());
        Account accAntek = accountRepository.save(Account.builder().user(juniorAntek).accountNumber("PL10000000100000001000000013").balance(new BigDecimal("45.00")).currency("PLN").type("JUNIOR").parentAccount(accEwa).build());
        Account accAnna = accountRepository.save(Account.builder().user(parentAnna).accountNumber("PL11110000111100001111000011").balance(new BigDecimal("15000.00")).currency("PLN").type("STANDARD").build());
        Account accPiotr = accountRepository.save(Account.builder().user(juniorPiotr).accountNumber("PL22220000222200002222000022").balance(new BigDecimal("150.50")).currency("PLN").type("JUNIOR").parentAccount(accAnna).build());
        Account accMichal = accountRepository.save(Account.builder().user(michal).accountNumber("PL55550000555500005555000055").balance(new BigDecimal("5000.00")).currency("PLN").type("STANDARD").build());
        Account accKatarzyna = accountRepository.save(Account.builder().user(katarzyna).accountNumber("PL66660000666600006666000066").balance(new BigDecimal("23400.00")).currency("PLN").type("STANDARD").build());
        Account accPodejrzany = accountRepository.save(Account.builder().user(podejrzany).accountNumber("PL88880000888800008888000088").balance(new BigDecimal("0.00")).currency("PLN").type("STANDARD").build());

        // ==========================================
        // 3. KARTY PŁATNICZE
        // ==========================================
        cardRepository.saveAll(List.of(
            Card.builder().account(accEwa).cardNumber("4100000000000000").transactionLimit(new BigDecimal("10000.00")).type("DEBIT").isBlocked(false).build(),
            Card.builder().account(accJanek).cardNumber("4100000000000001").transactionLimit(new BigDecimal("100.00")).type("PREPAID").isBlocked(false).build(),
            Card.builder().account(accZuzia).cardNumber("4100000000000002").transactionLimit(new BigDecimal("150.00")).type("PREPAID").isBlocked(false).build(),
            Card.builder().account(accAnna).cardNumber("4111111111111111").transactionLimit(new BigDecimal("5000.00")).type("DEBIT").isBlocked(false).build(),
            Card.builder().account(accMichal).cardNumber("4555555555555555").transactionLimit(new BigDecimal("10000.00")).type("CREDIT").isBlocked(false).build()
        ));

        // ==========================================
        // 4. TRANSAKCJE
        // ==========================================
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> transactions = new ArrayList<>();

        // --- PRZELEWY WEWNĘTRZNE ---
        transactions.add(createTx(accEwa, accJanek, "Jan Majewski", "Kieszonkowe - Luty", "50.00", "INTERNAL", "COMPLETED", now.minusDays(10)));
        transactions.add(createTx(accEwa, accZuzia, "Zuzanna Majewska", "Kieszonkowe - Luty", "50.00", "INTERNAL", "COMPLETED", now.minusDays(10)));
        transactions.add(createTx(accEwa, accAntek, "Antoni Majewski", "Kieszonkowe - Luty", "50.00", "INTERNAL", "COMPLETED", now.minusDays(10)));
        transactions.add(createTx(accEwa, accJanek, "Jan Majewski", "Za skoszenie trawnika", "30.00", "INTERNAL", "COMPLETED", now.minusDays(5)));
        transactions.add(createTx(accZuzia, accEwa, "Ewa Majewska", "Zwrot za kino", "25.00", "INTERNAL", "COMPLETED", now.minusDays(2)));
        transactions.add(createTx(accAntek, accJanek, "Jan Majewski", "Oddaję za grę", "15.00", "INTERNAL", "COMPLETED", now.minusDays(1)));
        
        // --- PRZELEWY WEWNĘTRZNE ---
        transactions.add(createTx(accAnna, accPiotr, "Piotr Nowak", "Na wycieczkę szkolną", "150.00", "INTERNAL", "COMPLETED", now.minusDays(7)));
        transactions.add(createTx(accKatarzyna, accMichal, "Michał Wiśniewski", "Za wczorajszą pizzę", "45.00", "INTERNAL", "COMPLETED", now.minusDays(3)));

        // --- PRZELEWY ZWYKŁE ELIXIR ---
        transactions.add(createTx(accMichal, accEwa, "Ewa Majewska", "Faktura FV/2026/02", "1500.00", "ELIXIR", "COMPLETED", now.minusDays(15)));
        transactions.add(createTx(accEwa, accKatarzyna, "Katarzyna Zielińska", "Zaliczka na projekt", "3200.00", "ELIXIR", "COMPLETED", now.minusDays(12)));
        transactions.add(createTx(accAnna, accMichal, "Michał Wiśniewski", "Kupno roweru (OLX)", "850.00", "ELIXIR", "COMPLETED", now.minusDays(8)));
        transactions.add(createTx(accKatarzyna, accAnna, "Anna Nowak", "Rozliczenie za prezent", "120.00", "ELIXIR", "COMPLETED", now.minusDays(6)));
        transactions.add(createExternalTx(accEwa, "PL99991111222233334444555566", "PGE Dystrybucja", "Rachunek za prąd", "340.50", "ELIXIR", "COMPLETED", now.minusDays(4)));
        transactions.add(createExternalTx(accMichal, "PL88881111222233334444555577", "Spółdzielnia Mieszkaniowa", "Czynsz Luty 2026", "890.00", "ELIXIR", "COMPLETED", now.minusDays(3)));
        transactions.add(createExternalTx(accKatarzyna, "PL77771111222233334444555588", "Urząd Skarbowy", "Podatek dochodowy PIT", "2100.00", "ELIXIR", "COMPLETED", now.minusDays(1)));
        
        // PENDING ELIXIR (Czekają na najbliższą sesję rozliczeniową)
        transactions.add(createExternalTx(accAnna, "PL55559999555599995555999955", "Orange Polska", "Abonament", "110.00", "ELIXIR", "PENDING", null));
        transactions.add(createExternalTx(accEwa, "PL44449999555599995555999955", "Allegro Pay", "Spłata raty", "250.00", "ELIXIR", "PENDING", null));

        // --- PRZELEWY NATYCHMIASTOWE (EXPRESS ELIXIR / RTGS) ---
        transactions.add(createTx(accMichal, accAnna, "Anna Nowak", "Szybki zwrot długu", "500.00", "INSTANT", "COMPLETED", now.minusHours(12)));
        transactions.add(createTx(accEwa, accMichal, "Michał Wiśniewski", "Pilna wpłata", "1500.00", "INSTANT", "COMPLETED", now.minusHours(5)));
        transactions.add(createExternalTx(accKatarzyna, "PL12341234123412341234123412", "Janusz Pol", "Zaliczka natychmiastowa", "3000.00", "INSTANT", "COMPLETED", now.minusHours(1)));

        // --- SYSTEM AML ---
        Transaction amlTx1 = createExternalTx(accPodejrzany, "CY11223344556677889900", "Offshore Corp", "Consulting", "50000.00", "SWIFT", "HELD_FOR_AML", null);
        amlTx1.setAmlClientExplanation("To zapłata za legalne doradztwo biznesowe.");
        amlTx1.setAmlBankDecisionNote("Wymagana weryfikacja źródła pochodzenia majątku.");
        transactions.add(amlTx1);

        Transaction amlTx2 = createExternalTx(accJanek, "PL99990000999900009999000099", "KASyno ONLINE", "Doładowanie VIP", "100.00", "INSTANT", "REJECTED_AML", now);
        amlTx2.setAmlBankDecisionNote("Blokada: Konto Junior próbuje zasilić podmiot hazardowy.");
        transactions.add(amlTx2);

        transactionRepository.saveAll(transactions);

        System.out.println("Pomyślnie załadowano " + transactions.size() + " transakcji testowych do bazy!");
    }

    private Transaction createTx(Account sender, Account receiver, String recName, String title, String amount, String type, String status, LocalDateTime execDate) {
        return Transaction.builder()
                .senderAccount(sender).senderAccountNumber(sender.getAccountNumber())
                .receiverAccount(receiver).receiverAccountNumber(receiver.getAccountNumber())
                .receiverName(recName).title(title)
                .amount(new BigDecimal(amount)).currency("PLN").status(status).type(type)
                .executionDate(execDate).build();
    }

    private Transaction createExternalTx(Account sender, String recAccNum, String recName, String title, String amount, String type, String status, LocalDateTime execDate) {
        return Transaction.builder()
                .senderAccount(sender).senderAccountNumber(sender.getAccountNumber())
                .receiverAccountNumber(recAccNum)
                .receiverName(recName).title(title)
                .amount(new BigDecimal(amount)).currency("PLN").status(status).type(type)
                .executionDate(execDate).build();
    }
}