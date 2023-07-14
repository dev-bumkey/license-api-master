package run.acloud.api.build.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import run.acloud.api.build.vo.BuildRunVO;

@Slf4j
@Service
public class PipelineAsyncService {

	@Autowired
	private PipelineBuildRunService buildRunService;

	/**
	 * BuildRunType에 의해 Async로 처리할 메서드 분기처리.<br/>
	 * 현재는 빌드 삭제와 빌드실행취소 2가지만 존재함.
	 *
	 * @param buildRunVO BuildRunVO 객체가 넘어옴
	 */
	@Async
	public void processPipelineService(BuildRunVO buildRunVO){

		switch (buildRunVO.getRunType()){
			case REMOVE:
				buildRunService.removeBuild(buildRunVO);
			case CANCEL:
				buildRunService.cancelBuildRun(buildRunVO);
		}

	}

}
