package com.milou.cli.dao;

import com.milou.cli.exception.DuplicateEmailException;
import com.milou.cli.exception.UserNotFoundException;
import com.milou.cli.model.User;
import jakarta.persistence.EntityManager;

import java.lang.runtime.ObjectMethods;

public class UserDao {
    private final EntityManager em;

    public UserDao(EntityManager em) {
        this.em = em;
    }

    public boolean existsByEmail(String email) {
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }


    public void saveOrThrow(User u) {
        if (existsByEmail(u.getEmail())) {
            throw new DuplicateEmailException(u.getEmail());
        }
        em.persist(u);
    }


    public User findByEmailOrThrow(String email) {
        return em.createQuery(
                        "SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException(email));
    }


    public void updatePassword(User u, String newHashedPassword) {
        u.setPasswordHash(newHashedPassword);
        em.merge(u);
    }


    public void delete(User u) {
        em.remove(em.contains(u) ? u : em.merge(u));
    }
}
