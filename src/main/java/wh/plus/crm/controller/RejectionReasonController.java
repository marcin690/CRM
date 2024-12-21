package wh.plus.crm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.RejectionReasonDTO;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rejection-reasons")
public class RejectionReasonController {

    @GetMapping
    public List<RejectionReasonDTO> getRejectionReasons() {
        return Arrays.stream(wh.plus.crm.model.RejectionReason.values())
                .map(reason -> new RejectionReasonDTO(reason.name(), reason.getDescription()))
                .collect(Collectors.toList());
    }

}
