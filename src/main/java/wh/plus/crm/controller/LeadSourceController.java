package wh.plus.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.repository.LeadSourceRepository;

import java.util.List;

@RestController
@RequestMapping("/lead-source")
public class LeadSourceController {

    @Autowired
    private LeadSourceRepository leadSourceRepository;


    @GetMapping
    public List<LeadSource> getAllSources() {
        return leadSourceRepository.findAll();
    }

}
