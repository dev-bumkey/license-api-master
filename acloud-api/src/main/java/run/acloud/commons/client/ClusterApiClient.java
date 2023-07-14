package run.acloud.commons.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.api.configuration.enums.IssueBindingType;
import run.acloud.api.configuration.enums.IssueType;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.IssueConfigAWSVO;
import run.acloud.api.configuration.vo.UserClusterRoleIssueBindingVO;
import run.acloud.api.configuration.vo.UserClusterRoleIssueVO;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.HttpClientUtil;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailClusterApiProperties;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 25.
 */
@Slf4j
@Component
public class ClusterApiClient {

    @Autowired
    private UserService userService;

    @Autowired
    private CocktailClusterApiProperties clusterApiProperties;

    public Map<String, Object> manageClusterRole(String userId, String userRole, Integer accountSeq, Integer userSeq, IssueType issueType, List<UserClusterRoleIssueVO> addRoles, List<UserClusterRoleIssueVO> editRoles, List<UserClusterRoleIssueVO> deleteRoles) throws CocktailException {
        String url = String.format("%s%s", StringUtils.removeEnd(clusterApiProperties.getClusterApiHost(), "/"), "/cluster/account/delete");

        Map<String, Object> result = Maps.newHashMap();
        try {

            Map<Integer, String> userRoleMap = Maps.newHashMap(); // 사용자 role 셋팅

            List<Header> headers = Lists.newArrayList();
            headers.add(new BasicHeader("user-id", userId));
            headers.add(new BasicHeader("user-role", userRole));

            List<Map<String, Object>> params = Lists.newArrayList();
            // 생성
            this.setUserClusterRoleIssueParams(params, "A", accountSeq, userSeq, issueType, userRoleMap, addRoles);
            // 변경
            this.setUserClusterRoleIssueParams(params, "E", accountSeq, userSeq, issueType, userRoleMap, editRoles);
            // 삭제
            this.setUserClusterRoleIssueParams(params, "D", accountSeq, userSeq, issueType, userRoleMap, deleteRoles);

            Map<String, Object> data = Maps.newHashMap();
            data.put("issueType", issueType.getCode());
            data.put("data", params);

            result = this.convertResponseToResult(HttpClientUtil.doPost(url, headers, ObjectMapperUtils.getMapper().writeValueAsString(data)), "manageClusterRole");

        } catch (JsonProcessingException jpe) {
            throw new CocktailException("fail to call Cluster API - [manageClusterRole]!!", jpe, ExceptionType.ExternalApiFail_ClusterApi);
        } catch (Exception e) {
            throw new CocktailException("fail to call Cluster API - [manageClusterRole]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
        }

        return result;
    }

    private void setUserClusterRoleIssueParams(List<Map<String, Object>> params, String changeTp, Integer accountSeq, Integer userSeq, IssueType issueType, Map<Integer, String> userRoleMap, List<UserClusterRoleIssueVO> roles) {
        if (CollectionUtils.isNotEmpty(roles)) {
            for (UserClusterRoleIssueVO roleRow : roles) {
                Map<String, Object> paramMap = Maps.newHashMap();
                paramMap.put("clusterSeq", roleRow.getClusterSeq());
                paramMap.put("userSeq", Optional.ofNullable(roleRow.getUserSeq()).orElseGet(() ->userSeq));
                paramMap.put("accountSeq", accountSeq);
                paramMap.put("clusterRole", roleRow.getIssueRole());
                paramMap.put("issueAccountName", roleRow.getIssueAccountName());
                if (StringUtils.equalsAny(changeTp, "A", "E")) {
                    if(StringUtils.isNotBlank(roleRow.getIssueConfig())) {
                        String userIamInfo = CryptoUtils.decryptAES(roleRow.getIssueConfig());
                        IssueConfigAWSVO issueConfigAWS = JsonUtils.fromGson(userIamInfo, IssueConfigAWSVO.class);
                        paramMap.put("awsUserName", issueConfigAWS.getUserName());
                    }
                }
                paramMap.put("changeTp", changeTp);
                if (IssueType.SHELL == issueType) {
                    paramMap.put("issueShellPath", roleRow.getIssueShellPath());
                } else {
                    // KUBECONFIG 만료 무기한 여부
                    paramMap.put("unLimit", BooleanUtils.toString(StringUtils.isNotBlank(roleRow.getExpirationDatetime()), "N", "Y"));
                }
                /**
                 * 바인딩유형
                 * - CLUSTER -> C
                 * - NAMESPACE -> N
                 */
                String preBindingType = this.convertBindingType(roleRow.getPreBindingType());
                String bindingType = this.convertBindingType(roleRow.getBindingType());
                // preBindingType 값이 없다면 bindingType 값으로 셋팅
                if (StringUtils.isBlank(preBindingType)) {
                    preBindingType = bindingType;
                }
                // preBindingType
                paramMap.put("preBindingType", preBindingType);
                // bindingType
                paramMap.put("bindingType", bindingType);

                // bindingType 별 셋팅
                if (IssueBindingType.valueOf(roleRow.getBindingType()) == IssueBindingType.NAMESPACE) {
                    // namespaces
                    // bindings 값이 없다면 보내지 않음.
                    if (CollectionUtils.isNotEmpty(roleRow.getBindings())) {
                        List<Map<String, String>> namespaces = Lists.newArrayList();
                        for (UserClusterRoleIssueBindingVO bindingRow : roleRow.getBindings()) {
                            Map<String, String> namespaceMap = Maps.newHashMap();
                            namespaceMap.put("name", bindingRow.getNamespace());
                            namespaceMap.put("role", bindingRow.getIssueRole());
                            namespaces.add(namespaceMap);
                        }
                        paramMap.put("namespaces", namespaces);
                        params.add(paramMap);
                    }
                } else {
                    params.add(paramMap);
                }
            }
        }
    }

    private String convertBindingType(String bindingType) {
        String convertBindingType = null;
        if (StringUtils.isNotBlank(bindingType)) {
            switch (IssueBindingType.valueOf(bindingType)) {
                case NAMESPACE:
                    convertBindingType = "N";
                    break;
                case CLUSTER:
                    convertBindingType = "C";
                    break;
            }
        }

        return convertBindingType;
    }

    private Map<String, Object> convertResponseToResult(String response, String callMethodName) throws IOException {
        Map<String, Object> result = ObjectMapperUtils.getMapper().readValue(response, new TypeReference<Map<String, Object>>(){});

        log.info("{}.{} response => {}", this.getClass().getName(), callMethodName, JsonUtils.toGson(result));

        return result;
    }

    private String genGetUrlWithParam(String url, Map<String, Object> paramMap, boolean isOptionalParam) throws Exception{
        if(!isOptionalParam && MapUtils.isEmpty(paramMap)){
            throw new CocktailException("Invalid parameter!!", ExceptionType.ExternalApiFail_ClusterApi);
        }else{
            StringBuffer paramStr = new StringBuffer();
            for(Map.Entry entryRow : paramMap.entrySet()){
                paramStr.append(String.format("%s=%s&", entryRow.getKey(), entryRow.getValue()));
            }
            return String.format("%s?%s", url, StringUtils.substringBeforeLast(paramStr.toString(), "&"));
        }
    }

//    public Map<String, Object> accessKeyValidator(ProviderCode providerCode, String jsonBody) throws CocktailException {
//        String url = String.format("%s%s%s", StringUtils.removeEnd(clusterApiProperties.getCollectorApiHost(), "/"), "/v3/audit/privilege/", providerCode.getCode());
//
//        Map<String, Object> result = Maps.newHashMap();
//        try {
//
//            List<Header> headers = Lists.newArrayList();
//            headers.add(new BasicHeader("user-id", ContextHolder.exeContext().getUserSeq().toString()));
//            headers.add(new BasicHeader("user-role", ContextHolder.exeContext().getUserRole()));
//
//            Map<String, Object> data = Maps.newHashMap();
//            data.put("data", jsonBody);
//
//            result = this.convertResponseToResult(HttpClientUtil.doPost(url, headers, ObjectMapperUtils.getMapper().writeValueAsString(data)), "accessKeyValidator");
//
//        } catch (JsonProcessingException e) {
//            throw new CocktailException("fail to call Cluster API - [accessKeyValidator]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
//        } catch (Exception e) {
//            throw new CocktailException("fail to call Cluster API - [accessKeyValidator]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
//        }
//
//        return result;
//    }

    public Map<String, Object> openshiftRoutes(String namespace, Integer clusterSeq, Integer userSeq, Integer accountSeq) throws Exception{
        String url = String.format("%s%s%s%s", StringUtils.removeEnd(clusterApiProperties.getClusterApiHost(), "/"), "/cluster/", clusterSeq.toString(), "/openshift/route");

        Map<String, Object> result = Maps.newHashMap();
        try {
            List<Header> headers = Lists.newArrayList();
            headers.add(new BasicHeader("user-id", ContextHolder.exeContext().getUserSeq().toString()));
            headers.add(new BasicHeader("user-role", ContextHolder.exeContext().getUserRole()));

            Map<String, Object> body = Maps.newHashMap();
            body.put("namespace", StringUtils.isBlank(namespace) ? "istio-system" : namespace);
            body.put("clusterSeq", clusterSeq.toString());
            body.put("accountSeq", accountSeq.toString());
            body.put("userSeq", userSeq.toString());
            body.put("userRole", ContextHolder.exeContext().getUserRole());

            result = this.convertResponseToResult(HttpClientUtil.doPost(url, headers, ObjectMapperUtils.getMapper().writeValueAsString(body)), "openshiftRoutes");
        } catch (Exception e) {
            throw new CocktailException("fail to call Cluster API - [openshiftRoutes]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
        }

        return result;
    }

    /** OpenShift SecurityContextConstrants 관련 메서드들 **/

    /**
     *
     *
     * @param clusterSeq
     * @return
     * @throws CocktailException
     */
    public Map<String, Object> listSecurityContextConstrants(Integer clusterSeq) throws CocktailException {
        // http://cluster-api:9083/cluster-api
        String clusterApiHost = StringUtils.removeEnd(clusterApiProperties.getClusterApiHost(), "/");

        // /cluster/{clusterSeq}/openshift/scc
        String path = String.format("/cluster/%s/openshift/scc", clusterSeq.toString());

        String url = String.format("%s%s", clusterApiHost, path);


        Map<String, Object> result = Maps.newHashMap();
        try {
            List<Header> headers = Lists.newArrayList();
            headers.add(new BasicHeader("user-id", ContextHolder.exeContext().getUserSeq().toString()));
            headers.add(new BasicHeader("user-role", ContextHolder.exeContext().getUserRole()));

            Map<String, Object> body = Maps.newHashMap();
            body.put("accountSeq", ContextHolder.exeContext().getUserAccountSeq());
            body.put("userSeq", ContextHolder.exeContext().getUserSeq().toString());
            body.put("userRole", ContextHolder.exeContext().getUserRole());

            // GET method 사용하기 때문에 query string 으로 값을 넘김.
            url = String.format("%s?%s", url, HttpClientUtil.urlEncodeUTF8(body));

            result = this.convertResponseToResult(HttpClientUtil.doGet(url, headers), "listSecurityContextConstrants");
        } catch (Exception e) {
            throw new CocktailException("fail to call Cluster API - ["+path+"]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
        }

        return result;
    }

    /**
     *
     *
     * @param clusterSeq
     * @return
     * @throws CocktailException
     */
    public Map<String, Object> getSecurityContextConstrants(Integer clusterSeq, String sccName) throws CocktailException {
        // http://cluster-api:9083/cluster-api
        String clusterApiHost = StringUtils.removeEnd(clusterApiProperties.getClusterApiHost(), "/");

        // GET /cluster/{clusterSeq}/openshift/scc/{name}
        String path = String.format("/cluster/%s/openshift/scc/%s", clusterSeq.toString(), sccName);

        String url = String.format("%s%s", clusterApiHost, path);


        Map<String, Object> result = Maps.newHashMap();
        try {
            List<Header> headers = Lists.newArrayList();
            headers.add(new BasicHeader("user-id", ContextHolder.exeContext().getUserSeq().toString()));
            headers.add(new BasicHeader("user-role", ContextHolder.exeContext().getUserRole()));

            Map<String, Object> body = Maps.newHashMap();
            body.put("accountSeq", ContextHolder.exeContext().getUserAccountSeq());
            body.put("userSeq", ContextHolder.exeContext().getUserSeq().toString());
            body.put("userRole", ContextHolder.exeContext().getUserRole());

            // GET method 사용하기 때문에 query string 으로 값을 넘김.
            url = String.format("%s?%s", url, HttpClientUtil.urlEncodeUTF8(body));

            result = this.convertResponseToResult(HttpClientUtil.doGet(url, headers), "getSecurityContextConstrants");
        } catch (Exception e) {
            throw new CocktailException("fail to call Cluster API - ["+path+"]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
        }
        /* response
        {
          "name": "string",
          "describeData": "string"
        }
         */

        return result;
    }


    public Map<String, Object> getSecurityContextConstrantsInNamespace(Integer clusterSeq, String namespace) throws CocktailException {
        // http://cluster-api:9083/cluster-api
        String clusterApiHost = StringUtils.removeEnd(clusterApiProperties.getClusterApiHost(), "/");

        // GET /cluster/{clusterSeq}/openshift/scc/namespace/{namespace}
        String path = String.format("/cluster/%s/openshift/scc/namespace/%s", clusterSeq.toString(), namespace);

        String url = String.format("%s%s", clusterApiHost, path);

        Map<String, Object> result = Maps.newHashMap();
        try {
            List<Header> headers = Lists.newArrayList();
            headers.add(new BasicHeader("user-id", ContextHolder.exeContext().getUserSeq().toString()));
            headers.add(new BasicHeader("user-role", ContextHolder.exeContext().getUserRole()));

            Map<String, Object> body = Maps.newHashMap();
            body.put("accountSeq", ContextHolder.exeContext().getUserAccountSeq());
            body.put("userSeq", ContextHolder.exeContext().getUserSeq().toString());
            body.put("userRole", ContextHolder.exeContext().getUserRole());

            // GET method 사용하기 때문에 query string 으로 값을 넘김.
            url = String.format("%s?%s", url, HttpClientUtil.urlEncodeUTF8(body));

            result = this.convertResponseToResult(HttpClientUtil.doGet(url, headers), "getSecurityContextConstrantsInNamespace");
        } catch (Exception e) {
            throw new CocktailException("fail to call Cluster API - ["+path+"]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
        }
        /* response
        {
          "namespace": "string",
          "roleRefs": [
            {
              "apiGroup": "string",
              "kind": "string",
              "name": "string",
              "sccName": "string"
            }
          ]
        }
         */

        return result;
    }

    public Map<String, Object> addSCC(Integer clusterSeq, String namespace, String sccName) throws CocktailException{
        // http://cluster-api:9083/cluster-api
        String clusterApiHost = StringUtils.removeEnd(clusterApiProperties.getClusterApiHost(), "/");

        // POST /cluster/{clusterSeq}/openshift/scc/add-scc-to-group/{namespace}/{name}
        String path = String.format("/cluster/%s/openshift/scc/add-scc-to-group/%s/%s", clusterSeq.toString(), namespace, sccName);

        String url = String.format("%s%s", clusterApiHost, path);

        Map<String, Object> result = Maps.newHashMap();
        try {
            List<Header> headers = Lists.newArrayList();
            headers.add(new BasicHeader("user-id", ContextHolder.exeContext().getUserSeq().toString()));
            headers.add(new BasicHeader("user-role", ContextHolder.exeContext().getUserRole()));

            Map<String, Object> body = Maps.newHashMap();
            body.put("accountSeq", ContextHolder.exeContext().getUserAccountSeq());
            body.put("userSeq", ContextHolder.exeContext().getUserSeq().toString());
            body.put("userRole", ContextHolder.exeContext().getUserRole());

            result = this.convertResponseToResult(HttpClientUtil.doPost(url, headers, ObjectMapperUtils.getMapper().writeValueAsString(body)), "addSCC");
        } catch (Exception e) {
            throw new CocktailException("fail to call Cluster API - ["+path+"]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
        }

        return result;
    }

    public Map<String, Object> deleteSCC(Integer clusterSeq, String namespace, String sccName){
        // http://cluster-api:9083/cluster-api
        String clusterApiHost = StringUtils.removeEnd(clusterApiProperties.getClusterApiHost(), "/");

        // DELETE /cluster/{clusterSeq}/openshift/scc/remove-scc-from-group/{namespace}/{name}
        String path = String.format("/cluster/%s/openshift/scc/remove-scc-from-group/%s/%s", clusterSeq.toString(), namespace, sccName);

        String url = String.format("%s%s", clusterApiHost, path);

        Map<String, Object> result = Maps.newHashMap();
        try {
            List<Header> headers = Lists.newArrayList();
            headers.add(new BasicHeader("user-id", ContextHolder.exeContext().getUserSeq().toString()));
            headers.add(new BasicHeader("user-role", ContextHolder.exeContext().getUserRole()));

            Map<String, Object> body = Maps.newHashMap();
            body.put("accountSeq", ContextHolder.exeContext().getUserAccountSeq());
            body.put("userSeq", ContextHolder.exeContext().getUserSeq().toString());
            body.put("userRole", ContextHolder.exeContext().getUserRole());

            // DELETE method 사용하기 때문에 query string 으로 값을 넘김.
            url = String.format("%s?%s", url, HttpClientUtil.urlEncodeUTF8(body));

            result = this.convertResponseToResult(HttpClientUtil.doDelete(url, headers), "deleteSCC");
        } catch (Exception e) {
            throw new CocktailException("fail to call Cluster API - ["+path+"]!!", e, ExceptionType.ExternalApiFail_ClusterApi);
        }

        return result;
    }
}
