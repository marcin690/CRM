package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.client.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Page<Client> findAll(Pageable pageable);

    @Query("SELECT c from Client c WHERE" +
        "(LOWER(c.clientFullName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
            "LOWER(c.clientBusinessName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
        "LOWER(c.clientEmail) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
            "CAST(c.vatNumber AS string) = :search)")
    Page<Client> searchClients(@Param("search") String search, Pageable pageable);

}
