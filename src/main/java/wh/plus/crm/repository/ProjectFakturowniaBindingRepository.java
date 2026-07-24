package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.invoice.ProjectFakturowniaBinding;

import java.util.List;

public interface ProjectFakturowniaBindingRepository extends JpaRepository<ProjectFakturowniaBinding, Long> {
    List<ProjectFakturowniaBinding> findAllByProject_Id(Long projectId);

    List<ProjectFakturowniaBinding> findAllByAccount_Id(Long accountId);

    List<ProjectFakturowniaBinding> findAllByAccount_EnabledTrue();

    /** Odpięcie wiązań faktur od usuwanego etapu (luźne stageId — inaczej finanse etapu wiszą na sierocie). */
    @Modifying
    @Query("update ProjectFakturowniaBinding b set b.stageId = null where b.stageId = :stageId")
    void clearStage(@Param("stageId") Long stageId);
}
