package com.mycompany;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptBanknotesTests {

    @Test
    void acceptSuccessfully() {
        final Account account = new Account(true, new Balance(100L));

        Collection<Banknote> banknotes = List.of(
                new Banknote(Nominal.FIFTY),
                new Banknote(Nominal.HUNDRED),
                new Banknote(Nominal.FIVE_HUNDRED),
                new Banknote(Nominal.THOUSAND));
        assertTrue(ATM.loginWith(account).acceptBanknotes(banknotes),
                "Banknotes are not accepted");
    }
}
