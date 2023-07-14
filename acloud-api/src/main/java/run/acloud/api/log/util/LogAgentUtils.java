package run.acloud.api.log.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.kubernetes.client.openapi.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import run.acloud.api.log.constant.LogAgentConstants;
import run.acloud.api.log.vo.LogAgentAccountMappingVO;
import run.acloud.api.log.vo.LogAgentVO;
import run.acloud.api.log.vo.LogAgentViewVO;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.properties.CocktailLogAgentProperties;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LogAgentUtils {
    public static String logAgentAddVOToDeployConfigYAML(LogAgentVO logAgent, CocktailLogAgentProperties cocktailLogAgentProperties) throws Exception{
        /*
          daemonSetVolumes:
              - name: cocktail-application-log
                hostPath:
                    path: "", (hostPath)
          daemonSetVolumeMounts:
              - name: cocktail-application-log
                mountPath: /var/log/app,
          resources:
              requests:
                cpu: 100m
                memory: 300Mi
              limits:
                cpu: 200m
                memory: 400Mi
          nodeSelector: {}
          tolerations: []
          affinity: {}
          flush : 1
          logLevel: info (logLevel)
          config:
              inputs: ""
              outputs: |
                  [OUTPUT]
                      name  loki
                      match *
                      host {{ .Values.lokiHost }}
                      port {{ .Values.lokiPort }}
                      bearer_token {{ .Values.lokiToken }}
                      labels scrape_job=cocktail-application-log, application={{ .Values.lokiApplicationName }}, {{ .Values.lokiCustomLabels }}
              customParsers: ""
          lokiHost: cocktail-logs-loki-distributed-gateway.cocktail-apm
          lokiPort: 8980
          lokiToken: ""
          lokiApplicationName: ""
          lokiCustomLabels: ""
         */
        JSON k8sJson = new JSON();
        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
        DocumentContext documentContext = JsonPath.using(conf).parse(LogAgentConstants.DEPLOY_CONFIG);

        // 들여쓰기가 존재하는 빈라인일 경우 단순 빈라인으로 변경한다.
        Function<String, String> removeIndentEmptyLine = line -> StringUtils.isBlank(line) ? "" : line;

        // 라인에 tab이 존재할 경우 공백4개로 대체한다.
        Function<String, String> replaceTab = line -> line.replace("\t", "    ");

        // 위 함수들을 적용시켜서 멀티라인 문자열을 생성
        Function<String, String> makeMultilineStringYAML = config -> Arrays.stream(config.split("\n"))
                .map(removeIndentEmptyLine)
                .map(replaceTab)
                .collect(Collectors.joining("\n"));

        // logAgent 객체의 정보를 이용해서 yaml 문자열 만들기 로직
        // 필수값들은 상단에 먼저 작업한다.
        documentContext.set("lokiHost", cocktailLogAgentProperties.getLogAgentLogPushUrl());
        documentContext.set("lokiPort", cocktailLogAgentProperties.getLogAgentLogPushPort());
        documentContext.set("daemonSetVolumes[0].hostPath.path", logAgent.getHostPath());
        documentContext.set("lokiApplicationName", logAgent.getApplicationName());
        documentContext.set("config.inputs", makeMultilineStringYAML.apply(logAgent.getAgentConfig()));
        documentContext.set("config.outputs", LogAgentConstants.LOKI_OUTPUT_DEPLOY_CONFIG);
        documentContext.set("lokiToken", CryptoUtils.decryptDefaultAES(logAgent.getToken()));

        if (logAgent.getCpuRequest() != null) {
            documentContext.set("resources.requests.cpu", logAgent.getCpuRequest()+LogAgentConstants.RESOURCE_CPU);
        }

        if(logAgent.getCpuLimit() != null) {
            documentContext.set("resources.limits.cpu", logAgent.getCpuLimit()+LogAgentConstants.RESOURCE_CPU);
        }

        if(logAgent.getMemoryRequest() != null) {
            documentContext.set("resources.requests.memory", logAgent.getMemoryRequest()+LogAgentConstants.RESOURCE_MEMORY);
        }

        if(logAgent.getMemoryLimit() != null) {
            documentContext.set("resources.limits.memory", logAgent.getMemoryLimit()+LogAgentConstants.RESOURCE_MEMORY);
        }

        if (StringUtils.isNotBlank(logAgent.getNodeSelector())) {
            documentContext.set("nodeSelector", getYamlValue(logAgent.getNodeSelector(), new TypeReference<Map<String, String>>() {}, k8sJson));
        }

        if (StringUtils.isNotBlank(logAgent.getTolerations())) {
            documentContext.set("tolerations", getYamlValue(logAgent.getTolerations(), new TypeReference<List<Map<String, Object>>>() {}, k8sJson));
        }

        if (StringUtils.isNotBlank(logAgent.getAffinity())) {
            documentContext.set("affinity", getYamlValue(logAgent.getAffinity(), new TypeReference<Map<String, Object>>() {}, k8sJson));
        }

        if (StringUtils.isNotBlank(logAgent.getLogLevel())) {
            documentContext.set("logLevel", logAgent.getLogLevel());
        }

        if (StringUtils.isNotBlank(logAgent.getParserConfig())) {
            documentContext.set("config.customParsers", makeMultilineStringYAML.apply(logAgent.getParserConfig()));
        }

        if (logAgent.getLokiCustomLabels() != null && !logAgent.getLokiCustomLabels().isEmpty()) {
            documentContext.set("lokiCustomLabels", labelMapToString(logAgent.getLokiCustomLabels()));
        }

        Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(documentContext.jsonString(), new TypeReference<>() {});
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);

        return yaml.dumpAsMap(valueMap);
    }

    private static String labelMapToString(Map<String, String> labelMap) {
        Function<Map.Entry<String, String>, String> getLabel = (Map.Entry<String, String> label) -> {
            // 라벨 이름이 미리 정의된 라벨 목록과 동일하다면 prefix를 붙인다.
            String labelKey = addPrefixIfExistPredefinedLabelKey(label.getKey().trim(), "user_");
            String labelValue = label.getValue().trim();

            // 라벨 값이 공백이 아니라면 'key=value' 형태로 반환, 공백이라면 'key' 형태로 반환
            if (StringUtils.isNotBlank(labelValue)) return labelKey + "=" + labelValue;
            return labelKey;
        };

        // labelMap에 위 함수를 적용시키고 ','로 합친다. Set<Map.Entry<String, String>> -> Set<String> -> String
        return labelMap.entrySet().stream().map(getLabel).collect(Collectors.joining(","));
    }

    private static String addPrefixIfExistPredefinedLabelKey(String labelKey, String prefix) {
        // 미리 정해진 라벨 목록 중 하나와 동일한지 찾는다.
        Optional<String> existLabel = Arrays.stream(LogAgentConstants.LOKI_REQUIRE_LABELS)
                .filter(label -> label.equals(labelKey))
                .findFirst();

        // 만약 미리 정해진 라벨과 동일하다면 prefix를 붙여서 반환한다.
        if (existLabel.isPresent()) {
            return prefix + labelKey;
        }

        // 동일한 라벨이 없다면 기존 라벨 이름을 반환한다.
        return labelKey;
    }

    public static LogAgentViewVO deployConfigYamlToLogAgentVO(LogAgentVO logAgent) throws Exception {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        Object o = yaml.loadAs(logAgent.getDeployConfig(), Map.class);
        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
        DocumentContext documentContext = JsonPath.using(conf).parse(o);

        // yaml 값을 map 객체에 넣어서 두번째 인수로 받은 메소드에 넣어줌
        BiConsumer<String, Consumer<Map<String, Object>>> mapSetter = (key, f) -> {
            Map<String, Object> map = Maps.newHashMap();
            map.put(key, documentContext.read(key));
            f.accept(map);
        };

        // yaml 값을 다시 logAgentView에 세팅하는 로직
        // 변환하면서 생기는 불필요한 escape string들은 제거한다.
        logAgent.setCpuRequest(getResource(documentContext.read("resources.requests.cpu"), LogAgentConstants.RESOURCE_CPU));
        logAgent.setCpuLimit(getResource(documentContext.read("resources.limits.cpu"), LogAgentConstants.RESOURCE_CPU));
        logAgent.setMemoryRequest(getResource(documentContext.read("resources.requests.memory"), LogAgentConstants.RESOURCE_MEMORY));
        logAgent.setMemoryLimit(getResource(documentContext.read("resources.limits.memory"), LogAgentConstants.RESOURCE_MEMORY));
        mapSetter.accept("affinity", affinityMap -> logAgent.setAffinity(yaml.dumpAsMap(affinityMap)));
        mapSetter.accept("tolerations", tolerationsMap -> logAgent.setTolerations(yaml.dumpAsMap(tolerationsMap)));
        mapSetter.accept("nodeSelector", nodeSelectorMap -> logAgent.setNodeSelector(yaml.dumpAsMap(nodeSelectorMap)));
        logAgent.setHostPath(yaml.dump(documentContext.read("daemonSetVolumes[0].hostPath.path")).trim());
        logAgent.setLogLevel(yaml.dump(documentContext.read("logLevel")).trim());
        logAgent.setAgentConfig(parseYamlMultilineString(yaml.dump(documentContext.read("config.inputs"))));
        logAgent.setParserConfig(parseYamlMultilineString(yaml.dump(documentContext.read("config.customParsers"))));
        logAgent.setLokiCustomLabels(labelStringToMap(yaml.dump(documentContext.read("lokiCustomLabels")).trim()));

        return logAgent;
    }

    private static String parseYamlMultilineString(String value) {
        // yaml을 파싱한 결과가 block 스타일의 지시어가 없는 단순 문자열인 경우
        if (value.contains("\"")) {
            // 1. '"' 제거 후 문자열 내부에 있는 \\n을 기준으로 분리하기 위해 \n를 제거한다.
            // 2. 문자열 내부에 각 라인 연결을 위한 이스케이프 "\\ "를 제거한다.
            // 3. \t이 포함된 경우 제거한다.
            // 3. 들여쓰기가 포함된 빈 라인인 경우, 단순 빈 라인으로 변경한다.
            // 4. 각 라인을 다시 \n로 연결한다.
            return Arrays.stream(value.replace("\"", "").replace("\n", "").split("\\\\n"))
                    .map(s -> s.replaceAll("\\\\\s", ""))
                    .map(s -> s.replace("\t", ""))
                    .map(s -> StringUtils.isBlank(s) ? "" : s)
                    .collect(Collectors.joining("\n"));
        }

        // yaml 멀티라인 문자열을 파싱하면 각 라인 맨 앞에 생기는 공백 2칸을 제거하는 함수
        Function<String, String> removeIndentIfExist = s -> {
            if (s.length() > 2) {
                return s.substring(2);
            }

            // 빈라인인 경우 그대로 반환
            return s;
        };

        // yaml 멀티라인 문자열을 파싱할 때 추가되는 공백 2칸을 제거하고 다시 합친다.
        // yaml block 스타일 지시어를 작업 전에 제거하고 시작
        return Arrays.stream(removeBlockIndicator(value).split("\n"))
                .map(removeIndentIfExist)
                .collect(Collectors.joining("\n"));
    }

    private static String removeBlockIndicator(String value) {
        // block 스타일 파싱일 때 생성되는 지시어 제거
        return value.replaceAll("\\|.?\n", "");
    }

    private static Map<String, String> labelStringToMap(String labelString) {
        // 라벨 문자열을 Map으로 변환한다. ex. 라벨 문자열은 다음과 같은 형식임 "key1=value1,$key, key2=value2"
        // 라벨 문자열이 비어있다면 빈 Map 객체 반환
        if (StringUtils.isBlank(labelString) || labelString.equals("''")) return new HashMap<>();

        // Map으로 변환하면서 라벨 value가 없는 경우 빈 문자열을 반환해주는 value mapper
        Function<String[], String> valueMapper = (String[] keyValue) -> keyValue.length > 1 ? keyValue[1] : "";

        return Arrays.stream(labelString.split(","))
                .map(labelKeyValue -> labelKeyValue.split("="))
                .collect(Collectors.toMap(labelKV -> labelKV[0], valueMapper));
    }

    public static Integer getResource(String resource, String separator) {
        if(StringUtils.isBlank(resource)){
            return null;
        }
        return NumberUtils.toInt(StringUtils.substringBeforeLast(resource, separator));
    }

    public static LogAgentVO copyLogAgentVO(LogAgentViewVO view) {
        LogAgentVO logAgent = new LogAgentVO();
        logAgent.setAccountSeq(view.getAccountSeq());
        logAgent.setAgentSeq(view.getAgentSeq());
        logAgent.setAgentName(view.getAgentName());
        logAgent.setAgentDescription(view.getAgentDescription());
        logAgent.setAgentConfig(view.getAgentConfig());
        logAgent.setParserConfig(view.getParserConfig());
        logAgent.setClusterSeq(view.getClusterSeq());
        logAgent.setNamespace(view.getNamespace());
        logAgent.setApplicationName(view.getApplicationName());
        logAgent.setHostPath(view.getHostPath());
        logAgent.setLogLevel(view.getLogLevel());
        logAgent.setLokiCustomLabels(view.getLokiCustomLabels());
        return logAgent;
    }

    public static LogAgentAccountMappingVO getAgentAccountMapping(LogAgentVO logAgent) {
        LogAgentAccountMappingVO logAgentAccountMapping = new LogAgentAccountMappingVO();
        logAgentAccountMapping.setAccountSeq(logAgent.getAccountSeq());
        logAgentAccountMapping.setLogAgentSeq(logAgent.getAgentSeq());
        return logAgentAccountMapping;
    }

    public static <T> T getYamlValue(String content, TypeReference<T> valueTypeRef, JSON k8sJson) throws Exception {
        if (StringUtils.isNotBlank(content)) {
            if (k8sJson == null) {
                k8sJson = new JSON();
            }

            // spec 체크
            Object k8sObj = run.acloud.api.k8sextended.util.Yaml.getSnakeYaml().load(content);
            String valueStr = k8sJson.serialize(k8sObj);
            return ObjectMapperUtils.getMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).setSerializationInclusion(JsonInclude.Include.NON_NULL).readValue(valueStr, valueTypeRef);
        } else {
            return null;
        }
    }
}
