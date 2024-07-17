package wh.plus.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.service.LeadService;

import java.util.List;
import java.util.Optional;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.service.LeadService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/leads")
public class LeadController {

    @Autowired
    private LeadService leadService;

    @GetMapping
    public ResponseEntity<List<Lead>> findAll() {
        List<Lead> leads = leadService.findAll();
        return new ResponseEntity<>(leads, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lead> getLeadById(@PathVariable Long id) {
        Optional<Lead> lead = leadService.findById(id);
        return lead.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Lead> createLead(@RequestBody Lead lead, Authentication authentication) {
        String username = authentication.getName();
        Lead createdLead = leadService.save(lead, username);
        return new ResponseEntity<>(createdLead, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Lead> updateLead(@PathVariable Long id, @RequestBody Lead lead) {
        Lead updatedLead = leadService.update(id, lead);
        return new ResponseEntity<>(updatedLead, HttpStatus.OK);
    }


}
