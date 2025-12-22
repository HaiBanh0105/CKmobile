package com.example.banking.model;

public class MortgageAccount {
    private String account_number;
    private double remaining_debt;
    private double monthly_payment;

    public MortgageAccount() {}

    public String getAccount_number() {
        return account_number;
    }

    public double getRemaining_debt() {
        return remaining_debt;
    }

    public double getMonthly_payment() {
        return monthly_payment;
    }
}
