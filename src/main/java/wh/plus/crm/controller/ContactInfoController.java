package wh.plus.crm.controller;

import jakarta.persistence.Id;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.model.contactInfo.ContactInfo;
import wh.plus.crm.repository.ContactInfoRepository;
import wh.plus.crm.service.ContactInfoService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/contact-info")
public class ContactInfoController {


    @Autowired
    private ContactInfoService contactInfoService;

    @GetMapping
    public ResponseEntity<List<ContactInfo>> findAllContactInfo() {
        List<ContactInfo> contactInfoList = contactInfoService.findAll();
        return new ResponseEntity<>(contactInfoList, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactInfo> getContactInfoById(@PathVariable Long id) {
        Optional<ContactInfo> contactinfoOptional = contactInfoService.findById(id);
        return contactinfoOptional.map(
                value -> new ResponseEntity<>(value, HttpStatus.OK)
        ).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<ContactInfo> createContactInfo(@RequestBody ContactInfo contactInfo, Authentication authentication){
        String username = authentication.getName();
        ContactInfo createdContactInfo = contactInfoService.save(contactInfo);
        return new ResponseEntity<>(createdContactInfo, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactInfo> updateContactInfo(@PathVariable Long id, @RequestBody ContactInfo contactInfo) {
        ContactInfo updatedContactInfo = contactInfoService.update(id, contactInfo);
        if (updatedContactInfo != null) {
            return new ResponseEntity<>(updatedContactInfo, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }





}
