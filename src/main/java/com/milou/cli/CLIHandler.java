package com.milou.cli;

import com.milou.cli.dao.UserDao;
import com.milou.cli.db.JPAUtil;
import com.milou.cli.exception.DuplicateEmailException;
import com.milou.cli.exception.InvalidCredentialsException;
import com.milou.cli.exception.UserNotFoundException;
import com.milou.cli.model.User;
import com.milou.cli.service.EmailService;
import jakarta.persistence.EntityManager;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Scanner;

public class CLIHandler {
    private final Scanner scanner = new Scanner(System.in);
    private User currentUser;
    private final EmailService emailService = new EmailService();

    public void start() {
        while (true) {
            System.out.print("[L]ogin, [S]ign up: ");
            String cmd = scanner.nextLine().trim();
            if (cmd.equalsIgnoreCase("L") || cmd.equalsIgnoreCase("Login")) {
                if (login()) {
                    mainMenu();
                }
            } else if (cmd.equalsIgnoreCase("S") || cmd.equalsIgnoreCase("Sign up")) {
                signup();
            } else {
                System.out.println("Invalid command.");
            }
        }
    }

    private void signup() {
        System.out.print("Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        if (!email.contains("@")) email = email + "@milou.com";
        System.out.print("Password: ");
        String pass = scanner.nextLine();
        if (pass.length() < 8) {
            System.out.println("Password must be at least 8 characters.");
            return;
        }

        EntityManager em = JPAUtil.getEntityManager();
        UserDao userDao = new UserDao(em);

        em.getTransaction().begin();
        try {
            User u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setPasswordHash(hashPassword(pass));
            userDao.saveOrThrow(u);
            em.getTransaction().commit();
            System.out.println("Your new account is created.\nGo ahead and login!");
        } catch (DuplicateEmailException ex) {
            em.getTransaction().rollback();
            System.out.println("Email already taken. Try again.");
        } catch (Exception ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            ex.printStackTrace();
            System.out.println("Error during signup.");
        } finally {
            em.close();
        }
    }

    private boolean login() {
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        if (!email.contains("@")) email = email + "@milou.com";
        System.out.print("Password: ");
        String pass = scanner.nextLine();

        EntityManager em = JPAUtil.getEntityManager();
        UserDao userDao = new UserDao(em);

        em.getTransaction().begin();
        try {
            User u = userDao.findByEmailOrThrow(email);
            if (!verifyPassword(pass, u.getPasswordHash())) {
                throw new InvalidCredentialsException();
            }
            this.currentUser = u;
            System.out.println("Welcome back, " + u.getName() + "!");
            // show unread summary
            emailService.viewList(currentUser, "unread");
            em.getTransaction().commit();
            return true;
        } catch (UserNotFoundException | InvalidCredentialsException ex) {
            System.out.println("Invalid credentials.");
            em.getTransaction().rollback();
            return false;
        } catch (Exception ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            ex.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }

    private void mainMenu() {
        while (true) {
            System.out.print("[S]end, [V]iew, [R]eply, [F]orward, [L]ogout: ");
            String cmd = scanner.nextLine().trim();
            if (cmd.equalsIgnoreCase("S")) {
                emailService.sendEmail(currentUser);
            } else if (cmd.equalsIgnoreCase("V")) {
                System.out.print("[A]ll, [U]nread, [S]ent, Read by [C]ode: ");
                String sub = scanner.nextLine().trim();
                if (sub.equalsIgnoreCase("A")) emailService.viewList(currentUser, "all");
                else if (sub.equalsIgnoreCase("U")) emailService.viewList(currentUser, "unread");
                else if (sub.equalsIgnoreCase("S")) emailService.viewList(currentUser, "sent");
                else if (sub.equalsIgnoreCase("C")) emailService.readByCode(currentUser);
                else System.out.println("Unknown view option.");
            } else if (cmd.equalsIgnoreCase("R")) {
                emailService.reply(currentUser);
            } else if (cmd.equalsIgnoreCase("F")) {
                emailService.forward(currentUser);
            } else if (cmd.equalsIgnoreCase("L")) {
                this.currentUser = null;
                System.out.println("Logged out.");
                break;
            } else {
                System.out.println("Unknown command.");
            }
        }
    }

    private String hashPassword(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    private boolean verifyPassword(String plain, String hash) {
        return hashPassword(plain).equals(hash);
    }
}
