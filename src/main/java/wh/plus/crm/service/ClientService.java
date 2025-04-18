package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.client.ClientDTO;
import wh.plus.crm.helper.NullPropertyUtils;
import wh.plus.crm.mapper.ClientMapper;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.repository.ClientRepository;
import wh.plus.crm.specyfications.ClientSpecification;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    // Pobranie konkretnego klienta
    public ClientDTO getClientWithProjectsAndOffers(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NoSuchElementException("Client not found"));

        return clientMapper.clientToClientDTO(client);
    }

    // pobranie wszystkich klientów wraz z paginacja
    public Page<ClientDTO> getClients(Pageable pageable){
       Pageable sortedPageable = PageRequest.of(
               pageable.getPageNumber(),
               pageable.getPageSize(),
               Sort.by(Sort.Direction.DESC, "id")
       );

       return clientRepository.findAll(sortedPageable).map(clientMapper::clientToClientDTO);
    }

    public Page<ClientDTO> searchClients(String fullName, String businessName, String email, Long phone, Long vatNumber, String searchLike, Pageable pageable) {
        Specification<Client> spec = Specification.where(null);

        if (fullName != null && !fullName.isEmpty()) {
            spec = spec.and(ClientSpecification.hasClientFullName(fullName));
        }
        if (businessName != null && !businessName.isEmpty()) {
            spec = spec.and(ClientSpecification.hasClientBusinessName(businessName));
        }
        if (email != null && !email.isEmpty()) {
            spec = spec.and(ClientSpecification.hasClientEmail(email));
        }
        if (phone != null) {
            spec = spec.and(ClientSpecification.hasClientPhone(phone));
        }
        if (vatNumber != null) {
            spec = spec.and(ClientSpecification.hasVatNumber(vatNumber));
        }

        if(searchLike != null) {
            spec = spec.and(ClientSpecification.hasNameOrBusinnesLike(searchLike));
        }

        Page<Client> clients = clientRepository.findAll(spec, pageable);
        return clients.map(clientMapper::clientToClientDTO);

    }

    // dodanie klienta
    public ClientDTO addClient(ClientDTO clientDTO) {
        Client client = clientMapper.clientDTOToClient(clientDTO);
        Client savedClient = clientRepository.save(client);
        return clientMapper.clientToClientDTO(savedClient);
    }

    // zaktualizowanie klienta
    public ClientDTO updateClient(Long clientId, ClientDTO clientDTO) {
        Client existingClient = clientRepository.findById(clientId)
                .orElseThrow(() -> new NoSuchElementException("Client not found"));

        List<Offer> existingOffers = existingClient.getOffers();


        String[] nullPropertyNames = NullPropertyUtils.getNullPropertyNames(clientDTO);
        BeanUtils.copyProperties(clientDTO, existingClient, nullPropertyNames);

        existingClient.setOffers(existingOffers);


        clientDTO.setOffers(clientMapper.mapOffersToSummary(existingClient.getOffers()));


        clientMapper.updateClientFromDTO(clientDTO, existingClient);
        existingClient = clientRepository.save(existingClient);
        return clientMapper.clientToClientDTO(existingClient);
    }

    // usuniecie klienta
    public void deleteClient(Long clientId){
        if(!clientRepository.existsById(clientId)){
            throw new NoSuchElementException("Client not found");
        }
        clientRepository.deleteById(clientId);
    }




}
