package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

   List<Event> findByClientId(Long clientId);

   List<Event> findByProjectId(Long projectId);

   @Query("SELECT e FROM Event e WHERE e.client.id = :clientId OR e.project.id = :projectId")
   Page<Event> findByClientOrProject(@Param("clientId") Long clientId, @Param("projectId") Long projectId, Pageable pageable);

   @Query("SELECT e FROM Event e " +
           "WHERE (e.date BETWEEN :from AND :to) OR e.cycleType != 'NONE'")
   List<Event> findEventsInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);



}
