package com.paddykim.platform.portal.cicd;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CicdRequestRepository extends JpaRepository<CicdRequest, Long> {

    @Override
    @EntityGraph(attributePaths = {"application", "environment", "component"})
    List<CicdRequest> findAll();
}
