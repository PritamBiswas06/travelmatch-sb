
package com.pvp.travelmatch.service;

/**
 * Provider-neutral SMS gateway interface.
 *
 * This allows you to replace the SMS provider later
 * without changing the OTP verification logic.
 */
public interface SmsGateway {

    /**
     * Sends an OTP to the destination phone number.
     *
     * @param destination recipient phone number
     * @param code        OTP code to send
     * @return provider message reference
     */
    String sendOtp(String destination, String code);
}