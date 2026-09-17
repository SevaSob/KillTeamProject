package ru.petrsu.killteam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@SpringBootApplication
public class KillTeamApplication {

    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 5432;
    private static final String DB_NAME = "killteam";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "12345";

    public static void main(String[] args) {
        ensureDatabase();
        SpringApplication.run(KillTeamApplication.class, args);
    }

    /**
     * Проверяет, существует ли база данных killteam. Если нет — создаёт.
     * Подключается к служебной базе postgres, потому что свою БД создать нельзя из неё самой.
     */
    private static void ensureDatabase() {
        String adminUrl = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/postgres";
        try (Connection conn = DriverManager.getConnection(adminUrl, DB_USER, DB_PASS)) {
            try (Statement st = conn.createStatement()) {
                ResultSet rs = st.executeQuery(
                        "SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'"
                );
                if (!rs.next()) {
                    st.execute("CREATE DATABASE " + DB_NAME);
                    System.out.println(">>> База данных '" + DB_NAME + "' создана.");
                } else {
                    System.out.println(">>> База данных '" + DB_NAME + "' уже существует.");
                }
            }
        } catch (Exception e) {
            System.err.println(">>> Не удалось проверить/создать БД: " + e.getMessage());
            System.err.println(">>> Убедись, что PostgreSQL запущен и пароль от 'postgres' верный.");
        }
    }
}