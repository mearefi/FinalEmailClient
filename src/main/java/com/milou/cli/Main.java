package com.milou.cli;

import com.milou.cli.db.HibernateUtil;

public class Main {
    public static void main(String[] args) {
        try {
            new CLIHandler().start();
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
