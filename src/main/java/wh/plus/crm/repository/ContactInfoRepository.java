package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.contactInfo.ContactInfo;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
}
