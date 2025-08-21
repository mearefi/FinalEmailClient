package com.milou.cli.service;

import com.milou.cli.model.Email;
import com.milou.cli.model.User;
import com.milou.cli.db.JPAUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Scanner;

public class EmailService {

    private final Scanner scanner = new Scanner(System.in);

    public void sendEmail(User sender) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            System.out.print("Enter recipient email: ");
            String recipientEmail = scanner.nextLine();

            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", recipientEmail);
            List<User> recipients = query.getResultList();

            if (recipients.isEmpty()) {
                System.out.println("Recipient not found!");
                return;
            }

            User recipient = recipients.get(0);

            System.out.print("Enter subject: ");
            String subject = scanner.nextLine();

            System.out.print("Enter body: ");
            String body = scanner.nextLine();

            Email email = new Email();
            email.setSender(sender);
            email.setRecipients(Recipient);
            email.setSubject(subject);
            email.setBody(body);
            email.setCode("code-" + System.currentTimeMillis());

            em.getTransaction().begin();
            em.persist(email);
            em.getTransaction().commit();

            System.out.println("Email sent successfully!");
        } finally {
            em.close();
        }
    }

    public void viewInbox(User currentUser) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Email> query = em.createQuery(
                    "SELECT e FROM Email e WHERE e.recipient = :user", Email.class);
            query.setParameter("user", currentUser);

            List<Email> inbox = query.getResultList();

            if (inbox.isEmpty()) {
                System.out.println("📭 Your inbox is empty.");
                return;
            }

            System.out.println("📩 Your Inbox:");
            for (Email e : inbox) {
                System.out.println("From: " + e.getSender().getEmail());
                System.out.println("Subject: " + e.getSubject());
                System.out.println("Body: " + e.getBody());
                System.out.println("------");
            }
        } finally {
            em.close();
        }
    }
}
