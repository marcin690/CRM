package wh.plus.crm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.contactInfo.ContactInfo;
import wh.plus.crm.repository.ContactInfoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ContactInfoService {

    @Autowired
    private ContactInfoRepository contactInfoRepository;

   public List<ContactInfo> findAll() {
       return contactInfoRepository.findAll();
   }

   public Optional<ContactInfo> findById(Long id) {
       return contactInfoRepository.findById(id);
   }

   public ContactInfo save(ContactInfo contactInfo) {
     return contactInfoRepository.save(contactInfo);
   }

    public ContactInfo update(Long id, ContactInfo contactInfo) {
        if (contactInfoRepository.existsById(id)) {
            contactInfo.setId(id);
            return contactInfoRepository.save(contactInfo);
        } else {
            return null;
        }
    }


}
