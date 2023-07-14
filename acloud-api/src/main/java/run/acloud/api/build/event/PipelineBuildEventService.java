package run.acloud.api.build.event;

import com.google.gson.JsonIOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.stereotype.Service;
import run.acloud.api.build.enums.BuildEventAction;
import run.acloud.api.build.vo.BuildEventResultVO;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.build.vo.BuildVO;
import run.acloud.commons.util.HttpClientUtil;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.exception.CocktailException;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PipelineBuildEventService{

    public void sendBuildState(String callbackUrl, Integer accountSeq, List<Integer> serviceSeqs, Integer buildSeq, BuildVO build) {
        log.debug("===================================================================");
        log.debug("sendBuildState : callbackUrl:{}, serviceSeqs:{}, buildSeq:{}, buildRunSeq:{}, BuildRunVO:{}", callbackUrl, serviceSeqs, buildSeq, JsonUtils.toGson(build));
        log.debug("===================================================================");
        try {
            // 빌드 상세
            BuildEventResultVO<BuildVO> buildResult = new BuildEventResultVO<BuildVO>();
            buildResult.setEvent(BuildEventAction.BUILD_STATE.getCode());

            Map<String, Object> target = new HashMap<>();
            target.put("accountSeq", accountSeq);
            target.put("serviceSeqs", serviceSeqs);
            target.put("buildSeq", buildSeq);
            buildResult.setTarget(target);

            BuildVO targetBuild = new BuildVO();
            if(build != null){
                BeanUtils.copyProperties(build, targetBuild, "buildSteps");
            }
            buildResult.setData(targetBuild);

            this.doMultiPost(callbackUrl, JsonUtils.toGson(buildResult));

        }
        catch (BeansException be) {
            log.error("Error on sendBuildState!", be);
        }
        catch (JsonIOException jie) {
            log.error("Error on sendBuildState!", jie);
        }
        catch (Exception e) {
            log.error("Error on sendBuildState!", e);
        }
    }

    public void sendBuildRunState(String callbackUrl, Integer accountSeq, List<Integer> serviceSeqs, Integer buildSeq, Integer buildRunSeq, BuildRunVO buildRunVO) {

        log.debug("===================================================================");
        log.debug("sendBuildRunState : callbackUrl:{}, serviceSeqs:{}, buildSeq:{}, buildRunSeq:{}, BuildRunVO:{}", callbackUrl, serviceSeqs, buildSeq, buildRunSeq, buildRunVO);
        log.debug("===================================================================");
        try {

            BuildEventResultVO<BuildRunVO> buildResult = new BuildEventResultVO<BuildRunVO>();
            buildResult.setEvent(BuildEventAction.BUILD_RUN_STATE.getCode());
            Map<String, Object> target = new HashMap<>();
            target.put("accountSeq", accountSeq);
            target.put("serviceSeqs", serviceSeqs);
            target.put("buildSeq", buildSeq);
            target.put("buildRunSeq", buildRunSeq);
            buildResult.setTarget(target);

            BuildRunVO targetBuildRun = new BuildRunVO();
            if(buildRunVO != null){
                BeanUtils.copyProperties(buildRunVO, targetBuildRun);
            }
            buildResult.setData(targetBuildRun);

            this.doMultiPost(callbackUrl, JsonUtils.toGson(buildResult));

        }
        catch (BeansException be) {
            log.error("Error on sendBuildState!", be);
        }
        catch (JsonIOException jie) {
            log.error("Error on sendBuildState!", jie);
        }
        catch (Exception e) {
            log.error("Error on sendBuildRunState!", e);
        }
    }

    private void doMultiPost(String callbackUrl, String buildResult) throws Exception {
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

                        result = HttpClientUtil.doPost(StringUtils.substringBefore(url, "?"), headers, buildResult);
                    }else{
                        result = HttpClientUtil.doPost(url, buildResult);
                    }
                    log.debug(result);
                }
                catch (StringIndexOutOfBoundsException sioobe) {
                    log.error("Error on doMultiPost - url : ["+url+"], data : [{}]", sioobe, buildResult);
                }
                catch (CocktailException ce) {
                    log.error("Error on doMultiPost - url : ["+url+"], data : [{}]", ce, buildResult);
                }
                catch (Exception e){
                    log.error("Error on doMultiPost - url : ["+url+"], data : [{}]", e, buildResult);
                }
            }
        }
    }

}
