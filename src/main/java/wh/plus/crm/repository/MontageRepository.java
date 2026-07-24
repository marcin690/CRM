package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.montage.Montage;

import java.util.List;

public interface MontageRepository extends JpaRepository<Montage, Long> {
    List<Montage> findAllByProject_IdOrderByStartDateAsc(Long projectId);

    List<Montage> findAllByOrderByStartDateAsc();

    /** Odpięcie montaży od usuwanej ekipy (żeby nie zostawały wiszące referencje). */
    @Modifying
    @Query("update Montage m set m.crew = null where m.crew.id = :crewId")
    void clearCrew(@Param("crewId") Long crewId);

    /** Odpięcie montaży od usuwanego etapu (luźne stageId — inaczej zostają sieroty). */
    @Modifying
    @Query("update Montage m set m.stageId = null where m.stageId = :stageId")
    void clearStage(@Param("stageId") Long stageId);
}
