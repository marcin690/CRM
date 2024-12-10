package wh.plus.crm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.dto.ContactDTO;
import wh.plus.crm.model.Contact;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    Page<Contact> findAll(Pageable pageable);

    List<Contact> findAllByClientId(Long clientId);
}
