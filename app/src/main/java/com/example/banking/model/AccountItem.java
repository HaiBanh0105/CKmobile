package com.example.banking.model;

public class AccountItem {
    public static final int TYPE_SAVINGS = 1;
    public static final int TYPE_MORTGAGE = 2;

    private int type;
    private SavingsAccount savings;
    private MortgageAccount mortgage;

    public AccountItem(SavingsAccount s) {
        type = TYPE_SAVINGS;
        savings = s;
    }

    public AccountItem(MortgageAccount m) {
        type = TYPE_MORTGAGE;
        mortgage = m;
    }

    public int getType() {
        return type;
    }

    public SavingsAccount getSavings() {
        return savings;
    }

    public MortgageAccount getMortgage() {
        return mortgage;
    }
}
