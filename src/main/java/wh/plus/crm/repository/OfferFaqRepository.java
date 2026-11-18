package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.offer.document.OfferFaq;

import java.util.List;

public interface OfferFaqRepository extends JpaRepository<OfferFaq, Long> {
    List<OfferFaq> findAllByOrderByPositionAscIdAsc();
}
