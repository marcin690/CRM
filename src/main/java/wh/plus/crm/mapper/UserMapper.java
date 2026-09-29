package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import wh.plus.crm.dto.UserDTO;
import wh.plus.crm.model.user.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Pola RBAC (roles/team/permissions/isSalesRepresentative) uzupełniane ręcznie w UserService.
    // password NIGDY nie trafia do DTO wyjściowego (żeby nie wyciekł hash).
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "teamName", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "isSalesRepresentative", ignore = true)
    @Mapping(target = "password", ignore = true)
    UserDTO userToUserDTO(User user);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    User userDTOtoUser(UserDTO userDTO);
}
