package wh.plus.crm.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import wh.plus.crm.dto.invoice.FakturowniaAccountDTO;
import wh.plus.crm.model.invoice.FakturowniaAccount;

@Mapper(componentModel = "spring")
public interface FakturowniaAccountMapper {

    /**
     * Mapowanie do DTO świadomie POMIJA {@code apiToken} — token nigdy nie
     * opuszcza serwera. Frontend dostaje tylko flagę {@code hasToken}, by
     * pokazać czy konto wymaga uzupełnienia.
     */
    @Mapping(target = "apiToken", ignore = true)
    @Mapping(target = "hasToken", expression = "java(entity.getApiToken() != null && !entity.getApiToken().isBlank())")
    FakturowniaAccountDTO toDto(FakturowniaAccount entity);

    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    FakturowniaAccount toEntity(FakturowniaAccountDTO dto);

    /**
     * Update IGNORUJE puste/null wartości — pozwala adminowi edytować konto
     * bez konieczności podawania tokenu ponownie. Jeśli pole {@code apiToken}
     * przyjdzie puste/null, dotychczasowy token zostaje nienaruszony.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    void update(FakturowniaAccountDTO dto, @MappingTarget FakturowniaAccount entity);

}
