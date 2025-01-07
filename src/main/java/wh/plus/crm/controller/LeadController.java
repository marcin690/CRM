package wh.plus.crm.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.client.ClientDTO;
import wh.plus.crm.dto.lead.LeadDTO;
import wh.plus.crm.mapper.ClientMapper;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.service.LeadService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



@RestController
@RequestMapping("/leads")
@AllArgsConstructor
public class LeadController {

    @Autowired
    private LeadService leadService;
    private final ClientMapper clientMapper;

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<LeadDTO>>> findAll(
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime toDate,
            @RequestParam(required = false) String employee,
            @RequestParam(required = false) Long status,
            @RequestParam(required = false) String search,
            PagedResourcesAssembler<LeadDTO> assembler
    ) {
        Page<LeadDTO> leads = leadService.getLeads(pageable, fromDate, toDate, employee, status, search);
        PagedModel<EntityModel<LeadDTO>> pagedModel = assembler.toModel(leads);
        return new ResponseEntity<>(pagedModel, HttpStatus.OK);
    }


    @GetMapping("/{id}")
    public ResponseEntity<LeadDTO> getLeadById(@PathVariable Long id) {
        Optional<LeadDTO> lead = leadService.findById(id);
        return lead.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<LeadDTO> createLead(@RequestBody LeadDTO leadDTO, Authentication authentication) {
        String username = authentication.getName();
        LeadDTO createdLead = leadService.save(leadDTO);
        return new ResponseEntity<>(createdLead, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<LeadDTO> updateLead(@PathVariable Long id, @RequestBody LeadDTO lead) {
        lead.setId(id); // Ustaw ID w LeadDTO
        LeadDTO updatedLead = leadService.update(id, lead); // Użyj metody update do aktualizacji
        return new ResponseEntity<>(updatedLead, HttpStatus.OK);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<LeadDTO> deleteLead(@RequestBody List<Long> ids){
        leadService.deleteLead(ids);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{leadId}/convert")
    public ResponseEntity<ClientDTO> convertLeadToClient(@PathVariable Long leadId) {
        Client client = leadService.convertLeadToClient(leadId); // Wywołanie metody z serwisu
        ClientDTO clientDTO = clientMapper.clientToClientDTO(client);
        return ResponseEntity.ok(clientDTO);
    }

}
