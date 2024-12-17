package wh.plus.crm.mapper;

import org.mapstruct.*;
import wh.plus.crm.dto.offer.OfferDTO;
import wh.plus.crm.dto.offer.OfferItemDTO;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.offer.OfferItem;

import java.util.List;

@Mapper(componentModel = "spring",uses = {UserMapper.class, ProjectMapper.class, LeadMapper.class, ClientMapper.class})
public interface OfferMapper {

    @Mapping(target = "offerItemList", source = "offerItems")
    @Mapping(target = "project", source = "project")
    @Mapping(target = "lead", source = "lead")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "euroExchangeRate", source = "euroExchangeRate") // Mapowanie kursu euro
    @Mapping(target = "clientGlobalId", source = "clientGlobalId")
    Offer toEntity(OfferDTO offerDTO);

    @Mapping(target = "offerItems", source = "offerItemList")
    @Mapping(target = "project", source = "project")
    @Mapping(target = "lead", source = "lead")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "client", target = "client")
    @Mapping(target = "euroExchangeRate", source = "euroExchangeRate") // Mapowanie kursu euro
    @Mapping(source = "clientGlobalId", target = "clientGlobalId")
    @Mapping(target = "rejectionOrApprovalDate", source = "rejectionOrApprovalDate")
    @Mapping(target = "signedContractDate", source = "signedContractDate")
    OfferDTO toOfferDTO(Offer offer);

    @Mapping(target = "offer", ignore = true)
    OfferItem toEntity(OfferItemDTO offerItemDTO);

    OfferItemDTO toOfferItemDTO(OfferItem offerItem);

    @AfterMapping
    default void setOfferInOfferItems(@MappingTarget Offer offer) {
        if (offer.getOfferItemList() != null) {
            for (OfferItem offerItem : offer.getOfferItemList()) {
                offerItem.setOffer(offer);
            }
   }
    }

    void updateEntityFromDTO(OfferDTO offerDTO, @MappingTarget Offer offer);
}
