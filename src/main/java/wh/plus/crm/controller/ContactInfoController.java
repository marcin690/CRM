package wh.plus.crm.controller;

import jakarta.persistence.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.config.auditor.AuditorAwareImpl;
import wh.plus.crm.dto.ContactInfoDTO;
import wh.plus.crm.model.contactInfo.ContactInfo;
import wh.plus.crm.repository.ContactInfoRepository;
import wh.plus.crm.service.ContactInfoService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/contact-info")
public class ContactInfoController {


    private static final Logger logger = LoggerFactory.getLogger(ContactInfoController.class);
    @Autowired
    private ContactInfoService contactInfoService;

    @PostMapping
    public ResponseEntity<ContactInfoDTO> createContactInfo(@RequestBody ContactInfoDTO contactInfoDTO) {
        ContactInfoDTO createdContactInfo = contactInfoService.createContactInfo(contactInfoDTO);
        return ResponseEntity.ok(createdContactInfo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactInfoDTO> updateContactInfo(@PathVariable Long id, @RequestBody ContactInfoDTO contactInfoDTO) {
        logger.debug("Received PUT request for id: {} with DTO: {}", id, contactInfoDTO);
        if (contactInfoDTO == null) {
            logger.error("Received null DTO");
        }
        ContactInfoDTO updatedContactInfo = contactInfoService.updateContactInfo(id, contactInfoDTO);
        return ResponseEntity.ok(updatedContactInfo);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ContactInfoDTO> getContactInfo(@PathVariable Long id) {
        ContactInfoDTO contactInfoDTO = contactInfoService.getContactInfo(id);
        return ResponseEntity.ok(contactInfoDTO);
    }

    @GetMapping
    public ResponseEntity <List<ContactInfoDTO>> getAllContactInfos() {
        List<ContactInfoDTO> contactInfosDTO = contactInfoService.getAllContactInfos();
        return ResponseEntity.ok(contactInfosDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContactInfo(@PathVariable Long id){
        contactInfoService.deleteContactInfo(id);
        return ResponseEntity.noContent().build();
    }






}
