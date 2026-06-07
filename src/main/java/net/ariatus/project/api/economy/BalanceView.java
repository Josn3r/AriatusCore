package net.ariatus.project.api.economy;

import java.math.BigDecimal;
import java.util.UUID;

public interface BalanceView {

    UUID uuid();

    Currency currency();

    BigDecimal balance();
}