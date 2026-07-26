package com.example.th06876_java202.Service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class OtpService {

    public String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}