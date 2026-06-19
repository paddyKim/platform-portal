package com.paddykim.platform.portal.cicd;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {

    private final CicdRequestService cicdRequestService;

    public AuditEventController(CicdRequestService cicdRequestService) {
        this.cicdRequestService = cicdRequestService;
    }

    @GetMapping
    public List<AuditEventResponse> listAuditEvents() {
        return cicdRequestService.listAuditEvents();
    }
}
