package net.ariatus.project.economy;

import net.ariatus.project.api.economy.BalanceView;
import net.ariatus.project.api.economy.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public class Balance implements BalanceView {

    private final UUID uuid;
    private final Currency currency;
    private final BigDecimal balance;

    public Balance(UUID uuid, Currency currency, BigDecimal balance) {
        this.uuid = uuid;
        this.currency = currency;
        this.balance = balance;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public Currency currency() {
        return currency;
    }

    @Override
    public BigDecimal balance() {
        return balance;
    }
}