package wh.plus.crm.mapper;

import org.mapstruct.*;
import wh.plus.crm.dto.ContactDTO;
import wh.plus.crm.dto.client.ClientDTO;
import wh.plus.crm.dto.client.ClientSummaryDTO;
import wh.plus.crm.dto.offer.OfferSummaryDTO;
import wh.plus.crm.dto.project.ProjectSummaryDTO;
import wh.plus.crm.model.Contact;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.project.Project;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "projects", expression = "java(mapProjectsToSummary(client.getProjects()))")
    @Mapping(target = "offers", expression = "java(mapOffersToSummary(client.getOffers()))")
    @Mapping(target = "contacts", expression = "java(mapContactsToDTO(client.getContacts()))")
    ClientDTO clientToClientDTO(Client client);

    Client clientDTOToClient(ClientDTO clientDTO);

    ClientSummaryDTO clientToClientSummaryDTO(Client client);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "offers", ignore = true)
    void updateClientFromDTO(ClientDTO clientDTO, @MappingTarget Client client);


    default List<ProjectSummaryDTO> mapProjectsToSummary(List<Project> projects) {
        if(projects == null) {
            return Collections.emptyList();
        }
        return projects.stream()
                .map(project -> new ProjectSummaryDTO(project.getId(), project.getName()))
                .collect(Collectors.toList());
    }

    default List<OfferSummaryDTO> mapOffersToSummary(List<Offer> offers){
        if (offers == null) {
            return Collections.emptyList();
        }
        return offers.stream()
                .map(offer -> new OfferSummaryDTO(offer.getId(), offer.getName(), offer.getOfferStatus(), offer.getTotalPrice()))
                .collect(Collectors.toList());
    }

    default List<ContactDTO> mapContactsToDTO(List<Contact> contacts) {
        if (contacts == null) {
            return Collections.emptyList();
        }
        return contacts.stream()
                .map(contact -> new ContactDTO(
                        contact.getId(),
                        contact.getFirstName(),
                        contact.getLastName(),
                        contact.getPhone(),
                        contact.getEmail(),
                        contact.getComment(),
                        null, // Możesz pominąć mapowanie projektu tutaj
                        null  // Możesz pominąć mapowanie klienta tutaj
                ))
                .collect(Collectors.toList());
    }


}
