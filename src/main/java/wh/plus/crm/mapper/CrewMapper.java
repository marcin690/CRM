package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import wh.plus.crm.dto.crew.CrewDTO;
import wh.plus.crm.dto.crew.CrewMemberDTO;
import wh.plus.crm.model.crew.Crew;
import wh.plus.crm.model.crew.CrewMember;

@Mapper(componentModel = "spring")
public interface CrewMapper {

    CrewDTO toDto(Crew entity);

    CrewMemberDTO toMemberDto(CrewMember member);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "members", ignore = true)
    void update(CrewDTO dto, @MappingTarget Crew entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "crew", ignore = true)
    void updateMember(CrewMemberDTO dto, @MappingTarget CrewMember entity);
}
