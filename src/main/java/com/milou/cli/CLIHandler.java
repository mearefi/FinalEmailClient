package com.milou.cli;

import com.milou.cli.dao.UserDao;
import com.milou.cli.exception.InvalidCredentialsException;
import com.milou.cli.exception.UserNotFoundException;
import com.milou.cli.model.User;
import com.milou.cli.db.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.Scanner;

public class CLIHandler {
    private final Scanner scanner = new Scanner(System.in);
    private final EntityManager em = JPAUtil.getEntityManager();
    private final UserDao userDao = new UserDao(em);
    private User currentUser;


    public void start() {
        while (true) {
            System.out.print("[L]ogin, [S]ign up: ");
            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("l") || cmd.equals("login")) {
                if (login()) break;
            } else if (cmd.equals("s") || cmd.equals("sign up")) {
                signup();
            } else {
                System.out.println("Invalid command.");
            }
        }
    }

    private boolean signup() {
        System.out.print("Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = normalizeEmail(scanner.nextLine().trim());

        System.out.print("Password: ");
        String pass = scanner.nextLine();

        if (pass.length() < 8) {
            System.out.println("Password must be at least 8 characters.");
            return false;
        }

        em.getTransaction().begin();
        if (userDao.existsByEmail(email)) {
            System.out.println("This email is already taken.");
            em.getTransaction().rollback();
            return false;
        }

        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(hashPassword(pass));
        userDao.saveOrThrow(u);
        em.getTransaction().commit();

        System.out.println("Your new account is created. Go ahead and login!");
        return false;
    }

    private boolean login() {
        System.out.print("Email: ");
        String email = normalizeEmail(scanner.nextLine().trim());

        System.out.print("Password: ");
        String pass = scanner.nextLine();

        em.getTransaction().begin();
        try {
            User user = userDao.findByEmailOrThrow(email);

            if (!user.getPasswordHash().equals(pass)) {
                throw new InvalidCredentialsException();
            }

            this.currentUser = user;
            System.out.println("Login successful. Welcome, " + user.getName() + "!");
            em.getTransaction().commit();
            return true;

        } catch (UserNotFoundException | InvalidCredentialsException e) {
            System.out.println("Login failed: " + e.getMessage());
            em.getTransaction().rollback();
            return false;
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            em.getTransaction().rollback();
            return false;
        }
    }


    private String normalizeEmail(String input) {
        return input.contains("@") ? input : input + "@milou.com";
    }

    private String hashPassword(String plain) {
        // TODO: bcrypt.hash(plain)
        return plain;
    }

    private boolean verifyPassword(String plain, String hash) {
        // TODO: bcrypt.verify(plain, hash)
        return plain.equals(hash);
    }
}
