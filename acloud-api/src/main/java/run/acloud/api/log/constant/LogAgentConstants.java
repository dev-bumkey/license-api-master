package run.acloud.api.log.constant;

public class LogAgentConstants {
    public static final String RESOURCE_CPU = "m";
    public static final String RESOURCE_MEMORY = "Mi";

    public static final String DEPLOY_CONFIG = "{" +
            "'daemonSetVolumes': [{'name': 'cocktail-application-log', 'hostPath': {'path': ''}}]," +
            "'daemonSetVolumeMounts': [{'name': 'cocktail-application-log', 'mountPath': '/var/log/app'}]," +
            "'resources':{'requests':{" +
            "               'cpu':'100m'," +
            "               'memory':'256Mi'}," +
            "             'limits':{" +
            "               'cpu':'300m'," +
            "               'memory':'512Mi'}}," +
            "'nodeSelector':{}," +
            "'tolerations':[]," +
            "'affinity':{}," +
            "'flush': 1," +
            "'logLevel': 'info'," +
            "'config': {'inputs': '', " +
            "           'outputs': '',"+
            "           'customParsers': ''}," +
            "'lokiHost': ''," +
            "'lokiPort': ''," +
            "'lokiToken': ''," +
            "'lokiApplicationName': ''," +
            "'lokiCustomLabels': ''" +
            "}";
    public static final String LOKI_OUTPUT_DEPLOY_CONFIG = """
            [OUTPUT]
                name loki
                match *
                host {{ .Values.lokiHost }}
                port {{ .Values.lokiPort }}
                bearer_token {{ .Values.lokiToken }}
                labels scrape_job=cocktail-application-log, application={{ .Values.lokiApplicationName }} {{- if .Values.lokiCustomLabels }} , {{ .Values.lokiCustomLabels }} {{- end}}""";

    public static final String[] LOKI_REQUIRE_LABELS = new String[] {"scrape_job", "application"};

}
