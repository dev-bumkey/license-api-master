package run.acloud.api.pl.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;
import run.acloud.api.build.service.PipelineBuildValidationService;
import run.acloud.api.build.vo.BuildAddVO;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.pipelineflow.service.PipelineAPIService;
import run.acloud.api.pipelineflow.service.WrappedBuildService;
import run.acloud.api.pl.dao.IPlMapper;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.api.pl.vo.PlRunBuildVO;
import run.acloud.api.pl.vo.PlRunVO;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.properties.CocktailUIProperties;

import java.util.Comparator;

@Slf4j
@Service
public class PlRunBuildService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private WrappedBuildService buildService;

    @Autowired
    private PipelineBuildValidationService buildValidationService;

    @Autowired
    @Qualifier(value = "pipelineBuildAddValidator")
    private Validator buildAddValidator;

    @Autowired
    private PipelineAPIService pipelineAPIService;

    @Autowired
    private CocktailUIProperties cocktailUIProperties;

    /**
     * 암호화된 BuildRunVO json 문자열을 복호화후 BuildRunVO 생성 & step 정보별 VO 만들어 셋팅후 리턴, 패스워드 없이 조회
     *
     * @param buildRunContents
     * @return
     * @throws Exception
     */
    public BuildRunVO convertBuildRunStringToDecryptedBuildRunVO(String buildRunContents) throws Exception {
        return convertBuildRunStringToDecryptedBuildRunVO(buildRunContents, true);
    }

    public BuildRunVO convertBuildRunStringToDecryptedBuildRunVO(String buildRunContents, boolean withoutPasswd) throws Exception {
        BuildRunVO buildConfig = JsonUtils.fromGson(buildRunContents, BuildRunVO.class);
        buildConfig = buildService.convertBuildStepRunConfig(buildConfig, withoutPasswd);

        return buildConfig;
    }

    /**
     * plRunSeq에 해당하는 PlRun 정보를 조회해 하나의 빌드 대상을 찾아 빌드 실행하는 메서드. <br/>
     * 빌드 실패시 빌드상태 update.
     *
     * @param plRunSeq
     */
    public PlRunBuildVO runPlBuild(Integer plRunSeq, ExecutingContextVO ctx){
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        PlRunVO plRun = plDao.getPlRunDetail(plRunSeq, "Y");// 실행할 대상인 건만 조회

        String callback = cocktailUIProperties.getCallbackBuilderUrl();

        // 실행할 빌드 정보 추출, 아직 실행되지 않은 건 중에 order 값 제일 작은건
        PlRunBuildVO plRunBuild = null;
        if(CollectionUtils.isNotEmpty(plRun.getPlRunBuilds())){
            // 상태가 WAIT 이면서 최소 order인 건 조회
            plRunBuild = plRun.getPlRunBuilds().stream().filter(build -> build.getRunStatus() == PlStatus.WAIT).min(Comparator.comparingInt(PlRunBuildVO::getRunOrder)).orElseGet(() ->null);
        }

        if (plRunBuild != null) {
            // 빌드 실행시 빌드서버에서 호출하는 response URL를 결정하기 위한 값 설정
            ExecutingContextVO executingContextVO = ContextHolder.exeContext(ctx);
            executingContextVO.setPlSeq(plRun.getPlSeq());
            executingContextVO.setPlRunSeq(plRunSeq);
            executingContextVO.setPlRunBuildSeq(plRunBuild.getPlRunBuildSeq());

            // PlRunBuild 의 RunStatus setting
            plRunBuild.setRunStatus(PlStatus.RUNNING);
            PlStatus plRunStatus = plRunBuild.getRunStatus();

            try {
                // PlRunBuild 의 RunStatus update & log update
                plDao.updatePlRunBuildStatus(plRunBuild.getPlRunBuildSeq(), plRunBuild.getRunStatus().toString());
                this.updatePlRunBuildLog(plRunBuild);

                // build validation, 동시빌드수 & 현재 build seq 에 해당하는 실행중인 build가 있는지 체크
                this.checkRunBuildValidation(plRun.getPlSeq(), plRunBuild);

                // 빌드 실행을 위한 VO 생성
                BuildRunVO tmpBuildRun = this.convertBuildRunStringToDecryptedBuildRunVO(plRunBuild.getBuildCont(), false);

                // 자동 태그가 아니고, 이전 태그와 현재 태그가 다르면 현재정 태그 설정
                String tagName = plRunBuild.getBuildTag();
                if (!"Y".equals(tmpBuildRun.getAutotagUseYn()) && !StringUtils.equals(tmpBuildRun.getTagName(), tagName)){
                    tagName = tmpBuildRun.getTagName();
                }
                tmpBuildRun.setTagName(tagName);

                BuildAddVO buildAdd = buildService.convertFromBuildRunToBuildAdd(tmpBuildRun);

                // 빌드 정보 validation check
                buildValidationService.checkBuildByAdd(buildAddValidator, buildAdd);

                // 빌드 실행 생성, buildRun 리턴
                BuildRunVO newBuildRun = buildService.createBuildRunByBuildRunModify(plRunBuild.getBuildSeq(), plRunBuild.getBuildPrevRunSeq(), callback, tagName, plRun.getRunNote(), 0,buildAdd, null);

                // PlRunBuild 의 build_run_seq updatee
                plDao.updatePlRunBuildForBuildRunSeq(plRunBuild.getPlRunBuildSeq(), newBuildRun.getBuildRunSeq());

                // 빌드 실행
                newBuildRun = pipelineAPIService.runBuild(newBuildRun);

                // 빌드 실행 상태 update 및 이벤트 전송
                buildService.updateStateAndSendEvent(newBuildRun);

                // plRunBuild의 상태 설정
                switch (newBuildRun.getRunState()){
                    case ERROR:
                        plRunStatus = PlStatus.ERROR;
                        break;
                    default:
                        log.debug("Unknown Run Status");
                        break;
                }

            } catch (Exception e) {
                log.debug("trace log ", e);
                plRunStatus = PlStatus.ERROR;
            } finally {
                plRunBuild.setRunStatus(plRunStatus);
            }

            // 실행시 오류 일때는 PlRun 정보 종료 처리
            if ( plRunBuild.getRunStatus() == PlStatus.ERROR ){
                // PlRunBuild 의 RunStatus update
                plDao.updatePlRunBuildStatus(plRunBuild.getPlRunBuildSeq(), plRunBuild.getRunStatus().toString());
                this.updatePlRunBuildLog(plRunBuild);

                plDao.updatePlRunStatus(PlStatus.ERROR.toString(), plRunSeq);
            }

        }

        return plRunBuild;
    }

    /**
     * PlRunBuildVO 의 상태에 따른 build log 저장 메서드
     *
     * @param plRunBuild
     */
    public void updatePlRunBuildLog(PlRunBuildVO plRunBuild){
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        String runLog = "\n";
        switch (plRunBuild.getRunStatus()){
            case RUNNING:
                runLog = runLog + "Start Build ";
                break;
            case ERROR:
                runLog = runLog + "Error Build ";
                break;
            case DONE:
                runLog = runLog + "Done Build ";
                break;
        }

        // build name 추출 및 log update
        runLog = runLog + this.getBuildNameFromImgURL(plRunBuild.getImgUrl());

        plDao.updatePlRunBuildLog(plRunBuild.getPlRunBuildSeq(), runLog);
    }

    private String getBuildNameFromImgURL(String imgUrl){
        String buildName = null;
        String[] tokens = imgUrl.split("[:]")[0].split("[/]");
        buildName = tokens[tokens.length -1];
        return buildName;
    }


    /**
     * 빌드 실행할때 마다 체크하는 메서드,
     * 동시빌드수 & 현재 build seq 에 해당하는 실행중인 build가 있는지 체크
     *
     * @param plSeq
     * @param runBuild
     * @throws Exception
     */
    private void checkRunBuildValidation(Integer plSeq, PlRunBuildVO runBuild) throws Exception{

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        Integer accountSeq = plDao.getAccountSeq(plSeq);
        // 빌드 가능한지 체크, 불가능 하면 Exception 발생
        buildService.checkPossibleRunBuildBySystem(accountSeq);

        // 현재 실행중인 빌드가 있는지 체크, 불가능 하면 Exception 발생
        buildService.checkPossibleRunBuildByBuild(runBuild.getBuildSeq(), 0);
    }

}
