package run.acloud.api.build.service;

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import run.acloud.api.build.constant.BuildConstants;
import run.acloud.api.build.dao.IBuildRunMapper;
import run.acloud.api.build.vo.BuildStepRunVO;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.service.NatsService;
import run.acloud.framework.properties.CocktailBuilderProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class PipelineBuildRunLogService {

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private CocktailBuilderProperties cocktailBuilderProperties;

	@Autowired
	private NatsService natsService;

	/**
	 * build log subscribe 하여 DB에 저장하는 메서드
	 * Async 로 처리함.
	 *
	 * @param buildStepRunSeq
	 * @update 20230530, coolingi, natstream => jetstream 연동으로 변경
	 */
	@Async
	public void updateBuildLog(Integer buildStepRunSeq){
		IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

		// logId 조회
		BuildStepRunVO buildStepRun = buildRunDao.getBuildStepRun(buildStepRunSeq);

		// 조회할 log의 subject 생성
		String subject = buildStepRun.getLogId();
		String streamName = cocktailBuilderProperties.getBuildQueueCid();
		String logSubject = streamName+"."+subject;

		SecureRandom rand = new SecureRandom();
		rand.setSeed(new Date().getTime());
		String clientName = BuildConstants.DEFAULT_NAT_CLIENT_ID + "_" + ResourceUtil.getResourcePrefix()+"_"+streamName + "_" + rand.nextInt(99999);


		// Consumer 설정
		ConsumerConfiguration cc = ConsumerConfiguration.builder()
				.inactiveThreshold(Duration.ofMinutes(2))
				.build();

		// pull option
		PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
				.name(clientName)
				.configuration(cc)
				.build();

		int readBlockCnt = 20; // 한번에 패치할 msg 갯수
		JetStreamSubscription sub = null;

		try (Connection nc = Nats.connect(natsService.getNatsClientOption())){

			JetStream jc = nc.jetStream();
			sub = jc.subscribe(logSubject, pullOptions);
			ConsumerInfo ci = sub.getConsumerInfo();

			long maxMsgCnt = ci.getNumPending();

			if (maxMsgCnt <= readBlockCnt) { // 읽어야 할 갯수가 readBlockCnt 보다 작거나 같을때는 한번에 읽고 끝낸다.
				fetchAndUpdateLog(buildStepRunSeq, sub, Long.valueOf(maxMsgCnt).intValue());

			} else { // 로그가 많을경우는 readBlockCnt 만큼씩만 읽어서 db에 저장한다.
				for (int i = readBlockCnt; i <= maxMsgCnt; i+=readBlockCnt) {
					fetchAndUpdateLog(buildStepRunSeq, sub, readBlockCnt);

					// i+readBlockCnt > maxMsgCnt 때는 다음 loop에 처리가 안되기 때문에 미리 처리한다.
					if (i+readBlockCnt > maxMsgCnt) {
						fetchAndUpdateLog(buildStepRunSeq, sub, Long.valueOf(maxMsgCnt-i).intValue());
					}
				}
			}

		} catch (IOException | InterruptedException | JetStreamApiException e) {
			log.error("Subscribe should not have thrown", e);
		} finally {
			if (sub != null && sub.isActive()) {
				sub.unsubscribe();
			}
		}

	}

	/**
	 * stream subscription 에서 fetch count만큼 가져와 해당 task log field에 update 한다.
	 *
	 * @param buildStepRunSeq update할 시퀀스
	 * @param sub stream subscription
	 * @param fetchCnt fetch할 count
	 */
	private void fetchAndUpdateLog(Integer buildStepRunSeq, JetStreamSubscription sub, int fetchCnt){
		StringBuilder logBuffer = new StringBuilder();

		List<Message> list = sub.fetch(fetchCnt, Duration.ofSeconds(60));
		String msgStr = "";
		for (Message msg : list) {
			msgStr = new String(msg.getData(), StandardCharsets.UTF_8);
			logBuffer.append(msgStr);
		}
		// 메시지 DB update
		if (logBuffer.length() > 0) {
			updateBuildStepRunLog(buildStepRunSeq, logBuffer.toString());
			logBuffer.setLength(0);
		}
	}

	// 로그 저장 메서드
	private int updateBuildStepRunLog(Integer buildStepRunSeq, String log){
		IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
		return buildRunDao.updateBuildStepRunLog(buildStepRunSeq, log);
	}

}
