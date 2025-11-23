package opensource.project;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // 애플리케이션 시작 시 데이터베이스 연결 테스트
    @Bean
    CommandLineRunner testConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("[SUCCESS] Oracle RDS 연결 성공");
                System.out.println("Database: " + connection.getMetaData().getDatabaseProductName());
            } catch (Exception e) {
                System.err.println("[FAILED] 연결 실패: " + e.getMessage());
            }
        };
    }
}
