package com.snowgears.shop;

public enum ShopType {

    SELL(0),

    BUY(1),

    BARTER(2);

    private final int slot;

    ShopType(int slot) {
        this.slot = slot;
    }

    @Override
    public String toString() {
        if (this == ShopType.SELL)
            return "sell";
        else if (this == ShopType.BUY)
            return "buy";
        else
            return "barter";
    }
}