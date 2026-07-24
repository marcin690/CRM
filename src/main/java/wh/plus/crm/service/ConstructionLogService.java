package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.project.ConstructionLogCommentDTO;
import wh.plus.crm.dto.project.ConstructionLogEntryDTO;
import wh.plus.crm.mapper.ConstructionLogEntryMapper;
import wh.plus.crm.model.project.ConstructionLogComment;
import wh.plus.crm.model.project.ConstructionLogEntry;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.project.ProjectStage;
import wh.plus.crm.repository.ConstructionLogCommentRepository;
import wh.plus.crm.repository.ConstructionLogEntryRepository;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.repository.ProjectStageRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConstructionLogService {

    private final ConstructionLogEntryRepository entryRepository;
    private final ConstructionLogCommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStageRepository projectStageRepository;
    private final ConstructionLogEntryMapper mapper;

    public ConstructionLogCommentDTO addComment(Long entryId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Komentarz nie może być pusty");
        }
        ConstructionLogEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));
        ConstructionLogComment comment = new ConstructionLogComment();
        comment.setEntry(entry);
        comment.setContent(content.trim());
        return mapper.toDto(commentRepository.save(comment));
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    public List<ConstructionLogEntryDTO> list(Long projectId) {
        return entryRepository.findAllByProject_IdOrderByEntryDateDesc(projectId)
                .stream().map(mapper::toDto).toList();
    }

    public ConstructionLogEntryDTO create(Long projectId, ConstructionLogEntryDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ConstructionLogEntry entry = mapper.toEntity(dto);
        entry.setProject(project);
        if (dto.getProjectStageId() != null) {
            ProjectStage stage = projectStageRepository.findById(dto.getProjectStageId())
                    .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + dto.getProjectStageId()));
            entry.setProjectStage(stage);
        }
        return mapper.toDto(entryRepository.save(entry));
    }

    public ConstructionLogEntryDTO update(Long entryId, ConstructionLogEntryDTO dto) {
        ConstructionLogEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));
        mapper.update(dto, entry);
        if (dto.getProjectStageId() != null) {
            ProjectStage stage = projectStageRepository.findById(dto.getProjectStageId())
                    .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + dto.getProjectStageId()));
            entry.setProjectStage(stage);
        }
        return mapper.toDto(entryRepository.save(entry));
    }

    public void delete(Long entryId) {
        entryRepository.deleteById(entryId);
    }

    public byte[] exportToExcel(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        List<ConstructionLogEntry> entries = entryRepository.findAllByProject_IdOrderByEntryDateDesc(projectId);
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Dziennik budowy");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"Data", "Tytuł", "Zakres", "Treść", "Sentyment", "Etap", "Autor", "Załączniki"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int r = 1;
            for (ConstructionLogEntry e : entries) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(e.getEntryDate() != null ? e.getEntryDate().format(df) : "");
                row.createCell(1).setCellValue(nullToEmpty(e.getTitle()));
                row.createCell(2).setCellValue(nullToEmpty(e.getScope()));
                row.createCell(3).setCellValue(nullToEmpty(e.getComments()));
                row.createCell(4).setCellValue(e.getCommentSentiment() != null ? e.getCommentSentiment().name() : "");
                row.createCell(5).setCellValue(e.getProjectStage() != null ? nullToEmpty(e.getProjectStage().getName()) : "");
                row.createCell(6).setCellValue(nullToEmpty(e.getCreatedBy()));
                row.createCell(7).setCellValue(String.join(" | ", e.getAttachmentUrls()));
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
