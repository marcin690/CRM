package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.offer.OfferDTO;
import wh.plus.crm.dto.offer.OfferItemDTO;
import wh.plus.crm.model.offer.OfferStatus;
import wh.plus.crm.service.OfferService;

@RestController
@RequestMapping("/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<OfferDTO>>> findAll(
            Pageable pageable,
            PagedResourcesAssembler<OfferDTO> assembler
    ) {
        // Pobieranie ofert z serwisu
        Page<OfferDTO> offers = offerService.getOffers(pageable);

        // Tworzenie modelu HATEOAS
        PagedModel<EntityModel<OfferDTO>> pagedModel = assembler.toModel(offers);

        return new ResponseEntity<>(pagedModel, HttpStatus.OK);
    }



    @GetMapping("/{id}")
    public ResponseEntity<OfferDTO> getOffersById(@PathVariable Long id){
        return ResponseEntity.ok(offerService.getOfferById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OfferDTO> updateOffer(@PathVariable Long id, @RequestBody OfferDTO offerDTO) {
        OfferDTO updatedOffer = offerService.updateOffer(id, offerDTO);
        return ResponseEntity.ok(updatedOffer);
    }

    @PostMapping
    public ResponseEntity<OfferDTO> createOffer(@RequestBody OfferDTO offerDTO){
        return ResponseEntity.ok(offerService.createOffer(offerDTO));
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateOfferStatus(@PathVariable Long id, @RequestBody OfferStatus offerStatus){
        offerService.updateOfferStatus(id, offerStatus);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id){
        offerService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }




}
