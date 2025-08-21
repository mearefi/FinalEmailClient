package com.milou.cli;

import com.milou.cli.model.*;
import com.milou.cli.service.EmailService;

public class Main {
    public static void main(String[] args) {
        User loggedInUser = currentUser;

        EmailService emailService = new EmailService();

        // مثال:
        emailService.sendEmail(loggedInUser);
        emailService.viewInbox(loggedInUser);
    }
}
