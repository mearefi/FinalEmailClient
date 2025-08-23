package com.milou.cli.dao;

import com.milou.cli.db.HibernateUtil;
import com.milou.cli.exception.DuplicateEmailException;
import com.milou.cli.exception.UserNotFoundException;
import com.milou.cli.model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class UserDao {

    public boolean existsByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("select count(u.id) from User u where u.email = :email", Long.class)
                    .setParameter("email", email)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public void saveOrThrow(User u) {
        if (existsByEmail(u.getEmail())) {
            throw new DuplicateEmailException(u.getEmail());
        }
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(u);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw ex;
        }
    }

    public User findByEmailOrThrow(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> q = session.createQuery("from User u where u.email = :email", User.class);
            q.setParameter("email", email);
            User u = q.uniqueResult();
            if (u == null) throw new UserNotFoundException(email);
            return u;
        }
    }
}
