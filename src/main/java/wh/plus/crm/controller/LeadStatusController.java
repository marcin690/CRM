package wh.plus.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.repository.LeadStatusRepository;

import java.util.List;

@RestController
@RequestMapping("/lead-status")
public class LeadStatusController {

    @Autowired
    private LeadStatusRepository leadStatusRepository;

    @GetMapping
    public List<LeadStatus> getAllStatuses() {
        return leadStatusRepository.findAll();
    }

    @PostMapping
    public LeadStatus createLeadStatus(@RequestBody LeadStatus leadStatus) {
        return leadStatusRepository.save(leadStatus);
    }

    @PutMapping("/{id}")
    public LeadStatus updateLeadStatus(@PathVariable Long id, @RequestBody LeadStatus leadStatus) {
        return leadStatusRepository.findById(id).map(status -> {
            status.setStatusName(leadStatus.getStatusName());
            return leadStatusRepository.save(status);
        }).orElseThrow(() -> new RuntimeException("Lead Status Not Found"));
    }




}
