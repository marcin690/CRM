package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.offer.document.OfferDocument;

import java.util.List;

public interface OfferDocumentRepository extends JpaRepository<OfferDocument, Long> {
    List<OfferDocument> findByOffer_IdOrderByIdDesc(Long offerId);

    @org.springframework.data.jpa.repository.Query("select coalesce(max(d.version), 0) from OfferDocument d where d.offer.id = :offerId")
    int maxVersion(@org.springframework.data.repository.query.Param("offerId") Long offerId);
}
