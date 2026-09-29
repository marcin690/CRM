package wh.plus.crm.specyfications;

import org.springframework.data.jpa.domain.Specification;
import wh.plus.crm.model.project.Project;

public class ProjectSpecification {

    public static Specification<Project> hasSalesTeam(Long salesTeamId) {
        return (root, query, cb) -> cb.equal(root.get("salesTeam").get("id"), salesTeamId);
    }

    public static Specification<Project> nameContains(String search) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }
}
