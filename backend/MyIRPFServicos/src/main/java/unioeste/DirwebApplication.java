package unioeste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "unioeste")
public class DirwebApplication {
    public static void main(String[] args) {
        SpringApplication.run(DirwebApplication.class, args);
    }
}