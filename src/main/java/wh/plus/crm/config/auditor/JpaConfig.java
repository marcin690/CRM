//package wh.plus.crm.config.auditor;
//
//import lombok.AllArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
//
//import wh.plus.crm.repository.UserRepository;
//
//@Configuration
//@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
//@AllArgsConstructor
//public class JpaConfig {
//
//
//
//
//    @Bean
//    public AuditorAwareImpl auditorProvider(UserRepository userRepository) {
//        return new AuditorAwareImpl(userRepository);
//    }
//
//
//}