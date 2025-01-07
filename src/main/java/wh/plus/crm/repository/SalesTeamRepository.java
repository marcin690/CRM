package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.user.SalesTeam;

public interface SalesTeamRepository extends JpaRepository<SalesTeam, Long> {
}
