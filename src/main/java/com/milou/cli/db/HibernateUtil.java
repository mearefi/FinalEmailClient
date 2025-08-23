package com.milou.cli.db;

import com.milou.cli.model.Email;
import com.milou.cli.model.Recipient;
import com.milou.cli.model.RecipientId;
import com.milou.cli.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");

            // ثبت صریح کلاس‌های انوتیت‌شده
            configuration.addAnnotatedClass(User.class);
            configuration.addAnnotatedClass(Email.class);
            configuration.addAnnotatedClass(Recipient.class);
            // اگر کلاس RecipientId به عنوان اَنوته دارید (composite key)، اضافه کن:
            try {
                configuration.addAnnotatedClass(RecipientId.class);
            } catch (Throwable ignored) {}

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties()).build();
            return configuration.buildSessionFactory(serviceRegistry);
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
