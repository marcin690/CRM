package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.crew.Crew;

import java.util.List;

public interface CrewRepository extends JpaRepository<Crew, Long> {
    List<Crew> findAllByOrderByNameAsc();
}
