package net.ariatus.project.api.economy;

public enum Currency {

    COINS(
            "COINS",
            "$",
            "Moneda principal",
            false
    ),

    ODRYS(
            "ODRYS",
            "Odrys",
            "Moneda premium",
            true
    );

    private final String id;
    private final String symbol;
    private final String displayName;
    private final boolean premium;

    Currency(String id, String symbol, String displayName, boolean premium) {
        this.id = id;
        this.symbol = symbol;
        this.displayName = displayName;
        this.premium = premium;
    }

    public String id() {
        return id;
    }

    public String symbol() {
        return symbol;
    }

    public String displayName() {
        return displayName;
    }

    public boolean premium() {
        return premium;
    }

    public static Currency from(String value) {
        for (Currency currency : values()) {
            if (currency.id.equalsIgnoreCase(value) || currency.name().equalsIgnoreCase(value)) {
                return currency;
            }
        }

        throw new IllegalArgumentException("Moneda desconocida: " + value);
    }
}