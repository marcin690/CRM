package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.project.ProjectDTO;
import wh.plus.crm.mapper.ProjectMapper;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.user.SalesTeam;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.repository.AdditionalCostRepository;
import wh.plus.crm.repository.ClientRepository;
import wh.plus.crm.repository.ContactRepository;
import wh.plus.crm.repository.EventRepository;
import wh.plus.crm.repository.FurnitureItemRepository;
import wh.plus.crm.repository.InvoiceRepository;
import wh.plus.crm.repository.MontageRepository;
import wh.plus.crm.repository.OrderRepository;
import wh.plus.crm.repository.ProjectCommentRepository;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.repository.ProjectSharePointFileRepository;
import wh.plus.crm.repository.SalesTeamRepository;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ClientRepository clientRepository;
    private final SalesTeamRepository salesTeamRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final MontageRepository montageRepository;
    private final AdditionalCostRepository additionalCostRepository;
    private final FurnitureItemRepository furnitureItemRepository;
    private final ProjectCommentRepository projectCommentRepository;
    private final ProjectSharePointFileRepository sharePointFileRepository;
    private final EventRepository eventRepository;
    private final ContactRepository contactRepository;

    public Page<ProjectDTO> getProjects(Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        return projectRepository.findAll(sorted).map(projectMapper::projectToProjectDTO);
    }

    public Page<ProjectDTO> searchProjects(String search, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        return projectRepository.searchProject(search, sorted).map(projectMapper::projectToProjectDTO);
    }

    public ProjectDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        return projectMapper.projectToProjectDTO(project);
    }

    public ProjectDTO createProject(ProjectDTO dto) {
        Project project = projectMapper.projectDTOToProject(dto);

        if (dto.getClient() != null && dto.getClient().getId() != null) {
            Client client = clientRepository.findById(dto.getClient().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            project.setClient(client);
        }

        if (dto.getSalesTeamId() != null) {
            SalesTeam team = salesTeamRepository.findById(dto.getSalesTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales Team not found"));
            project.setSalesTeam(team);
        }

        Project saved = projectRepository.save(project);
        return projectMapper.projectToProjectDTO(saved);
    }

    public ProjectDTO updateProject(Long id, ProjectDTO dto) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        projectMapper.updateProjectFromProjectDTO(dto, existing);

        if (dto.getClient() != null && dto.getClient().getId() != null) {
            Client client = clientRepository.findById(dto.getClient().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            existing.setClient(client);
        }

        if (dto.getSalesTeamId() != null) {
            SalesTeam team = salesTeamRepository.findById(dto.getSalesTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales Team not found"));
            existing.setSalesTeam(team);
        }

        Project saved = projectRepository.save(existing);
        return projectMapper.projectToProjectDTO(saved);
    }

    @Transactional
    public void deleteProject(Long id) {
        // Dzieci bez kaskady od strony Project — usuwamy jawnie w kolejności zależności.
        // 1. Faktury (referują wiązanie Fakturowni, które zaraz zniknie kaskadą).
        invoiceRepository.deleteAll(invoiceRepository.findAllByBinding_Project_IdOrderByIssueDateDesc(id));
        // 2. Zamówienia (+ pozycje kaskadą), montaże, koszty, meble, komentarze, pliki SharePoint.
        orderRepository.deleteAll(orderRepository.findAllByProject_IdOrderByIdDesc(id));
        montageRepository.deleteAll(montageRepository.findAllByProject_IdOrderByStartDateAsc(id));
        additionalCostRepository.deleteAll(additionalCostRepository.findAllByProject_IdOrderByCostDateDesc(id));
        furnitureItemRepository.deleteAll(furnitureItemRepository.findAllByProject_IdOrderBySortOrderAscIdAsc(id));
        projectCommentRepository.deleteAll(projectCommentRepository.findAllByProject_IdOrderByCreationDateDesc(id));
        sharePointFileRepository.deleteAll(sharePointFileRepository.findAllByProject_IdOrderByCategoryAscNameAsc(id));
        // 3. Odpięcie zdarzeń i kontaktów (zachowujemy je).
        eventRepository.detachProject(id);
        contactRepository.detachProject(id);
        // 4. Sam projekt — kaskada: etapy(+zadania), oferty, wiązania, dziennik(+komentarze), join dostawców.
        projectRepository.deleteById(id);
    }
}
