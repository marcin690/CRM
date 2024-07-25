package wh.plus.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.LeadDTO;
import wh.plus.crm.service.LeadService;
import java.util.List;
import java.util.Optional;



@RestController
@RequestMapping("/leads")
public class LeadController {

    @Autowired
    private LeadService leadService;

    @GetMapping
    public ResponseEntity<List<LeadDTO>> findAll() {
        List<LeadDTO> leads = leadService.findAll();
        return new ResponseEntity<>(leads, HttpStatus.OK);
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

}
