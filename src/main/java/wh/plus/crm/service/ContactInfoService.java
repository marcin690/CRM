package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.ContactInfoDTO;
import wh.plus.crm.mapper.ContactInfoMapper;
import wh.plus.crm.model.contactInfo.ContactInfo;
import wh.plus.crm.repository.ContactInfoRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ContactInfoService {

    @Autowired
    private ContactInfoRepository contactInfoRepository;

    @Autowired
    private ContactInfoMapper contactInfoMapper;

    @Transactional
    public ContactInfoDTO createContactInfo(ContactInfoDTO contactInfoDTO) {
        ContactInfo contactInfo = contactInfoMapper.toEntity(contactInfoDTO);
        ContactInfo savedContactInfo = contactInfoRepository.save(contactInfo);
        return contactInfoMapper.toDTO(savedContactInfo);
    }

    @Transactional
    public ContactInfoDTO updateContactInfo(Long id, ContactInfoDTO contactInfoDTO){
        ContactInfo contactInfo = contactInfoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact info not found"));

        contactInfoMapper.updateEntity(contactInfoDTO, contactInfo); // Poprawiona linia
        ContactInfo updatedContactInfo = contactInfoRepository.save(contactInfo);
        return contactInfoMapper.toDTO(updatedContactInfo);
    }

    @Transactional
    public ContactInfoDTO getContactInfo(Long id) {
        ContactInfo contactInfo = contactInfoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact Info not found"));
        return contactInfoMapper.toDTO(contactInfo);
    }

    @Transactional
    public List<ContactInfoDTO> getAllContactInfos() {
        List<ContactInfo> contactInfos = contactInfoRepository.findAll();
        return contactInfos.stream()
                .map(contactInfoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteContactInfo(Long id){
        ContactInfo contactInfo = contactInfoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact Info not found"));
        contactInfoRepository.delete(contactInfo);
    }
}
