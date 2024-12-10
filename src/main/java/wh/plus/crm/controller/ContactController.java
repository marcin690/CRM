package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.ContactDTO;
import wh.plus.crm.model.Contact;
import wh.plus.crm.service.ContactService;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping("/client/{clientId}")
    public List<ContactDTO> getContacts(@PathVariable Long clientId){
        return contactService.getContactsByClientId(clientId);
    }

}
