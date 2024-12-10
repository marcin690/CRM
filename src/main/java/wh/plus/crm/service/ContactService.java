package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.ContactDTO;
import wh.plus.crm.mapper.ContactMapper;
import wh.plus.crm.model.Contact;
import wh.plus.crm.repository.ContactRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final ContactMapper contactMapper;

    /**
     * Pobiera listę wszystkich kontaktów powiązanych z danym klientem.
     *
     * @param clientId ID klienta
     * @return Lista obiektów ContactDTO
     */
    public List<ContactDTO> getContactsByClientId(Long clientId) {
        List<Contact> contacts = contactRepository.findAllByClientId(clientId);

        if (contacts.isEmpty()) {
            throw new NoSuchElementException("No contacts found for client ID: " + clientId);
        }

        return contacts.stream()
                .map(contactMapper::contactToContactDTO)
                .collect(Collectors.toList());
    }


}
