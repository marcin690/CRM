package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import wh.plus.crm.dto.UserDTO;
import wh.plus.crm.model.User;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO userToUserDTO(User user);
    User userDTOtoUser(UserDTO userDTO);
}
