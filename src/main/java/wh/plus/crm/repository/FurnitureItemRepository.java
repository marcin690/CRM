package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.furniture.FurnitureItem;

import java.util.List;

public interface FurnitureItemRepository extends JpaRepository<FurnitureItem, Long> {
    List<FurnitureItem> findAllByProject_IdOrderBySortOrderAscIdAsc(Long projectId);
}
