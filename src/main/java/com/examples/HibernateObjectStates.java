package com.examples;

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
 * Created by ka40215 on 11/26/15.
 */
public class HibernateObjectStates {
    public static class HibernateObjectStatesTest {
        @Test
        public void detachedToPersistentTest() {
            Session s1 = HibernateUtil.buildSessionFactoryCreate().openSession();
            Transaction t1 = s1.beginTransaction();
            User u1 = new User();
            s1.save(u1);
            t1.commit();
            s1.close();

            // u1 detached
            u1.setName("detached");


            Session s2 = HibernateUtil.buildSessionFactoryUpdate().openSession();
            Transaction t2 = s2.beginTransaction();

            User u1_get = (User) s2.get(User.class, 1);

            Assert.assertEquals("user name is not null", null, u1_get.getName());
            s2.merge(u1); // merging state of a detached object
            t2.commit();

            u1_get = (User) s2.get(User.class, 1);

            Assert.assertEquals("user name is not detached", "detached", u1_get.getName());
            s2.close();
        }

        @Test
        public void testObjectStates() {
            Session s1 = HibernateUtil.buildSessionFactoryCreate().openSession();
            Transaction t1 = s1.beginTransaction();
            User u1 = new User(); // user is now transient
            User u2 = new User();
            s1.save(u1);
            s1.saveOrUpdate(u2);

            t1.commit(); // Now user is a persistent instance, which has a database identity - has a primary key
            s1.close(); // u1 and u2 are detached

            System.out.println(u1.getUserId());
            Session s2 = HibernateUtil.buildSessionFactoryUpdate().openSession();
            Transaction t2 = s2.beginTransaction();
            User u3 = (User) s2.get(User.class, 1); // u3 is persistent
            s2.delete(u3);
            t2.commit(); // u3 is removed
            s2.close();

            System.out.println(u3.getUserId());
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
