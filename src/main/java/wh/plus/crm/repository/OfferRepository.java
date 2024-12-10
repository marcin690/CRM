package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.offer.Offer;

import java.util.List;


public interface OfferRepository extends JpaRepository<Offer, Long> {

    Page<Offer> findAll(Pageable pageable);

    Page<Offer> findByClientId(Long clientId, Pageable pageable);




}
