package com.examples.concurrency.isolationissues;

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
    tx1 ----------------------------------------> 3. commit tx1
                    1. update D1

                                2. update D1
    tx2 -----------------------------------------> 4. rollback tx2

 Oracle supports only read committed and serialization isolation levels. So these are not demonstrable in oracle
 */
public class LostUpdateProblem {

    public static class LostUpdateProblemTest {
        @Test
        public void testLostUpdate() throws InterruptedException {

            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Session session = HibernateUtil.buildSessionFactoryCreate().openSession();

                    Transaction tx1 = session.beginTransaction();
                    User user1 = new User();
                    user1.setName("D1");
                    session.save(user1);
                    tx1.commit();

                    User user = (User)session.get(User.class,1);

                    Assert.assertEquals("D1", user.getName());
                    session.close();
                }
            }.start();

            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Session session = HibernateUtil.buildSessionFactoryUpdate().openSession();

                    Transaction tx2 = session.beginTransaction();
                    User user1 = new User();
                    user1.setName("D2");
                    user1.setUserId(1);
                    session.saveOrUpdate(user1);
                    tx2.rollback();
//                  tx2.commit();
                    User user = (User)session.get(User.class, 1);

                    Assert.assertEquals(null, user.getName());
                    session.close();
                }
            }.start();
            Thread.sleep(5000);
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
        private static SessionFactory buildSessionFactory(String hbm2ddlAuto) {
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
                        .setProperty(Environment.ISOLATION, "2")
                        .addAnnotatedClass(User.class)
                        .buildSessionFactory();

            } catch (Throwable ex) {
                // Make sure you log the exception, as it might be swallowed
                System.err.println("Initial SessionFactory creation failed." + ex);
                throw new ExceptionInInitializerError(ex);
            }
        }

        private static SessionFactory buildSessionFactoryCreate() {
            return buildSessionFactory("create");
        }

        private static SessionFactory buildSessionFactoryUpdate() {
            return buildSessionFactory("update");
        }
    }
}
