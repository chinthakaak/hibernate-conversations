package com.examples.concurrency;

import junit.framework.Assert;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Created by ka40215 on 11/27/15.
 */
public class OptimisticConcurrencyDisabledLastCommitWins {


    public static class OptimisticConcurrencyDisabledLastCommitWinsNoVersionTest {
        @Test
        public void testOptimisticConcurrencyDisabledLastCommitWinsNoVersion() throws InterruptedException {
            Session session = HibernateUtil.buildSessionFactoryCreate().openSession();
            Transaction tx = session.beginTransaction();
            User user = new User();
            user.setName("D1");
            session.save(user);
            tx.commit();
            session.close();
            new Thread() {
                @Override
                public void run() {
                    Session session = HibernateUtil.buildSessionFactoryUpdateIsolation(2).openSession();
                    Transaction tx =  session.beginTransaction();
                    User user = (User)session.get(User.class,1);
                    Assert.assertEquals("D1", user.getName());
                    user.setName("First Commit");
                    tx.commit();
                    session.close();
                }
            }.start();

            new Thread() {
                @Override
                public void run() {
                    Session session = HibernateUtil.buildSessionFactoryUpdateIsolation(2).openSession();
                    Transaction tx = session.beginTransaction();
                    User user = (User)session.get(User.class,1);
                    Assert.assertEquals("D1", user.getName());
                    user.setName("Second Commit");
                    delay(1);
                    tx.commit();
                    session.close();
                }
            }.start();
            delay(5);
            Session session2 = HibernateUtil.buildSessionFactoryUpdateIsolation(2).openSession();
            Transaction tx2 = session2.beginTransaction();
            User user1 = (User) session2.get(User.class, 1);
            Assert.assertEquals("Second Commit", user1.getName());
            tx2.commit();
            session2.close();
        }

        public void delay(int interval) {
            try {
                Thread.sleep(interval * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    @Entity
    @Table(name = "USERS")
    private static class User {
        @Id
        @SequenceGenerator(name = "seq", sequenceName = "seq")
        @GeneratedValue(generator = "seq")
        private int userId;

        @Column(name = "NAME")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }
    }

    private static class HibernateUtil {
        private static SessionFactory buildSessionFactory(String hbm2ddlAuto, int isolation) {
            try {
                // Create the SessionFactory programmatically
                return new AnnotationConfiguration()
                        .setProperty(Environment.DRIVER, "oracle.jdbc.driver.OracleDriver")
                        .setProperty(Environment.URL, "jdbc:oracle:thin:@127.0.0.1:1521:xe")
                        .setProperty(Environment.USER, "HBCONVERSATIONS")
                        .setProperty(Environment.PASS, "password")
                        .setProperty(Environment.DIALECT, "org.hibernate.dialect.Oracle10gDialect")
                        .setProperty(Environment.SHOW_SQL, "true")
                        .setProperty(Environment.HBM2DDL_AUTO, hbm2ddlAuto)
                        .setProperty(Environment.ISOLATION, ""+isolation)
                        .addAnnotatedClass(User.class)
                        .buildSessionFactory();

            } catch (Throwable ex) {
                // Make sure you log the exception, as it might be swallowed
                System.err.println("Initial SessionFactory creation failed." + ex);
                throw new ExceptionInInitializerError(ex);
            }
        }

        private static SessionFactory buildSessionFactoryCreate() {
            return buildSessionFactory("create", 2);
        }

        private static SessionFactory buildSessionFactoryUpdate() {
            return buildSessionFactory("update", 2);
        }
        private static SessionFactory buildSessionFactoryUpdateIsolation(int isolation) {
            return buildSessionFactory("update", isolation);
        }
    }
}
