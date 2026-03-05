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
import wh.plus.crm.dto.project.ProjectDTO;
import wh.plus.crm.service.ProjectService;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<ProjectDTO>>> findAll(
            Pageable pageable,
            PagedResourcesAssembler<ProjectDTO> assembler
    ) {
        Page<ProjectDTO> projects = projectService.getProjects(pageable);
        return new ResponseEntity<>(assembler.toModel(projects), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<ProjectDTO>>> search(
            @RequestParam String search,
            Pageable pageable,
            PagedResourcesAssembler<ProjectDTO> assembler
    ) {
        Page<ProjectDTO> projects = projectService.searchProjects(search, pageable);
        return ResponseEntity.ok(assembler.toModel(projects));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectDTO> create(@RequestBody ProjectDTO dto) {
        return ResponseEntity.ok(projectService.createProject(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDTO> update(@PathVariable Long id, @RequestBody ProjectDTO dto) {
        return ResponseEntity.ok(projectService.updateProject(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
