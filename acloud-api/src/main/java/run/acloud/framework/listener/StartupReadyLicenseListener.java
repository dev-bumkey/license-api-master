package run.acloud.framework.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import run.acloud.api.openapi.service.OpenapiService;
import run.acloud.commons.service.LicenseService;

@Slf4j
@Component
@Order(0)
public class StartupReadyLicenseListener implements ApplicationListener<ApplicationReadyEvent> {
    
    @Autowired
    private LicenseService licenseService;

    @Autowired
    private OpenapiService openapiService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        try {
            // issue trial license
            licenseService.issueTrialLicense(null, true, false, false);

            // generate jwks
            openapiService.generateJwk(true, false, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }
}
