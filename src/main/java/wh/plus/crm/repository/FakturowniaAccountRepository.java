package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.invoice.FakturowniaAccount;

import java.util.List;

public interface FakturowniaAccountRepository extends JpaRepository<FakturowniaAccount, Long> {
    List<FakturowniaAccount> findAllByEnabledTrue();
}
