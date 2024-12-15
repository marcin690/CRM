package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import wh.plus.crm.dto.UserDTO;
import wh.plus.crm.model.User;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "avatar", target = "avatar")
    UserDTO userToUserDTO(User user);

    @Mapping(source = "avatar", target = "avatar")
    User userDTOtoUser(UserDTO userDTO);
}
