package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.EventDTO;
import wh.plus.crm.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/client/{clientId}")
    public List<EventDTO> getEventsByClientid(@PathVariable Long clientId){
        return eventService.getEventsByClientID(clientId);
    }

    @GetMapping("/project/{clientId}")
    public List<EventDTO> getEventsByProjectId(@PathVariable Long projectId){
        return eventService.getEventByProjectId(projectId);
    }

    @PostMapping
    public EventDTO createEvent(@RequestBody EventDTO eventDTO ){
       return eventService.createEvent(eventDTO);
    }

    @PatchMapping("/{eventId}")
    public EventDTO updateEvent(@PathVariable Long eventId, @RequestBody EventDTO eventDTO){
        return eventService.updateEvent(eventId, eventDTO);
    }

    @DeleteMapping("/{eventId}")
    public void deleteEvent(@PathVariable Long evenId){
        eventService.deleteEvent(evenId);
    }



}
