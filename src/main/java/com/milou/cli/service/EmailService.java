package com.milou.cli.service;

import com.milou.cli.db.HibernateUtil;
import com.milou.cli.model.Email;
import com.milou.cli.model.Recipient;
import com.milou.cli.model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EmailService {
    private final Scanner scanner = new Scanner(System.in);
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom rnd = new SecureRandom();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    public void sendEmail(User sender) {
        if (sender == null) {
            System.out.println("لطفاً ابتدا وارد حساب خود شوید.");
            return;
        }

        System.out.print("Recipient(s) (comma separated): ");
        String recipientsLine = scanner.nextLine().trim();
        if (recipientsLine.isEmpty()) {
            System.out.println("No recipients entered.");
            return;
        }
        String[] recipients = recipientsLine.split("\\s*,\\s*");

        System.out.print("Subject: ");
        String subject = scanner.nextLine();

        System.out.print("Body: ");
        String body = scanner.nextLine();

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            Email email = new Email();
            email.setSender(sender);
            email.setSubject(subject);
            email.setBody(body);
            email.setCode(randomCode(6));
            session.save(email);
            session.flush();

            List<String> skipped = new ArrayList<>();
            for (String r : recipients) {
                if (!r.contains("@")) r = r + "@milou.com";
                Query<User> q = session.createQuery("from User u where u.email = :email", User.class);
                q.setParameter("email", r);
                List<User> found = q.getResultList();
                if (found.isEmpty()) {
                    skipped.add(r);
                    continue;
                }
                User u = found.get(0);
                Recipient rec = new Recipient();
                rec.setEmail(email);
                rec.setUser(u);
                rec.setRead(false);
                session.save(rec);
            }

            tx.commit();
            System.out.println("Successfully sent your email.");
            System.out.println("Code: " + email.getCode());
            if (!skipped.isEmpty()) {
                System.out.println("Warning: these recipients not found and were skipped: " + String.join(", ", skipped));
            }
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            System.out.println("Error sending email: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void viewList(User currentUser, String mode) {
        if (currentUser == null) {
            System.out.println("لطفاً ابتدا وارد شوید.");
            return;
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            List<Email> list;
            switch (mode.toLowerCase()) {
                case "all":
                    list = session.createQuery(
                                    "select r.email from Recipient r where r.user.id = :uid order by r.email.sentAt desc", Email.class)
                            .setParameter("uid", currentUser.getId())
                            .getResultList();
                    System.out.println("All Emails:");
                    break;
                case "unread":
                    list = session.createQuery(
                                    "select r.email from Recipient r where r.user.id = :uid and r.isRead = false order by r.email.sentAt desc", Email.class)
                            .setParameter("uid", currentUser.getId())
                            .getResultList();
                    System.out.println("Unread Emails:");
                    break;
                case "sent":
                    list = session.createQuery(
                                    "from Email e where e.sender.id = :uid order by e.sentAt desc", Email.class)
                            .setParameter("uid", currentUser.getId())
                            .getResultList();
                    System.out.println("Sent Emails:");
                    break;
                default:
                    System.out.println("Unknown mode");
                    return;
            }

            if (list.isEmpty()) {
                System.out.println("No emails.");
                return;
            }

            for (int i = 0; i < list.size(); i++) {
                Email e = list.get(i);
                if (mode.equalsIgnoreCase("sent")) {
                    List<String> rcps = session.createQuery(
                                    "select r.user.email from Recipient r where r.email.id = :eid", String.class)
                            .setParameter("eid", e.getId())
                            .getResultList();
                    System.out.printf("[%d] + %s - %s (%s)%n", i + 1, String.join(", ", rcps), truncate(e.getSubject()), e.getCode());
                } else {
                    System.out.printf("[%d] + %s - %s (%s)%n", i + 1, e.getSender().getEmail(), truncate(e.getSubject()), e.getCode());
                }
            }

            System.out.print("Enter email [index or code] to read (or press Enter to return): ");
            String in = scanner.nextLine().trim();
            if (in.isEmpty()) return;

            String codeToRead;
            try {
                int idx = Integer.parseInt(in);
                if (idx >= 1 && idx <= list.size()) {
                    codeToRead = list.get(idx - 1).getCode();
                } else {
                    System.out.println("Invalid index.");
                    return;
                }
            } catch (NumberFormatException nfe) {
                codeToRead = in;
            }

            readByCode(currentUser, codeToRead);
        } finally {
            session.close();
        }
    }

    public void readByCode(User currentUser, String code) {
        if (currentUser == null) { System.out.println("Login first."); return; }
        if (code == null || code.isEmpty()) { System.out.println("Code is empty."); return; }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            Query<Email> q = session.createQuery("from Email e where e.code = :code", Email.class);
            q.setParameter("code", code);
            Email e = q.uniqueResult();
            if (e == null) {
                System.out.println("No email with that code.");
                return;
            }

            boolean allowed = false;
            if (e.getSender().getId().equals(currentUser.getId())) allowed = true;
            Long cnt = session.createQuery(
                            "select count(r) from Recipient r where r.email.id = :eid and r.user.id = :uid", Long.class)
                    .setParameter("eid", e.getId()).setParameter("uid", currentUser.getId()).uniqueResult();
            if (cnt != null && cnt > 0) allowed = true;
            if (!allowed) {
                System.out.println("You cannot read this email.");
                return;
            }

            List<String> rcps = session.createQuery(
                            "select r.user.email from Recipient r where r.email.id = :eid", String.class)
                    .setParameter("eid", e.getId())
                    .getResultList();

            System.out.println();
            System.out.println("Code: " + e.getCode());
            System.out.println("Recipient(s): " + String.join(", ", rcps));
            System.out.println("Subject: " + (e.getSubject() == null ? "" : e.getSubject()));
            System.out.println("Date: " + (e.getSentAt() == null ? "" : e.getSentAt().toLocalDate().format(DF)));
            System.out.println();
            System.out.println(e.getBody());
            System.out.println();

            tx = session.beginTransaction();
            session.createQuery("update Recipient r set r.isRead = true where r.email.id = :eid and r.user.id = :uid")
                    .setParameter("eid", e.getId()).setParameter("uid", currentUser.getId()).executeUpdate();
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            ex.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void readByCode(User currentUser) {
        System.out.print("Code: ");
        String code = scanner.nextLine().trim();
        readByCode(currentUser, code);
    }

    public void reply(User currentUser) {
        if (currentUser == null) { System.out.println("Login first."); return; }
        System.out.print("Code: ");
        String code = scanner.nextLine().trim();
        System.out.print("Body: ");
        String body = scanner.nextLine();

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Query<Email> q = session.createQuery("from Email e where e.code = :code", Email.class);
            q.setParameter("code", code);
            Email original = q.uniqueResult();
            if (original == null) {
                System.out.println("Email not found.");
                tx.rollback();
                return;
            }

            Long cnt = session.createQuery("select count(r) from Recipient r where r.email.id = :eid and r.user.id = :uid", Long.class)
                    .setParameter("eid", original.getId()).setParameter("uid", currentUser.getId()).uniqueResult();
            if (!(original.getSender().getId().equals(currentUser.getId()) || (cnt != null && cnt > 0))) {
                System.out.println("You cannot reply to this email.");
                tx.rollback();
                return;
            }

            List<String> rcpsEmails = session.createQuery(
                            "select distinct u.email from Recipient r join r.user u where r.email.id = :eid", String.class)
                    .setParameter("eid", original.getId())
                    .getResultList();
            if (!rcpsEmails.contains(original.getSender().getEmail())) rcpsEmails.add(original.getSender().getEmail());

            Email reply = new Email();
            reply.setSender(currentUser);
            reply.setSubject("[Re] " + (original.getSubject() == null ? "" : original.getSubject()));
            reply.setBody(body);
            reply.setCode(randomCode(6));
            session.save(reply);
            session.flush();

            for (String re : rcpsEmails) {
                if (!re.contains("@")) re = re + "@milou.com";
                Query<User> uq = session.createQuery("from User u where u.email = :email", User.class);
                uq.setParameter("email", re);
                List<User> found = uq.getResultList();
                if (found.isEmpty()) continue;
                Recipient rec = new Recipient();
                rec.setEmail(reply);
                rec.setUser(found.get(0));
                rec.setRead(false);
                session.save(rec);
            }

            tx.commit();
            System.out.println("Successfully sent your reply to email " + code + ".");
            System.out.println("Code: " + reply.getCode());
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            ex.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void forward(User currentUser) {
        if (currentUser == null) { System.out.println("Login first."); return; }
        System.out.print("Code: ");
        String code = scanner.nextLine().trim();
        System.out.print("Recipient(s): ");
        String recipientsLine = scanner.nextLine().trim();
        String[] recipients = recipientsLine.split("\\s*,\\s*");

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Query<Email> q = session.createQuery("from Email e where e.code = :code", Email.class);
            q.setParameter("code", code);
            Email original = q.uniqueResult();
            if (original == null) {
                System.out.println("Email not found.");
                tx.rollback();
                return;
            }

            Long cnt = session.createQuery("select count(r) from Recipient r where r.email.id = :eid and r.user.id = :uid", Long.class)
                    .setParameter("eid", original.getId()).setParameter("uid", currentUser.getId()).uniqueResult();
            if (!(original.getSender().getId().equals(currentUser.getId()) || (cnt != null && cnt > 0))) {
                System.out.println("You cannot forward this email.");
                tx.rollback();
                return;
            }

            Email fw = new Email();
            fw.setSender(currentUser);
            fw.setSubject("[Fw] " + (original.getSubject() == null ? "" : original.getSubject()));
            fw.setBody(original.getBody());
            fw.setCode(randomCode(6));
            session.save(fw);
            session.flush();

            List<String> skipped = new ArrayList<>();
            for (String r : recipients) {
                if (!r.contains("@")) r = r + "@milou.com";
                Query<User> uq = session.createQuery("from User u where u.email = :email", User.class);
                uq.setParameter("email", r);
                List<User> found = uq.getResultList();
                if (found.isEmpty()) {
                    skipped.add(r);
                    continue;
                }
                Recipient rec = new Recipient();
                rec.setEmail(fw);
                rec.setUser(found.get(0));
                rec.setRead(false);
                session.save(rec);
            }

            tx.commit();
            System.out.println("Successfully forwarded your email.");
            System.out.println("Code: " + fw.getCode());
            if (!skipped.isEmpty()) {
                System.out.println("Warning: these recipients not found and were skipped: " + String.join(", ", skipped));
            }
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            ex.printStackTrace();
        } finally {
            session.close();
        }
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 40 ? s.substring(0, 37) + "..." : s;
    }
}
