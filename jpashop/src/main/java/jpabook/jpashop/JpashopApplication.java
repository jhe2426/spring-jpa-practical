package jpabook.jpashop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tools.jackson.datatype.hibernate7.Hibernate7Module;

@SpringBootApplication
public class JpashopApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpashopApplication.class, args);
	}

    @Bean
    Hibernate7Module hibernate7Module() {
        Hibernate7Module hibernate7Module = new Hibernate7Module();
//        hibernate7Module.configure(Hibernate7Module.Feature.FORCE_LAZY_LOADING, true);
        return hibernate7Module;
    }
}
