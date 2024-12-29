package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.offer.OfferDTO;
import wh.plus.crm.dto.offer.OfferItemDTO;
import wh.plus.crm.model.offer.*;
import wh.plus.crm.service.OfferService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<OfferDTO>>> findAll(
            @RequestParam(defaultValue = "id,desc") String sort,
            Pageable pageable,
            PagedResourcesAssembler<OfferDTO> assembler
    ) {


        // Pobieranie ofert z serwisu
        Page<OfferDTO> offers = offerService.getOffers(pageable);

        // Tworzenie modelu HATEOAS
        PagedModel<EntityModel<OfferDTO>> pagedModel = assembler.toModel(offers);

        return new ResponseEntity<>(pagedModel, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<OfferDTO>>> searchOffers(
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) ClientType clientType,
            @RequestParam(required = false) InvestorType investorType,
            @RequestParam(required = false) OfferStatus offerStatus,
            @RequestParam(required = false) ObjectType objectType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) SalesOpportunityLevel salesOpportunityLevel,
            Pageable pageable,
            PagedResourcesAssembler<OfferDTO> assembler
    ) {

        Page<OfferDTO> offers = offerService.searchOffers(
                 createdBy,  name,  clientType,  investorType,  offerStatus,  objectType,  description,  userId,  clientId,  leadId,  projectId,
                 startDate,  endDate,  salesOpportunityLevel, pageable
        );

        // Tworzenie modelu HATEOAS
        PagedModel<EntityModel<OfferDTO>> pagedModel = assembler.toModel(offers);

        return ResponseEntity.ok(pagedModel);
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
