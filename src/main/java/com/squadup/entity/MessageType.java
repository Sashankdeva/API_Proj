package com.squadup.entity;

public enum MessageType {
    DM, GROUP;

    public String json() {
        return name().toLowerCase();
    }
}
