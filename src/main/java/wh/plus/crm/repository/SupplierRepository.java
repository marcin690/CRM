package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.supplier.Supplier;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Page<Supplier> findByNameContainingIgnoreCaseOrNipContainingIgnoreCase(String name, String nip, Pageable pageable);

    /** Dopasowanie dostawcy po nazwie (do „find-or-create" przy wysyłce mebli do zamówienia). */
    Optional<Supplier> findFirstByNameIgnoreCase(String name);
}
