package wh.plus.crm.mapper;
import org.mapstruct.*;
import wh.plus.crm.dto.ContactDTO;
import wh.plus.crm.dto.client.ClientSummaryDTO;
import wh.plus.crm.dto.project.ProjectSummaryDTO;
import wh.plus.crm.model.Contact;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.project.Project;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ContactMapper {

        /**
         * Mapuje encję Contact na obiekt ContactDTO.
         *
         * @param contact encja Contact do zmapowania
         * @return obiekt ContactDTO
         */
        @Mapping(target = "project", expression = "java(mapProjectToSummary(contact.getProject()))")
        @Mapping(target = "client" , expression = "java(mapClientToSummary(contact.getClient()))")
        ContactDTO contactToContactDTO(Contact contact);

        /**
         * Mapuje obiekt ContactDTO na encję Contact.
         *
         * @param contactDTO obiekt ContactDTO do zmapowania
         * @return encja Contact
         */
        Contact contactDTOToContact(ContactDTO contactDTO);

        /**
         * Aktualizuje istniejącą encję Contact na podstawie obiektu ContactDTO.
         * Ignoruje wartości null w polach DTO.
         *
         * @param contactDTO obiekt ContactDTO z danymi do aktualizacji
         * @param contact    encja Contact do zaktualizowania
         */
        @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
        void updateContactFromContactDTO(ContactDTO contactDTO, @MappingTarget Contact contact);

        /**
         * Mapuje pojedynczy obiekt `Project` na `ProjectSummaryDTO`.
         * <p>
         * Używane do mapowania relacji z pojedynczym projektem.
         *
         * @param project Obiekt Project do zmapowania.
         * @return Obiekt ProjectSummaryDTO zawierający uproszczone dane projektu
         *         lub null, jeśli project jest null.
         */
        default ProjectSummaryDTO mapProjectToSummary(Project project){
                if (project == null) return null;
                return new ProjectSummaryDTO(project.getId(), project.getName());
        }

        /**
         * Mapuje listę obiektów `Project` na `List<ProjectSummaryDTO>`.
         * <p>
         * Obsługuje mapowanie relacji, gdzie encja Contact ma przypisaną listę projektów.
         *
         *
         * @return Lista obiektów ProjectSummaryDTO. Zwraca pustą listę, jeśli projects jest null.
         */
        default ClientSummaryDTO mapClientToSummary(Client client) {
                if (client == null) {
                        return null;
                }
                return ClientSummaryDTO.builder()
                        .id(client.getId())
                        .clientFullName(client.getClientFullName())
                        .clientBusinessName(client.getClientBusinessName())
                        .build();
        }

}
