package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.project.Project;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    Page<Project> findAll(Pageable pageable);

    @Query("SELECT c FROM Project c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%',:search,'%')) ")
    Page<Project> searchProject(@Param("search") String search, Pageable pageable);

    /** Projekty danego zespołu — do raportu finansowego z filtrowaniem widoczności. */
    List<Project> findAllBySalesTeam_Id(Long teamId);

}
