package wh.plus.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.CommentDTO;
import wh.plus.crm.service.CommentService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentDTO>> findAll() {
      List<CommentDTO> comments = commentService.findCommentsAll();
      return new ResponseEntity<>(comments, HttpStatus.OK);
    }

    @GetMapping("/{entityType}/{entityId}/count")
    public ResponseEntity<Long> countCommentsByEntity(
            @PathVariable String entityType, @PathVariable Long entityId
    ){
        Long count = commentService.countCommentsByEntity(entityType, entityId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentDTO> getCommentById(@PathVariable Long id){
        Optional<CommentDTO> commment = Optional.ofNullable(commentService.findCommentById(id));
        return commment.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/client/{clientGlobalId}")
    public ResponseEntity<Page<CommentDTO>> getCommentsByClientGlobalId(@PathVariable String clientGlobalId,
       @PageableDefault(size = 25, sort = "eventDate") Pageable pageable){
        return ResponseEntity.ok(commentService.findByClientGlobalId(clientGlobalId, pageable));
    }

    @PostMapping()
    public ResponseEntity<CommentDTO> createComment(@RequestBody CommentDTO commentDTO){
        CommentDTO createdComment = commentService.addComments(commentDTO);
        return ResponseEntity.ok(createdComment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentDTO> updateComment (@PathVariable Long id, @RequestBody CommentDTO commentDTO){
        CommentDTO updatedComment = commentService.updateComment(id, commentDTO);
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommentDTO> deleteComment(@PathVariable Long id){
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }







}
