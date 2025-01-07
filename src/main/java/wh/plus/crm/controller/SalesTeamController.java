package wh.plus.crm.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.model.user.SalesTeam;
import wh.plus.crm.service.SalesTeamService;

import java.util.List;

@RestController
@RequestMapping("/sales-team")
@AllArgsConstructor
public class SalesTeamController {

    private final SalesTeamService salesTeamService;

    @GetMapping
    public ResponseEntity<List<SalesTeam>> getAllSalesTeams() {
        return ResponseEntity.ok(salesTeamService.getAllSalesTeams());
    }


}
