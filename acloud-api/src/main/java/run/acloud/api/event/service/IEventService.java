package run.acloud.api.event.service;

import org.springframework.stereotype.Service;
import run.acloud.commons.vo.ExecutingContextVO;

@Service
public interface IEventService {

	void sendServices(Integer serviceSeq, Integer servicemapSeq, ExecutingContextVO context);

	void sendSerivcemapServers(Integer servicemapSeq, ExecutingContextVO context);

	void sendServicemapGroups(Integer serviceSeq, ExecutingContextVO context);

}
