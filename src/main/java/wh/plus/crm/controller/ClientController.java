package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
    public ResponseEntity<PagedModel<EntityModel<ClientDTO>>> getClients(
            Pageable pageable,
            @RequestParam(required = false) String search,
            PagedResourcesAssembler<ClientDTO> assembler
    ) {
        Page<ClientDTO> clientsPage;
        if(search != null && !search.isEmpty()) {
            clientsPage = clientService.searchClients(pageable, search);
        } else {
            clientsPage = clientService.getClients(pageable);
        }
        PagedModel<EntityModel<ClientDTO>> pagedModel = assembler.toModel(clientsPage);
        return new ResponseEntity<>(pagedModel, HttpStatus.OK);
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
