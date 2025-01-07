package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.user.SalesTeam;
import wh.plus.crm.repository.SalesTeamRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesTeamService {

    private final SalesTeamRepository salesTeamRepository;

    public List<SalesTeam> getAllSalesTeams() {
        return salesTeamRepository.findAll();
    }


}
