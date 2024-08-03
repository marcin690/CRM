package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wh.plus.crm.config.auditor.AuditorAwareImpl;
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

    private static final Logger logger = LoggerFactory.getLogger(ContactInfoService.class);

    @Transactional
    public ContactInfoDTO createContactInfo(ContactInfoDTO contactInfoDTO) {
        ContactInfo contactInfo = contactInfoMapper.toEntity(contactInfoDTO);
        ContactInfo savedContactInfo = contactInfoRepository.save(contactInfo);
        return contactInfoMapper.toDTO(savedContactInfo);
    }


    @Transactional
    public ContactInfoDTO updateContactInfo(Long id, ContactInfoDTO contactInfoDTO) {
        logger.debug("Starting updateContactInfo for id: {}", id);

        // Log the incoming DTO
        logger.debug("Incoming DTO: {}", contactInfoDTO);

        ContactInfo contactInfo = contactInfoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact info not found for id: " + id));

        // Log the existing entity before update
        logger.debug("Existing entity before update: {}", contactInfo);
//
//        // Ręczna aktualizacja pól
//        contactInfo.setFullName(contactInfoDTO.getFullName());
//        contactInfo.setClientBusinessName(contactInfoDTO.getClientBusinessName());
//        contactInfo.setClientAdress(contactInfoDTO.getClientAdress());
//        contactInfo.setClientCity(contactInfoDTO.getClientCity());
//        contactInfo.setClientState(contactInfoDTO.getClientState());
//        contactInfo.setClientZip(contactInfoDTO.getClientZip());
//        contactInfo.setClientCountry(contactInfoDTO.getClientCountry());
//        contactInfo.setClientEmail(contactInfoDTO.getClientEmail());
//        contactInfo.setClientPhone(contactInfoDTO.getClientPhone());
//        contactInfo.setVatNumber(contactInfoDTO.getVatNumber());
//        contactInfo.setClient(contactInfoDTO.isClient());

            contactInfoMapper.updateEntity(contactInfoDTO, contactInfo);

        // Log the entity after update
        logger.debug("Entity after manual update: {}", contactInfo);

        ContactInfo updatedContactInfo = contactInfoRepository.save(contactInfo);

        // Log the saved entity
        logger.debug("Entity after save: {}", updatedContactInfo);

        ContactInfoDTO updatedContactInfoDTO = contactInfoMapper.toDTO(updatedContactInfo);

        // Log the outgoing DTO
        logger.debug("Outgoing DTO: {}", updatedContactInfoDTO);

        return updatedContactInfoDTO;
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
