package com.squadup.repository;

import com.squadup.dto.SquadSearchCriteria;
import com.squadup.entity.Squad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Custom repository fragment: queries too dynamic for derived methods or @Query. */
public interface SquadRepositoryCustom {

    Page<Squad> search(SquadSearchCriteria criteria, Pageable pageable);
}
