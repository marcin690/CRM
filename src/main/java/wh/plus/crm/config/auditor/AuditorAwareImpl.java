//package wh.plus.crm.config.auditor;
//
//import lombok.AllArgsConstructor;
//import org.springframework.data.domain.AuditorAware;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import wh.plus.crm.model.User;
//import wh.plus.crm.repository.UserRepository;
//
//import java.util.Optional;
//
//@AllArgsConstructor
//public class AuditorAwareImpl implements AuditorAware<User> {
//
//    private static final Logger logger = LoggerFactory.getLogger(AuditorAwareImpl.class);
//    private final UserRepository userRepository;
//
//    @Override
//    public Optional<User> getCurrentAuditor() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return Optional.empty();
//        }
//        String username = authentication.getName();
//        logger.debug("Current auditor username: " + username);
//
//        if ("anonymousUser".equals(username)) {
//            return Optional.empty();
//        }
//
//        return userRepository.findByUsername(username);
//    }
//}
