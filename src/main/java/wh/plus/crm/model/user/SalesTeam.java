package wh.plus.crm.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "sales_teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Lista userów zespołu nie jest potrzebna w JSON (dropdowny używają id+name)
    // i powodowała cykl serializacji SalesTeam -> users -> user.team -> ...
    @OneToMany(mappedBy = "team", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JsonIgnore
    private List<User> users;



}
