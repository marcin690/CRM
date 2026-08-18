package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.aivaluation.AiAgent;

import java.util.List;
import java.util.Optional;

public interface AiAgentRepository extends JpaRepository<AiAgent, Long> {
    Optional<AiAgent> findByCode(String code);
    List<AiAgent> findByActiveTrueOrderByNameAsc();
}
