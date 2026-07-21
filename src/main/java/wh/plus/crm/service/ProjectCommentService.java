package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.project.ProjectCommentDTO;
import wh.plus.crm.mapper.ProjectCommentMapper;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.project.ProjectComment;
import wh.plus.crm.repository.ProjectCommentRepository;
import wh.plus.crm.repository.ProjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectCommentService {

    private final ProjectCommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCommentMapper mapper;

    public List<ProjectCommentDTO> list(Long projectId) {
        return commentRepository.findAllByProject_IdOrderByCreationDateDesc(projectId)
                .stream().map(mapper::toDto).toList();
    }

    public ProjectCommentDTO create(Long projectId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Komentarz nie może być pusty");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectComment comment = new ProjectComment();
        comment.setProject(project);
        comment.setContent(content.trim());
        return mapper.toDto(commentRepository.save(comment));
    }

    public void delete(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}
