package wh.plus.crm.service.sharepoint;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.sharepoint.ProjectSharePointFileDTO;
import wh.plus.crm.dto.sharepoint.SharePointItemDTO;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.sharepoint.ProjectSharePointFile;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.repository.ProjectSharePointFileRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SharePointService {

    private final SharePointGraphClient graphClient;
    private final ProjectSharePointFileRepository fileRepository;
    private final ProjectRepository projectRepository;

    public boolean isConfigured() {
        return graphClient.isConfigured();
    }

    public List<SharePointItemDTO> browse(String path) {
        requireConfigured();
        return graphClient.listChildren(path);
    }

    public List<ProjectSharePointFileDTO> listProjectFiles(Long projectId) {
        return fileRepository.findAllByProject_IdOrderByCategoryAscNameAsc(projectId)
                .stream().map(this::toDto).toList();
    }

    public ProjectSharePointFileDTO attach(Long projectId, ProjectSharePointFileDTO dto) {
        if (dto.getItemId() == null || dto.getItemId().isBlank()) {
            throw new IllegalArgumentException("Brak identyfikatora pliku SharePoint");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectSharePointFile file = new ProjectSharePointFile();
        file.setProject(project);
        file.setItemId(dto.getItemId());
        file.setDriveId(dto.getDriveId());
        file.setName(dto.getName());
        file.setWebUrl(dto.getWebUrl());
        file.setMimeType(dto.getMimeType());
        file.setSize(dto.getSize());
        file.setCategory(dto.getCategory());
        return toDto(fileRepository.save(file));
    }

    public void detach(Long fileId) {
        fileRepository.deleteById(fileId);
    }

    private void requireConfigured() {
        if (!graphClient.isConfigured()) {
            throw new IllegalStateException(
                    "Integracja SharePoint nie jest skonfigurowana — ustaw SHAREPOINT_TENANT_ID, "
                            + "SHAREPOINT_CLIENT_ID, SHAREPOINT_CLIENT_SECRET i SHAREPOINT_SITE_URL.");
        }
    }

    private ProjectSharePointFileDTO toDto(ProjectSharePointFile f) {
        ProjectSharePointFileDTO dto = new ProjectSharePointFileDTO();
        dto.setId(f.getId());
        dto.setItemId(f.getItemId());
        dto.setDriveId(f.getDriveId());
        dto.setName(f.getName());
        dto.setWebUrl(f.getWebUrl());
        dto.setMimeType(f.getMimeType());
        dto.setSize(f.getSize());
        dto.setCategory(f.getCategory());
        dto.setCreatedBy(f.getCreatedBy());
        dto.setCreationDate(f.getCreationDate());
        return dto;
    }
}
