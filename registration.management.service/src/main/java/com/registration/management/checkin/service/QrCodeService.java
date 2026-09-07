package com.registration.management.checkin.service;

public interface QrCodeService {

    byte[] generateQrCodePng(String text, int width, int height);

    String generateQrCodeBase64(String text, int width, int height);
}
