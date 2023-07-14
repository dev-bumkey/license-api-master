package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by choewonseog on 2017. 1. 25..
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailServiceProperties.PREFIX)
public class CocktailServiceProperties {
    public static final String PREFIX = "cocktail.service";

    private String mode;
    private String type;
    private String releaseVersion;
    private String logBasePath;
    private int logMaxHistory;
    private int k8sclientConnectTimeout;
    private boolean k8sclientDebugging;
    private String defaultLanguage;

    // cloud client info for token API of cluster
    private String gkeClientId;
    private String gkeClientSecret;
    private String aksClientId;
    private String aksClientSecret;

    private int k8sRetryMaxCount;

    private String regionTimeZone; // GMT(Default), Asia/Shanghai, Asia/Seoul, Asia/Tokyo

    private String authServerHost;
    private String authCheckUrl;

    private int auditLogPeriodMonth; // auditLog (칵테일) 보관 주기 (월 단위)

    private boolean userSleepEnabled; // 사용자 휴면 기능 활성화

    private String ncloudApiGw; // 네이버 클라우드 API Gateway 주소

}
