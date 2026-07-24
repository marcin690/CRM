package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.crew.CrewMember;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {
}
