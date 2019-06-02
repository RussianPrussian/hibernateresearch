package main;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import main.models.Author;
import main.models.AuthorAward;
import main.models.Book;

@ComponentScan(basePackages = {"main"})
@Configuration
public class Main {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		new AnnotationConfigApplicationContext(Main.class);
	}
	
	@Bean
	public SessionFactory sessionFactory() {
		org.hibernate.cfg.Configuration hibConfig = new org.hibernate.cfg.Configuration();
		hibConfig.setProperty(Environment.URL, "jdbc:postgresql://localhost:5432/library");
		hibConfig.setProperty(Environment.USER, "alex");
		hibConfig.setProperty(Environment.PASS, "alex");
		hibConfig.setProperty(Environment.DRIVER, "org.postgresql.Driver");
		hibConfig.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
		hibConfig.setProperty(Environment.POOL_SIZE, "3");
		hibConfig.addPackage("main.models");
		hibConfig.addAnnotatedClass(Author.class);
		hibConfig.addAnnotatedClass(AuthorAward.class);
		hibConfig.addAnnotatedClass(Book.class);

		return hibConfig.buildSessionFactory();
	}

}
