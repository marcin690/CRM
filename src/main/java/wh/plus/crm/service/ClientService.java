package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.client.ClientDTO;
import wh.plus.crm.mapper.ClientMapper;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.repository.ClientRepository;

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
        Page<Client> clientPage = clientRepository.findAll(pageable);
        return clientPage.map(clientMapper::clientToClientDTO);
    }

    public Page<ClientDTO> searchClients(Pageable pageable, String search) {
        Page<Client> clients = clientRepository.searchClients(search, pageable);
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
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NoSuchElementException("Client not found"));

        clientMapper.updateClientFromDTO(clientDTO, client);
        client = clientRepository.save(client);
        return clientMapper.clientToClientDTO(client);
    }

    // usuniecie klienta
    public void deleteClient(Long clientId){
        if(!clientRepository.existsById(clientId)){
            throw new NoSuchElementException("Client not found");
        }
        clientRepository.deleteById(clientId);
    }




}
