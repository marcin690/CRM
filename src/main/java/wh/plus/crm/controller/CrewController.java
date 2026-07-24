package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.crew.CrewDTO;
import wh.plus.crm.dto.crew.CrewMemberDTO;
import wh.plus.crm.service.CrewService;

import java.util.List;

/** Ekipy monterskie — poziom aplikacji. */
@RestController
@RequestMapping("/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService service;

    @GetMapping
    public ResponseEntity<List<CrewDTO>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<CrewDTO> create(@RequestBody CrewDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CrewDTO> update(@PathVariable Long id, @RequestBody CrewDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{crewId}/members")
    public ResponseEntity<CrewMemberDTO> addMember(@PathVariable Long crewId, @RequestBody CrewMemberDTO dto) {
        return ResponseEntity.ok(service.addMember(crewId, dto));
    }

    @DeleteMapping("/{crewId}/members/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long crewId, @PathVariable Long memberId) {
        service.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }
}
