package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.invoice.FakturowniaAccount;
import wh.plus.crm.model.invoice.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByFakturowniaIdAndAccount(Long fakturowniaId, FakturowniaAccount account);

    List<Invoice> findAllByBinding_Project_IdOrderByIssueDateDesc(Long projectId);

    List<Invoice> findAllByBinding_Id(Long bindingId);
}
