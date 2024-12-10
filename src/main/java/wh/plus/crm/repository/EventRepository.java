package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

   List<Event> findByClientId(Long clientId);

   List<Event> findByProjectId(Long projectId);


}
