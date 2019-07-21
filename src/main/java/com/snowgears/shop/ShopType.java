package com.snowgears.shop;

public enum ShopType {
    SELL,
    BUY,
    BARTER,
    GAMBLE,
    COMBO;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}