package capstoneBackend.ca.sheridancollege.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public void sendOtpSms(String toPhoneNumber, String otp) {
        Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromPhoneNumber),
                "Your Trend Sense code is: " + otp + ". Expires in 10 minutes."
        ).create();
    }

    public void sendReviewConfirmationSms(String toPhoneNumber, long caseNumber) {
        Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromPhoneNumber),
                "Trend Sense: Your review has been received. Case #" + caseNumber + ". We'll get back to you soon."
        ).create();
    }

    public void sendAdminReplySms(String toPhoneNumber, long caseNumber, String reply) {
        Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromPhoneNumber),
                "Trend Sense (Case #" + caseNumber + "): " + reply
        ).create();
    }
}
