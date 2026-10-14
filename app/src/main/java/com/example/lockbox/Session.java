package com.example.lockbox;

public class Session {
    private static String masterPin = "";

    public static void setMasterPin(String pin) {
        masterPin = pin;
    }

    public static String getMasterPin() {
        return masterPin;
    }
}