package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.offer.document.OfferDocument;

import java.util.List;

public interface OfferDocumentRepository extends JpaRepository<OfferDocument, Long> {
    List<OfferDocument> findByOffer_IdOrderByIdDesc(Long offerId);
}
