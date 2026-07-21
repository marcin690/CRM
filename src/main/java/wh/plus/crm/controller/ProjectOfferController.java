package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.offer.OfferSummaryDTO;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.OfferRepository;
import wh.plus.crm.repository.ProjectRepository;

import java.util.List;

/**
 * Pinowanie/odpinanie ofert do projektu.
 * Często oferta na starcie powstaje przy leadzie — gdy lead przechodzi w projekt,
 * trzeba mieć łatwy sposób przepiąć powiązane oferty.
 */
@RestController
@RequestMapping("/projects/{projectId}/offers")
@RequiredArgsConstructor
public class ProjectOfferController {

    private final ProjectRepository projectRepository;
    private final OfferRepository offerRepository;

    @GetMapping("/pinnable")
    public ResponseEntity<List<OfferSummaryDTO>> pinnable(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        if (project.getClient() == null) {
            return ResponseEntity.ok(List.of());
        }
        Long clientId = project.getClient().getId();
        List<OfferSummaryDTO> result = offerRepository
                .findPinnableForProject(projectId, clientId)
                .stream()
                .map(o -> new OfferSummaryDTO(o.getId(), o.getName(), o.getOfferStatus(), o.getTotalPrice()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{offerId}")
    @Transactional
    public ResponseEntity<Void> pin(@PathVariable Long projectId, @PathVariable Long offerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));
        offer.setProject(project);
        offerRepository.save(offer);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{offerId}")
    @Transactional
    public ResponseEntity<Void> unpin(@PathVariable Long projectId, @PathVariable Long offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));
        if (offer.getProject() != null && offer.getProject().getId().equals(projectId)) {
            offer.setProject(null);
            offerRepository.save(offer);
        }
        return ResponseEntity.noContent().build();
    }
}
