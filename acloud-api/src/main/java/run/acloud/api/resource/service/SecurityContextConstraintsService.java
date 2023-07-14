package run.acloud.api.resource.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.resource.vo.SecurityContextConstraintsDetailVO;
import run.acloud.api.resource.vo.SecurityContextConstraintsNameVO;
import run.acloud.api.resource.vo.SecurityContextConstraintsVO;
import run.acloud.commons.client.ClusterApiClient;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SecurityContextConstraintsService {

    @Autowired
    private ClusterApiClient clusterApiClient;

    // 클러스터의 scc 목록을 조회한다.
    public List<SecurityContextConstraintsVO> listSecurityContextConstrants(Integer clusterSeq) throws CocktailException{
        Map<String, Object> result= clusterApiClient.listSecurityContextConstrants(clusterSeq);

        // 오류 체크
        resultCheck(result);

        ObjectMapperUtils.getMapper().convertValue(result.get("result"), new TypeReference<List<SecurityContextConstraintsVO>>(){});
        /* response
        {
          "code": 0,
          "status": "string",
          "message": "string",
          "result": [
            {
              "name": "string",
              "caps": "string",
              "priv": "string",
              "selinux": "string",
              "runasuser": "string",
              "fsgroup": "string",
              "supgroup": "string",
              "priority": "string",
              "readonlyrootfs": true,
              "volumes": "string"
            }
          ]
        }*/

        //result 처리
        List<SecurityContextConstraintsVO> sccList = ObjectMapperUtils.getMapper().convertValue(result.get("result"), new TypeReference<List<SecurityContextConstraintsVO>>(){});

        return sccList;
    }


    public SecurityContextConstraintsDetailVO getSecurityContextConstrants(Integer clusterSeq, String sccName) throws CocktailException {

        // cluster api call
        Map<String, Object> result = clusterApiClient.getSecurityContextConstrants(clusterSeq, sccName);

        // 오류 체크
        resultCheck(result);

        /* response
        {
          "code": 0,
          "status": "string",
          "message": "string",
          "name": "string",
          "describeData": "string"
        }*/

        // 결과 처리
        SecurityContextConstraintsDetailVO sccDetailVO = new SecurityContextConstraintsDetailVO();
        sccDetailVO.setName((String)result.get("name"));
        sccDetailVO.setDescribeData((String)result.get("describeData"));

        return sccDetailVO;
    }

    public List<SecurityContextConstraintsNameVO> getSecurityContextConstrantsInNamespce(Integer clusterSeq, String namespace) throws CocktailException {

        // cluster api call
        Map<String, Object> result = clusterApiClient.getSecurityContextConstrantsInNamespace(clusterSeq, namespace);

        // 오류 체크
        resultCheck(result);

        /* response
        {
          "code": 0,
          "status": "string",
          "message": "string",
          "namespace": "string",
          "roleRefs": [
            {
              "apiGroup": "string",
              "kind": "string",
              "name": "string",
              "sccName": "string"
            }
          ]
        }*/

        // result 처리
        List<SecurityContextConstraintsNameVO> sccNameList = new ArrayList<SecurityContextConstraintsNameVO>();

        List<Map<String, String>> sccmaplist = ObjectMapperUtils.getMapper().convertValue(result.get("roleRefs"), new TypeReference<List<Map<String, String>>>(){});
        for (Map<String, String> map : sccmaplist) {
            SecurityContextConstraintsNameVO sccNameVO = new SecurityContextConstraintsNameVO();
            sccNameVO.setName(map.get("sccName"));
            sccNameList.add(sccNameVO);
        }

        return sccNameList;
    }

    public void addSCC(Integer clusterSeq, String namespace, String sccName) throws CocktailException {

        // cluster api call
        Map<String, Object> result = clusterApiClient.addSCC(clusterSeq, namespace, sccName);

        // 오류 체크
        resultCheck(result);
    }

    public void deleteSCC(Integer clusterSeq, String namespace, String sccName) throws CocktailException {
        // cluster api call
        Map<String, Object> result = clusterApiClient.deleteSCC(clusterSeq, namespace, sccName);

        // 오류 체크
        resultCheck(result);
    }

    private void resultCheck(Map<String, Object> result) throws CocktailException {
        // 오류 체크
        Integer code = (Integer) result.get("code");
        String status = (String) result.get("status");
        String message = (String) result.get("message");
        if (code.intValue()!= 200 || !"ok".equals(status)) {
            String exceptionMessage = String.format("fail to call Cluster API - [code: %s, status: %s, message: %s]!!", code, status, message);
            throw new CocktailException(exceptionMessage, ExceptionType.ExternalApiFail_ClusterApi);
        }
    }
}
