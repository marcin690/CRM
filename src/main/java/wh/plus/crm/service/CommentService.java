package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.CommentDTO;
import wh.plus.crm.mapper.CommentMapper;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.comment.Comment;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.repository.ClientRepository;
import wh.plus.crm.repository.CommentRepository;
import wh.plus.crm.repository.LeadRepository;
import wh.plus.crm.repository.OfferRepository;

import java.util.List;
import java.util.Locale;
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
    @Autowired
    private ClientRepository clientRepository;
    private OfferRepository offerRepository;

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

    public Page<CommentDTO> findByClientGlobalId(String clientGlobalId, Pageable pageable){
        Page<Comment> commentPage = commentRepository.findByClientGlobalId(clientGlobalId, pageable);
        return commentPage.map(commentMapper::toDTO);
    }

    @Transactional
    public CommentDTO addComments(CommentDTO commentDTO){
        Comment comment = commentMapper.toEntity(commentDTO);

        // Licznik powiązanych encji
        int linkedEntities = 0;

        // Ustaw relację z Lead
        if (commentDTO.getLeadId() != null) {
            Lead lead = leadRepository.findById(commentDTO.getLeadId())
                    .orElseThrow(() -> new IllegalArgumentException("Lead with ID " + commentDTO.getLeadId() + " not found"));
            comment.setLead(lead);

        }

        // Ustaw relację z Client
        if (commentDTO.getClientId() != null) {
            Client client = clientRepository.findById(commentDTO.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            comment.setClient(client);

        }

        // Ustaw relację z Offer
        if (commentDTO.getOfferId() != null) {
            Offer offer = offerRepository.findById(commentDTO.getOfferId())
                    .orElseThrow(() -> new IllegalArgumentException("Offer with ID " + commentDTO.getOfferId() + " not found"));
            comment.setOffer(offer);

        }



        // Zapisz komentarz
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


    public Long countCommentsByEntity(String entityType, Long entityId) {
        switch (entityType.toUpperCase()) {
            case "CLIENT":
                return commentRepository.countByClientId(entityId);
            case "OFFER":
                return commentRepository.countByOfferId(entityId);
            case "LEAD":
                return commentRepository.countByLeadId(entityId);
            case "PROJECT":
                return commentRepository.countByProjectId(entityId);
            default:
                throw new IllegalArgumentException("Nieprawidłowy typ encji: " + entityType);


        }
    }
}
