package com.milou.cli.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {
    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("finalEmailClientPU");

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
}
