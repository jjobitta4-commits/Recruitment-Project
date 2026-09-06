package com.recruitment.util;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EmailService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, String> RECENT_CODES = new ConcurrentHashMap<>();

    public static String generateCode() {
        int num = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(num);
    }

    public static boolean sendVerificationEmail(String toEmail, String code) {
        return sendVerificationEmail(toEmail, code, "REGISTRATION");
    }

    public static boolean sendVerificationEmail(String toEmail, String code, String purpose) {
        String clean = cleanEmail(toEmail);
        RECENT_CODES.put(clean, code);

        String subject = "RecruitHub Verification Code: " + code;
        String actionName = purpose.equalsIgnoreCase("REGISTRATION") ? "Verify Your Registration" : "Confirm Your Identity";

        System.out.println("");
        System.out.println("  +===========================================================+");
        System.out.println("  |             RECRUITHUB AUTOMATED EMAIL DISPATCH           |");
        System.out.println("  +===========================================================+");
        System.out.printf("  | TO:      %-48s |\n", toEmail);
        System.out.printf("  | SUBJECT: %-48s |\n", subject);
        System.out.println("  +-----------------------------------------------------------+");
        System.out.println("  | Dear User,                                                |");
        System.out.printf("  | Please use the following code to %-24s |\n", actionName + ": ");
        System.out.println("  |                                                           |");
        System.out.printf("  |               >>>   [ %c %c %c %c %c %c ]   <<<                 |\n",
                code.charAt(0), code.charAt(1), code.charAt(2),
                code.charAt(3), code.charAt(4), code.charAt(5));
        System.out.println("  |                                                           |");
        System.out.println("  | This verification code is valid for 10 minutes.           |");
        System.out.println("  | If you did not request this, please ignore this email.    |");
        System.out.println("  +===========================================================+");
        System.out.println("");

        return true;
    }

    public static String getRecentCode(String email) {
        return RECENT_CODES.get(cleanEmail(email));
    }

    private static String cleanEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
