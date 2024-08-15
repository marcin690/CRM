package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.CommentDTO;
import wh.plus.crm.mapper.CommentMapper;
import wh.plus.crm.model.comment.Comment;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.repository.CommentRepository;
import wh.plus.crm.repository.LeadRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private CommentMapper commentMapper;

    @Transactional
    public List<CommentDTO> findCommentsAll() {
        return commentRepository.findAll().stream().map(commentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDTO findCommentById(Long id){
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Comments not found"));
        return commentMapper.toDTO(comment);
    }

    @Transactional
    public CommentDTO addComments(CommentDTO commentDTO){
        Comment comment = commentMapper.toEntity(commentDTO);

        if (commentDTO.getLeadId() != null) {
            Optional<Lead> lead = leadRepository.findById(commentDTO.getLeadId());
            lead.ifPresent(comment::setLead);
        }

        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toDTO(savedComment);
    }

    @Transactional
    public CommentDTO updateComment(Long id, CommentDTO commentDTO){
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));

        if (commentDTO.getLeadId() != null) {
            Optional<Lead> lead = leadRepository.findById(commentDTO.getLeadId());
            lead.ifPresent(comment::setLead);
        }

        commentMapper.updateEntity(commentDTO, comment);
        Comment updatedComment = commentRepository.save(comment);
        CommentDTO updatedCommentDTO = commentMapper.toDTO(updatedComment);
        return updatedCommentDTO;

    }

    @Transactional
    public void deleteComment(Long id){
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));
        commentRepository.delete(comment);
    }



}
