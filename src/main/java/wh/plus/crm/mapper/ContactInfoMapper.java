package wh.plus.crm.mapper;


import org.mapstruct.*;
import wh.plus.crm.dto.ContactInfoDTO;
import wh.plus.crm.model.contactInfo.ContactInfo;



@Mapper(componentModel = "spring")
public interface ContactInfoMapper {

    ContactInfoDTO toDTO(ContactInfo contactInfo);

    @Mapping(target = "id", ignore = true)
    ContactInfo toEntity(ContactInfoDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntity(ContactInfoDTO dto, @MappingTarget ContactInfo entity);
}
