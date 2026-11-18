package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.model.offer.document.OfferDocument;
import wh.plus.crm.model.offer.document.OfferFaq;
import wh.plus.crm.service.offerdoc.OfferDocumentService;

import java.util.List;

/** Automatyczna oferta PDF: szkic od AI → poprawki → PDF zapisany przy ofercie. */
@RestController
@RequiredArgsConstructor
public class OfferDocumentController {

    private final OfferDocumentService service;

    @GetMapping("/offers/{offerId}/documents")
    public List<OfferDocument> list(@PathVariable Long offerId) { return service.list(offerId); }

    @PostMapping("/offers/{offerId}/documents")
    public OfferDocument draft(@PathVariable Long offerId, @RequestBody OfferDocumentService.DraftRequest req) {
        return service.draft(offerId, req);
    }

    @GetMapping("/offer-documents/{id}")
    public OfferDocument get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/offer-documents/{id}")
    public OfferDocument update(@PathVariable Long id, @RequestBody OfferDocumentService.UpdateRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/offer-documents/{id}/pdf")
    public OfferDocument pdf(@PathVariable Long id) { return service.generatePdf(id); }

    @GetMapping("/offer-faqs")
    public List<OfferFaq> faqs() { return service.faqs(); }

    @PostMapping("/offer-faqs")
    public OfferFaq createFaq(@RequestBody OfferFaq faq) { return service.saveFaq(null, faq); }

    @PutMapping("/offer-faqs/{id}")
    public OfferFaq updateFaq(@PathVariable Long id, @RequestBody OfferFaq faq) { return service.saveFaq(id, faq); }

    @DeleteMapping("/offer-faqs/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        service.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }
}
