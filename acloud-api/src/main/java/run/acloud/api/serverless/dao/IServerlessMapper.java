package run.acloud.api.serverless.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.serverless.vo.GatewayCertificateVO;
import run.acloud.api.serverless.vo.ServerlessInfoVO;
import run.acloud.api.serverless.vo.ServerlessVO;
import run.acloud.api.serverless.vo.ServerlessWorkspaceVO;

import java.util.List;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 1. 10.
 */
@Repository
public interface IServerlessMapper {

	ClusterVO getClusterByClusterId(@Param("clusterId") String clusterId);

	List<ServerlessWorkspaceVO> getWorkspaces(
			  @Param("accountSeq") Integer accountSeq
			, @Param("serviceName") String serviceName
			, @Param("userId") String userId
	);

	ServerlessWorkspaceVO getWorkspace(
			  @Param("accountSeq") Integer accountSeq
			, @Param("serviceSeq") Integer serviceSeq
			, @Param("serviceName") String serviceName
			, @Param("userId") String userId
	);

	int addServerless(ServerlessVO serverless);

	int removeServerless(
			  @Param("servicemapSeq") Integer servicemapSeq
			, @Param("updater") Integer updater
	);

	int addServerlessInfo(ServerlessInfoVO serverlessInfo);

	int updateFunctionToken(
			  @Param("serverlessInfoSeq") Integer serverlessInfoSeq
			, @Param("token") String token
			, @Param("updater") Integer updater
	);

	int updateFunctionTokenByName(
			  @Param("servicemapSeq") Integer servicemapSeq
			, @Param("serverlessType") String serverlessType
			, @Param("functionName") String functionName
			, @Param("token") String token
			, @Param("updater") Integer updater
	);

	int removeServerlessInfoByProject(
			  @Param("servicemapSeq") Integer servicemapSeq
			, @Param("updater") Integer updater
	);

	int removeServerlessInfo(
			@Param("servicemapSeq") Integer servicemapSeq
			, @Param("serverlessType") String serverlessType
			, @Param("functionName") String functionName
			, @Param("updater") Integer updater
	);

	int deleteServerlessInfoByProject(
			@Param("servicemapSeq") Integer servicemapSeq
			, @Param("updater") Integer updater
	);

	int deleteServerlessInfoByName(
			  @Param("servicemapSeq") Integer servicemapSeq
			, @Param("serverlessType") String serverlessType
			, @Param("functionName") String functionName
			, @Param("updater") Integer updater
	);

	int deleteServerlessInfo(
			@Param("serverlessInfoSeq") Integer serverlessInfoSeq
	);

	List<ServerlessVO> getServerlesses(
			  @Param("serviceSeq") Integer serviceSeq
			, @Param("serverlessType") String serverlessType
	);

	ServerlessVO getServerless(
			@Param("serverlessSeq") Integer serverlessSeq
	);

	ServerlessInfoVO getServerlessInfo(
			@Param("serverlessInfoSeq") Integer serverlessInfoSeq
	);


	int addGatewayCertificate(GatewayCertificateVO gatewayCertificate);

	List<GatewayCertificateVO> getGatewayCertificates();
}
