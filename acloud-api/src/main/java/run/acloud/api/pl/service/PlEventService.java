package run.acloud.api.pl.service;

import io.nats.client.Connection;
import io.nats.client.Nats;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.api.pl.vo.PlEventResVO;
import run.acloud.api.pl.vo.PlEventResultVO;
import run.acloud.commons.service.NatsService;
import run.acloud.commons.util.HttpClientUtil;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailBuilderProperties;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PlEventService {

    public static String PREFIX_PUB_SUBJECT_FMT = "status/pl/run/%s";

    @Autowired
    private CocktailBuilderProperties cocktailBuilderProperties;

    @Autowired
    private NatsService natsService;

    public void sendPlRunState(String callbackUrl, Integer plSeq, Integer plRunSeq, PlStatus status) {
        sendPlRunState(callbackUrl, plSeq, plRunSeq, status, null);
    }

    public void sendPlRunState(String callbackUrl, Integer plSeq, Integer plRunSeq, PlStatus status, PlEventResVO eventResVO) {

        String logLine = "===================================================================";
        log.debug("{}\nsendPlRunState : callbackUrl:{}, plSeq:{}, plRunSeq:{}, plRunStatus:{}, eventResVO:{}\n{}", logLine, callbackUrl, plSeq, plRunSeq, status.getCode(), JsonUtils.toGson(eventResVO), logLine);

        try {

            if (callbackUrl != null) {
                PlEventResultVO<PlEventResVO> plResult = new PlEventResultVO<PlEventResVO>();
                plResult.setEvent("PL_RUN_STATE");
                Map<String, Object> target = new HashMap<>();
                target.put("plSeq", plSeq);
                target.put("plRunSeq", plRunSeq);
                target.put("status", status.getCode());
                plResult.setTarget(target);

                if(eventResVO != null){
                    plResult.setData(eventResVO);
                }

                // callback url 전송
                this.doMultiPost(callbackUrl, JsonUtils.toGson(plResult));

                // plrun 상태 publishing, 현대카드나 캐피털 SR-Agent 사용시 pl 상태에 대한 정보를 전송하기 위해 추가된 것으로 예상됨.
                this.publishPlStatus(plResult);
            }

        }
        catch (UnsupportedEncodingException uee) {
            log.error("Error on sendPlRunState!", uee);
        }
        catch (Exception e) {
            log.error("Error on sendPlRunState!", e);
        }
    }

    // callback URL로 이벤트 전송
    private void doMultiPost(String callbackUrl, String plResult) throws Exception {
        String result = "";
        String callbackHeader = "";
        if(StringUtils.isNotBlank(callbackUrl)){
            if(StringUtils.indexOf(callbackUrl, "%3A%2F%2F") > 0){
                callbackUrl = URLDecoder.decode(callbackUrl, "UTF-8");
            }
            for(String url : StringUtils.split(callbackUrl, "|")){
                try{
                    if(StringUtils.lastIndexOf(url, "?") > 0){
                        List<Header> headers = new ArrayList<>();
                        callbackHeader = StringUtils.substringAfterLast(url ,"?");

                        if(StringUtils.isNotBlank(callbackHeader)){
                            for(String param : StringUtils.split(callbackHeader, "&")){
                                if(StringUtils.isNotBlank(param) && StringUtils.indexOf(param, "=") > 0){
                                    headers.add(new BasicHeader(StringUtils.split(param, "=")[0], StringUtils.split(param, "=")[1]));
                                }
                            }
                        }

                        result = HttpClientUtil.doPost(StringUtils.substringBefore(url, "?"), headers, plResult);
                    }else{
                        result = HttpClientUtil.doPost(url, plResult);
                    }
                    log.debug(result);
                }
                catch (CocktailException ce) {
                    log.error("Error on doMultiPost - url : [{}], data : [{}]", url, plResult, ce);
                }
                catch (Exception e){
                    log.error("Error on doMultiPost - url : [{}], data : [{}]", url, plResult, e);
                }
            }
        }
    }

    // Nats로 PL상태 publishing
    private void publishPlStatus(PlEventResultVO<PlEventResVO> plResult) {

        // publish 할 subject 생성
        String subject = String.format(PREFIX_PUB_SUBJECT_FMT, plResult.getTarget().get("plRunSeq"));
        log.debug("public subject : "+ subject);


        try (Connection nc = Nats.connect(natsService.getNatsClientOption())){

            String message = JsonUtils.toGson(plResult);

            // public message
            nc.publish(subject, message.getBytes("UTF-8"));

        } catch (IOException | InterruptedException e) {
            log.error("fail publishPlStatus!!", e);
        }

    }
}
