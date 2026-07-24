package wh.plus.crm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.dto.ContactDTO;
import wh.plus.crm.model.Contact;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    Page<Contact> findAll(Pageable pageable);

    List<Contact> findAllByClientId(Long clientId);

    /** Odpięcie kontaktów od usuwanego projektu. */
    @Modifying
    @Query("update Contact c set c.project = null where c.project.id = :projectId")
    void detachProject(@Param("projectId") Long projectId);
}
