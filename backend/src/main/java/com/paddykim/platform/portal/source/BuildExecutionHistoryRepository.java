package com.paddykim.platform.portal.source;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildExecutionHistoryRepository extends JpaRepository<BuildExecutionHistory, Long> {

    List<BuildExecutionHistory> findBySourceRepositoryIdAndBuildProfileIdOrderByCreatedAtDesc(
            Long sourceRepositoryId,
            Long buildProfileId
    );

    void deleteBySourceRepositoryId(Long sourceRepositoryId);

    void deleteByBuildProfileId(Long buildProfileId);
}
