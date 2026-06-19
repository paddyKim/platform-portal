package com.paddykim.platform.portal.cicd;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Override
    @EntityGraph(attributePaths = {"cicdRequest"})
    List<AuditEvent> findAll();
}
