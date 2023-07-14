package run.acloud.api.event.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.event.enums.EventTypeEnum;
import run.acloud.framework.properties.CocktailUIProperties;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 25.
 */
@Slf4j
@Service
public class EventService{

    @Autowired
    private CocktailUIProperties cocktailUIProperties;

    @Autowired
    private IEventService eventUrlService;

//    @Autowired
//    @EventType(EventTypeEnum.Names.MESSAGE)
//    private IEventService eventMessageService;

    public IEventService getInstance() {

        if (StringUtils.equalsIgnoreCase(EventTypeEnum.URL.getType(), cocktailUIProperties.getEventType())) {
            return this.eventUrlService;
        }

        return this.eventUrlService;
    }

    public IEventService getInstance(EventTypeEnum eventType) {
        IEventService eventServiceInst = null;

        switch (eventType){
            case URL:
                eventServiceInst = eventUrlService;
                break;
            case MESSAGE:
//                eventServiceInst = eventMessageService;
//                break;
            default:
                break;
        }

        return eventServiceInst;
    }
}
