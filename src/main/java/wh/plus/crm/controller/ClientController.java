package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.client.ClientDTO;
import wh.plus.crm.service.ClientService;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<Page<ClientDTO>> getClients(
            Pageable pageable,
            @RequestParam(required = false) String search
    ) {
        Page<ClientDTO> clients;
        if(search != null && !search.isEmpty()) {
            clients = clientService.searchClients(pageable, search);
        } else {
            clients  = clientService.getClients(pageable);
        }
        return ResponseEntity.ok(clients);
    }


    @GetMapping("/{clientId}")
    public ClientDTO getClientWithProjectsAndOffers(@PathVariable Long clientId){
        return clientService.getClientWithProjectsAndOffers(clientId);
    }

    @PostMapping
    public ClientDTO createClient(@RequestBody ClientDTO clientDTO) {
        return clientService.addClient(clientDTO);
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable Long clientId, @RequestBody ClientDTO clientDTO) {
        ClientDTO updatedClient = clientService.updateClient(clientId, clientDTO);
        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping
    public ResponseEntity<ClientDTO> deleteClient(@PathVariable Long clientId){
        clientService.deleteClient(clientId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
