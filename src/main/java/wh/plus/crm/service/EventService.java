package wh.plus.crm.service;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.EventDTO;
import wh.plus.crm.helper.NullPropertyUtils;
import wh.plus.crm.mapper.EventMapper;
import wh.plus.crm.model.Event;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.repository.ClientRepository;
import wh.plus.crm.repository.EventRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Klasa serwisowa odpowiedzialna za obsługę logiki biznesowej związanej z wydarzeniami.
 * Obejmuje to pobieranie wydarzeń na podstawie identyfikatora klienta lub projektu, tworzenie, aktualizowanie i usuwanie wydarzeń.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository; // Repozytorium do dostępu do danych wydarzeń w bazie danych.
    private final EventMapper eventMapper; // Mapper do konwersji obiektów Event na EventDTO i odwrotnie.
    private final ClientRepository clientRepository;

    /**
     * Pobiera listę wydarzeń powiązanych z określonym identyfikatorem klienta.
     *
     * @param clientId identyfikator klienta, którego wydarzenia mają zostać pobrane.
     * @return lista obiektów EventDTO reprezentujących wydarzenia przypisane do określonego klienta.
     */
    public List<EventDTO> getEventsByClientID(Long clientId){
        return eventRepository.findByClientId(clientId)
                .stream()
                .map(eventMapper::eventToEventDTO) // Konwertuje każde wydarzenie z typu Event na EventDTO.
                .collect(Collectors.toList()); // Zbiera zamapowane EventDTO do listy.
    }

    /**
     * Pobiera listę wydarzeń powiązanych z określonym identyfikatorem projektu.
     *
     * @param projectId identyfikator projektu, którego wydarzenia mają zostać pobrane.
     * @return lista obiektów EventDTO reprezentujących wydarzenia przypisane do określonego projektu.
     */
    public List<EventDTO> getEventByProjectId(Long projectId){
        return eventRepository.findByProjectId(projectId)
                .stream()
                .map(eventMapper::eventToEventDTO) // Konwertuje każde wydarzenie z typu Event na EventDTO.
                .collect(Collectors.toList()); // Zbiera zamapowane EventDTO do listy.
    }

    /**
     * Tworzy nowe wydarzenie na podstawie przekazanego obiektu EventDTO.
     *
     * @param eventDTO obiekt DTO zawierający dane wydarzenia do zapisania.
     * @return stworzony obiekt EventDTO, zawierający dane wydarzenia po zapisaniu w bazie danych.
     */
    public EventDTO createEvent(EventDTO eventDTO){
        Event event = eventMapper.eventDTOTOEvent(eventDTO); // Konwertuje EventDTO na obiekt Event.

        if (eventDTO.getClientId() != null){
            Client client = clientRepository.findById(eventDTO.getClientId())
                    .orElseThrow(() -> new NoSuchElementException("Cliect not found"));
            event.setClient(client);
        }

        Event savedEvent = eventRepository.save(event); // Zapisuje wydarzenie w bazie danych.
        return eventMapper.eventToEventDTO(savedEvent); // Konwertuje zapisane wydarzenie z powrotem na EventDTO.
    }

    /**
     * Aktualizuje istniejące wydarzenie na podstawie przekazanego EventDTO.
     *
     * @param eventId identyfikator wydarzenia do zaktualizowania.
     * @param eventDTO obiekt DTO zawierający dane, które mają zostać zapisane.
     * @return zaktualizowany obiekt EventDTO.
     * @throws NoSuchElementException w przypadku, gdy wydarzenie o podanym identyfikatorze nie istnieje.
     */


    public EventDTO updateEvent(Long eventId, EventDTO eventDTO){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event id not found")); // Jeśli nie znaleziono wydarzenia, wyrzuca wyjątek.

        BeanUtils.copyProperties(eventDTO, event, NullPropertyUtils.getNullPropertyNames(eventDTO));

        eventMapper.updateEventFromEventDTO(eventDTO, event); // Aktualizuje dane wydarzenia na podstawie EventDTO.

        // Aktualizuj powiązanie z klientem tylko, jeśli clientId jest ustawione
        if (eventDTO.getClientId() != null) {
            if (eventDTO.getClientId() == 0) {
                // Usuwanie powiązania, gdy clientId = 0
                event.setClient(null);
            } else {
                // Aktualizacja powiązania z nowym klientem
                Client client = clientRepository.findById(eventDTO.getClientId())
                        .orElseThrow(() -> new NoSuchElementException("Client not found"));
                event.setClient(client);
            }
        }



        Event updatedEvent = eventRepository.save(event); // Zapisuje zaktualizowane wydarzenie w bazie danych.
        return eventMapper.eventToEventDTO(updatedEvent); // Konwertuje zaktualizowane wydarzenie na EventDTO.
    }

    /**
     * Usuwa wydarzenie o podanym identyfikatorze.
     *
     * @param eventId identyfikator wydarzenia do usunięcia.
     */
    public void deleteEvent(Long eventId){
        eventRepository.deleteById(eventId); // Usuwa wydarzenie z bazy danych na podstawie identyfikatora.
    }
}
