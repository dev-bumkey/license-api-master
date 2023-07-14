package run.acloud.api.external.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.acloud.commons.annotations.InHouse;
import run.acloud.framework.properties.CocktailEfkProperties;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
@InHouse
@Slf4j
@RestController
@RequestMapping(value = "/api/env")
public class EnvironmentController {

    @Autowired
    private CocktailEfkProperties cocktailEfkProperties;

    @GetMapping("/{envName}")
    public String getEnv(@PathVariable String envName) throws Exception {
    	log.debug("[BEGIN] getEnv");

    	String result = "";

        if("kibanaUrl".equals(envName)) {
            result = cocktailEfkProperties.getKibanaUrl();
        }
    	
        log.debug("[END  ] getEnv");

        return result;
    }
}
