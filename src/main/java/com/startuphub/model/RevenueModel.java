package com.startuphub.model;

/** Business/revenue models supported by the automatic revenue calculator. */
public enum RevenueModel {
    SUBSCRIPTION("Subscription"),
    TRANSACTION("Transaction / commission"),
    ADVERTISEMENT("Advertisement");

    private final String label;

    RevenueModel(String label) { this.label = label; }

    public String getLabel() { return label; }
}
