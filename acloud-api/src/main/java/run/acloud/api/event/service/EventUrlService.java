package run.acloud.api.event.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.event.constants.EventConstants;
import run.acloud.api.event.vo.EventResultVO;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.properties.CocktailUIProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 25.
 */
@Slf4j
@Service
public class EventUrlService extends BaseEvent implements IEventService{

    @Autowired
    private CocktailUIProperties uiProperties;

    @SuppressWarnings("unchecked")
    @Override
    public void sendServices(Integer serviceSeq, Integer servicemapSeq, ExecutingContextVO context) {
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        log.debug("Callback sendServices : {}, {}, {}", serviceSeq, servicemapSeq, JsonUtils.toGson(context));
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        try {
            Integer userSeq = null;
            String userRole = null;
            if (ContextHolder.exeContext() != null){
                userSeq = ContextHolder.exeContext().getUserSeq();
                userRole = ContextHolder.exeContext().getUserRole();
            }

            if (userSeq == null && context != null){
                userSeq = context.getUserSeq();
                userRole = context.getUserRole();
            }

            // TODO: 정보를 실제 받을 수 있도록 수정해야 함.
            EventResultVO result = makeCallback(serviceSeq, servicemapSeq, userRole, userSeq, EventConstants.TYPE_SERVICE_SUMMARIES);
            this.callback(result);

        }
        catch (ClientProtocolException cpe) {
            exceptionHandle("Fail to callback sendServices to User Interface", cpe, context);
            log.warn("Fail to callback sendServices to User Interface: {}", cpe.getMessage());
        }
        catch (IOException ie) {
            exceptionHandle("Fail to callback sendServices to User Interface", ie, context);
            log.warn("Fail to callback sendServices to User Interface: {}", ie.getMessage());
        }
        catch (Exception eo) {
            exceptionHandle("Fail to callback sendServices to User Interface", eo, context);
            log.warn("Fail to callback sendServices to User Interface: {}", eo.getMessage());
        }
    }

    private EventResultVO makeCallback(Integer servicemapSeq, String userRole, Integer requester, String event) {
        return this.makeCallback(null, servicemapSeq, userRole, requester, event);
    }

    private EventResultVO makeCallback(Integer serviceSeq, Integer servicemapSeq, String userRole, Integer requester, String event) {
        Map<String, Object>  target = new HashMap<>();
        if(serviceSeq != null){
            target.put("serviceSeq", serviceSeq);
        }
        if(servicemapSeq != null){
            target.put("servicemapSeq", servicemapSeq);
        }
        if (requester != null) {
            target.put("userSeq", requester);
        }
        if (StringUtils.isNotBlank(userRole)) {
            target.put("userRole", userRole);
        }

        EventResultVO eventResult = this.makeCallback(target, event);

        return eventResult;
    }

    private EventResultVO makeCallback(Map<String, Object> target, String event) {
        EventResultVO eventResult = new EventResultVO();
        eventResult.setEvent(event);
        eventResult.setTarget(target);

        return eventResult;
    }

    private void callback(EventResultVO result) throws Exception {
        HttpClient httpClient = Utils.makeHttpClient(false);
        if (uiProperties == null) {
            log.debug("CocktailUIProperties is NULL!!!");
        } else {
            log.debug("CALLBACK URL = " + uiProperties.getCallbackUrl());

            HttpPost post = new HttpPost(this.uiProperties.getCallbackUrl());
            StringEntity entity = new StringEntity(JsonUtils.toGson(result));
            post.setEntity(entity);
            post.setHeader("Content-type", "application/json");
            if(ContextHolder.exeContext().getUserSeq() != null){
                post.setHeader("user-id", String.valueOf(ContextHolder.exeContext().getUserSeq()));
            }else {
                post.setHeader("user-id", MapUtils.getString(result.getTarget(), "userSeq", null));
            }
            if(StringUtils.isNotBlank(ContextHolder.exeContext().getUserRole())){
                post.setHeader("user-role", ContextHolder.exeContext().getUserRole());
            }else {
                post.setHeader("user-role", MapUtils.getString(result.getTarget(), "userRole", null));
            }

            HttpResponse response = httpClient.execute(post);
            log.debug("User interface response code : {}, content: {}", response.getStatusLine().getStatusCode(), response);
        }

    }

    @Override
    public void sendSerivcemapServers(Integer servicemapSeq, ExecutingContextVO context) {
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        log.debug("Callback sendSerivcemapServers : {}, {}", servicemapSeq, JsonUtils.toGson(context));
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        try {
            EventResultVO result = makeCallback(servicemapSeq, context.getUserRole(), context.getUserSeq(), EventConstants.TYPE_APPMAP_SERVER);

            this.callback(result);
        }
        catch (ClientProtocolException cpe) {
            exceptionHandle("Fail to callback sendSerivcemapServers to User Interface", cpe, context);
            log.warn("Fail to callback sendSerivcemapServers to User Interface: {}", cpe.getMessage());
        }
        catch (IOException ie) {
            exceptionHandle("Fail to callback sendSerivcemapServers to User Interface", ie, context);
            log.warn("Fail to callback sendSerivcemapServers to User Interface: {}", ie.getMessage());
        }
        catch (Exception eo) {
            exceptionHandle("Fail to callback sendSerivcemapServers to User Interface", eo, context);
            log.warn("Fail to callback sendSerivcemapServers to User Interface: {}", eo.getMessage());
        }
    }

    @Override
    public void sendServicemapGroups(Integer serviceSeq, ExecutingContextVO context) {
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        log.debug("Callback sendServicemapGroups : {}, {}", serviceSeq, JsonUtils.toGson(context));
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        try {
            EventResultVO  result = makeCallback(serviceSeq, null, ContextHolder.exeContext().getUserRole(), ContextHolder.exeContext().getUserSeq(), EventConstants.TYPE_APPMAP_GROUP);
            this.callback(result);
        }
        catch (ClientProtocolException cpe) {
            exceptionHandle("Fail to callback sendServicemapGroups to User Interface", cpe, context);
            log.warn("Fail to callback sendServicemapGroups to User Interface: {}", cpe.getMessage());
        }
        catch (IOException ie) {
            exceptionHandle("Fail to callback sendServicemapGroups to User Interface", ie, context);
            log.warn("Fail to callback sendServicemapGroups to User Interface: {}", ie.getMessage());
        }
        catch (Exception eo) {
            exceptionHandle("Fail to callback sendServicemapGroups to User Interface", eo, context);
            log.warn("Fail to callback sendServicemapGroups to User Interface: {}", eo.getMessage());
        }
    }


}
