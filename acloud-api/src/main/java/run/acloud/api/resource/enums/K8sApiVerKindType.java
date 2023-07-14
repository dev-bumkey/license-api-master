package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;

public enum K8sApiVerKindType implements EnumCode {

	/**
	 * DEPLOYMENT
	 */
	DEPLOYMENT_V1_6 (K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_6 , K8sApiGroupType.APPS, K8sApiType.V1BETA1, "N"),
	DEPLOYMENT_V1_7 (K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_7 , K8sApiGroupType.APPS, K8sApiType.V1BETA1, "N"),
	DEPLOYMENT_V1_8 (K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_8 , K8sApiGroupType.APPS, K8sApiType.V1BETA2, "N"),
	DEPLOYMENT_V1_9 (K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_9 , K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	DEPLOYMENT_V1_10(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_10, K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	DEPLOYMENT_V1_11(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_11, K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	DEPLOYMENT_V1_12(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_12, K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	DEPLOYMENT_V1_13(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_13, K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	DEPLOYMENT_V1_14(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_14, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_15(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_15, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_16(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_16, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_17(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_17, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_18(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_18, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_19(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_19, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_20(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_20, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_21(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_21, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_22(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_22, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_23(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_23, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_24(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_24, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_25(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_25, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_26(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_26, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_27(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_27, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_28(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_28, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_29(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_29, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_30(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_30, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_31(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_31, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_32(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_32, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_33(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_33, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_34(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_34, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	DEPLOYMENT_V1_35(K8sApiKindType.DEPLOYMENT, K8sApiVerType.V1_35, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),

	/**
	 * STATEFUL_SET
	 */
	STATEFUL_SET_V1_6 (K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_6 , K8sApiGroupType.APPS, K8sApiType.V1BETA1, "N"),
	STATEFUL_SET_V1_7 (K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_7 , K8sApiGroupType.APPS, K8sApiType.V1BETA1, "N"),
	STATEFUL_SET_V1_8 (K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_8 , K8sApiGroupType.APPS, K8sApiType.V1BETA2, "N"),
	STATEFUL_SET_V1_9 (K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_9 , K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	STATEFUL_SET_V1_10(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_10, K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	STATEFUL_SET_V1_11(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_11, K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	STATEFUL_SET_V1_12(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_12, K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	STATEFUL_SET_V1_13(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_13, K8sApiGroupType.APPS, K8sApiType.V1     , "N"),
	STATEFUL_SET_V1_14(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_14, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_15(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_15, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_16(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_16, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_17(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_17, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_18(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_18, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_19(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_19, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_20(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_20, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_21(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_21, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_22(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_22, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_23(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_23, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_24(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_24, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_25(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_25, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_26(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_26, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_27(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_27, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_28(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_28, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_29(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_29, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_30(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_30, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_31(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_31, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_32(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_32, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_33(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_33, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_34(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_34, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),
	STATEFUL_SET_V1_35(K8sApiKindType.STATEFUL_SET, K8sApiVerType.V1_35, K8sApiGroupType.APPS, K8sApiType.V1     , "Y"),

	/**
	 * DAEMON_SET
	 */
	DAEMON_SET_V1_6 (K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_6 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	DAEMON_SET_V1_7 (K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_7 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	DAEMON_SET_V1_8 (K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_8 , K8sApiGroupType.APPS      , K8sApiType.V1BETA2, "N"),
	DAEMON_SET_V1_9 (K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_9 , K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	DAEMON_SET_V1_10(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_10, K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	DAEMON_SET_V1_11(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_11, K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	DAEMON_SET_V1_12(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_12, K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	DAEMON_SET_V1_13(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_13, K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	DAEMON_SET_V1_14(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_14, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_15(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_15, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_16(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_16, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_17(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_17, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_18(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_18, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_19(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_19, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_20(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_20, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_21(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_21, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_22(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_22, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_23(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_23, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_24(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_24, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_25(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_25, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_26(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_26, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_27(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_27, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_28(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_28, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_29(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_29, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_30(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_30, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_31(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_31, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_32(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_32, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_33(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_33, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_34(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_34, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	DAEMON_SET_V1_35(K8sApiKindType.DAEMON_SET, K8sApiVerType.V1_35, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),

	/**
	 * JOB
	 */
	JOB_V1_6 (K8sApiKindType.JOB, K8sApiVerType.V1_6 , K8sApiGroupType.BATCH, K8sApiType.V1, "N"),
	JOB_V1_7 (K8sApiKindType.JOB, K8sApiVerType.V1_7 , K8sApiGroupType.BATCH, K8sApiType.V1, "N"),
	JOB_V1_8 (K8sApiKindType.JOB, K8sApiVerType.V1_8 , K8sApiGroupType.BATCH, K8sApiType.V1, "N"),
	JOB_V1_9 (K8sApiKindType.JOB, K8sApiVerType.V1_9 , K8sApiGroupType.BATCH, K8sApiType.V1, "N"),
	JOB_V1_10(K8sApiKindType.JOB, K8sApiVerType.V1_10, K8sApiGroupType.BATCH, K8sApiType.V1, "N"),
	JOB_V1_11(K8sApiKindType.JOB, K8sApiVerType.V1_11, K8sApiGroupType.BATCH, K8sApiType.V1, "N"),
	JOB_V1_12(K8sApiKindType.JOB, K8sApiVerType.V1_12, K8sApiGroupType.BATCH, K8sApiType.V1, "N"),
	JOB_V1_13(K8sApiKindType.JOB, K8sApiVerType.V1_13, K8sApiGroupType.BATCH, K8sApiType.V1, "N"),
	JOB_V1_14(K8sApiKindType.JOB, K8sApiVerType.V1_14, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_15(K8sApiKindType.JOB, K8sApiVerType.V1_15, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_16(K8sApiKindType.JOB, K8sApiVerType.V1_16, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_17(K8sApiKindType.JOB, K8sApiVerType.V1_17, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_18(K8sApiKindType.JOB, K8sApiVerType.V1_18, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_19(K8sApiKindType.JOB, K8sApiVerType.V1_19, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_20(K8sApiKindType.JOB, K8sApiVerType.V1_20, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_21(K8sApiKindType.JOB, K8sApiVerType.V1_21, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_22(K8sApiKindType.JOB, K8sApiVerType.V1_22, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_23(K8sApiKindType.JOB, K8sApiVerType.V1_23, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_24(K8sApiKindType.JOB, K8sApiVerType.V1_24, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_25(K8sApiKindType.JOB, K8sApiVerType.V1_25, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_26(K8sApiKindType.JOB, K8sApiVerType.V1_26, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_27(K8sApiKindType.JOB, K8sApiVerType.V1_27, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_28(K8sApiKindType.JOB, K8sApiVerType.V1_28, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_29(K8sApiKindType.JOB, K8sApiVerType.V1_29, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_30(K8sApiKindType.JOB, K8sApiVerType.V1_30, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_31(K8sApiKindType.JOB, K8sApiVerType.V1_31, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_32(K8sApiKindType.JOB, K8sApiVerType.V1_32, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_33(K8sApiKindType.JOB, K8sApiVerType.V1_33, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_34(K8sApiKindType.JOB, K8sApiVerType.V1_34, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),
	JOB_V1_35(K8sApiKindType.JOB, K8sApiVerType.V1_35, K8sApiGroupType.BATCH, K8sApiType.V1, "Y"),

	/**
	 * CRON_JOB
	 */
	CRON_JOB_V1_6 (K8sApiKindType.CRON_JOB, K8sApiVerType.V1_6 , K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "N"),
	CRON_JOB_V1_7 (K8sApiKindType.CRON_JOB, K8sApiVerType.V1_7 , K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "N"),
	CRON_JOB_V1_8 (K8sApiKindType.CRON_JOB, K8sApiVerType.V1_8 , K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "N"),
	CRON_JOB_V1_9 (K8sApiKindType.CRON_JOB, K8sApiVerType.V1_9 , K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "N"),
	CRON_JOB_V1_10(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_10, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "N"),
	CRON_JOB_V1_11(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_11, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "N"),
	CRON_JOB_V1_12(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_12, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "N"),
	CRON_JOB_V1_13(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_13, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "N"),
	CRON_JOB_V1_14(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_14, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "Y"),
	CRON_JOB_V1_15(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_15, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "Y"),
	CRON_JOB_V1_16(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_16, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "Y"),
	CRON_JOB_V1_17(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_17, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "Y"),
	CRON_JOB_V1_18(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_18, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "Y"),
	CRON_JOB_V1_19(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_19, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "Y"),
	CRON_JOB_V1_20(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_20, K8sApiGroupType.BATCH, K8sApiType.V1BETA1, "Y"),
	CRON_JOB_V1_21(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_21, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_22(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_22, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_23(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_23, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_24(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_24, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_25(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_25, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_26(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_26, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_27(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_27, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_28(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_28, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_29(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_29, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_30(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_30, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_31(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_31, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_32(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_32, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_33(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_33, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_34(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_34, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),
	CRON_JOB_V1_35(K8sApiKindType.CRON_JOB, K8sApiVerType.V1_35, K8sApiGroupType.BATCH, K8sApiType.V1     , "Y"),

	/**
	 * REPLICA_SET
	 */
	REPLICA_SET_V1_6 (K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_6 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	REPLICA_SET_V1_7 (K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_7 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	REPLICA_SET_V1_8 (K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_8 , K8sApiGroupType.APPS      , K8sApiType.V1BETA2, "N"),
	REPLICA_SET_V1_9 (K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_9 , K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	REPLICA_SET_V1_10(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_10, K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	REPLICA_SET_V1_11(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_11, K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	REPLICA_SET_V1_12(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_12, K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	REPLICA_SET_V1_13(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_13, K8sApiGroupType.APPS      , K8sApiType.V1     , "N"),
	REPLICA_SET_V1_14(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_14, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_15(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_15, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_16(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_16, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_17(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_17, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_18(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_18, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_19(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_19, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_20(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_20, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_21(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_21, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_22(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_22, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_23(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_23, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_24(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_24, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_25(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_25, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_26(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_26, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_27(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_27, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_28(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_28, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_29(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_29, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_30(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_30, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_31(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_31, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_32(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_32, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_33(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_33, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_34(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_34, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),
	REPLICA_SET_V1_35(K8sApiKindType.REPLICA_SET, K8sApiVerType.V1_35, K8sApiGroupType.APPS      , K8sApiType.V1     , "Y"),

	/**
	 * SERVICE
	 */
	SERVICE_V1_6 (K8sApiKindType.SERVICE, K8sApiVerType.V1_6 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SERVICE_V1_7 (K8sApiKindType.SERVICE, K8sApiVerType.V1_7 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SERVICE_V1_8 (K8sApiKindType.SERVICE, K8sApiVerType.V1_8 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SERVICE_V1_9 (K8sApiKindType.SERVICE, K8sApiVerType.V1_9 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SERVICE_V1_10(K8sApiKindType.SERVICE, K8sApiVerType.V1_10, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SERVICE_V1_11(K8sApiKindType.SERVICE, K8sApiVerType.V1_11, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SERVICE_V1_12(K8sApiKindType.SERVICE, K8sApiVerType.V1_12, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SERVICE_V1_13(K8sApiKindType.SERVICE, K8sApiVerType.V1_13, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SERVICE_V1_14(K8sApiKindType.SERVICE, K8sApiVerType.V1_14, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_15(K8sApiKindType.SERVICE, K8sApiVerType.V1_15, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_16(K8sApiKindType.SERVICE, K8sApiVerType.V1_16, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_17(K8sApiKindType.SERVICE, K8sApiVerType.V1_17, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_18(K8sApiKindType.SERVICE, K8sApiVerType.V1_18, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_19(K8sApiKindType.SERVICE, K8sApiVerType.V1_19, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_20(K8sApiKindType.SERVICE, K8sApiVerType.V1_20, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_21(K8sApiKindType.SERVICE, K8sApiVerType.V1_21, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_22(K8sApiKindType.SERVICE, K8sApiVerType.V1_22, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_23(K8sApiKindType.SERVICE, K8sApiVerType.V1_23, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_24(K8sApiKindType.SERVICE, K8sApiVerType.V1_24, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_25(K8sApiKindType.SERVICE, K8sApiVerType.V1_25, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_26(K8sApiKindType.SERVICE, K8sApiVerType.V1_26, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_27(K8sApiKindType.SERVICE, K8sApiVerType.V1_27, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_28(K8sApiKindType.SERVICE, K8sApiVerType.V1_28, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_29(K8sApiKindType.SERVICE, K8sApiVerType.V1_29, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_30(K8sApiKindType.SERVICE, K8sApiVerType.V1_30, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_31(K8sApiKindType.SERVICE, K8sApiVerType.V1_31, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_32(K8sApiKindType.SERVICE, K8sApiVerType.V1_32, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_33(K8sApiKindType.SERVICE, K8sApiVerType.V1_33, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_34(K8sApiKindType.SERVICE, K8sApiVerType.V1_34, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SERVICE_V1_35(K8sApiKindType.SERVICE, K8sApiVerType.V1_35, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),

	/**
	 * NAMESPACE
	 */
	NAMESPACE_V1_6 (K8sApiKindType.NAMESPACE, K8sApiVerType.V1_6 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	NAMESPACE_V1_7 (K8sApiKindType.NAMESPACE, K8sApiVerType.V1_7 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	NAMESPACE_V1_8 (K8sApiKindType.NAMESPACE, K8sApiVerType.V1_8 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	NAMESPACE_V1_9 (K8sApiKindType.NAMESPACE, K8sApiVerType.V1_9 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	NAMESPACE_V1_10(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_10, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	NAMESPACE_V1_11(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_11, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	NAMESPACE_V1_12(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_12, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	NAMESPACE_V1_13(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_13, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	NAMESPACE_V1_14(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_14, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_15(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_15, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_16(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_16, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_17(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_17, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_18(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_18, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_19(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_19, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_20(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_20, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_21(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_21, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_22(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_22, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_23(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_23, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_24(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_24, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_25(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_25, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_26(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_26, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_27(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_27, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_28(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_28, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_29(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_29, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_30(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_30, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_31(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_31, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_32(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_32, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_33(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_33, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_34(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_34, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	NAMESPACE_V1_35(K8sApiKindType.NAMESPACE, K8sApiVerType.V1_35, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),

	/**
	 * INGRESS
	 */
	INGRESS_V1_6 (K8sApiKindType.INGRESS, K8sApiVerType.V1_6 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	INGRESS_V1_7 (K8sApiKindType.INGRESS, K8sApiVerType.V1_7 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	INGRESS_V1_8 (K8sApiKindType.INGRESS, K8sApiVerType.V1_8 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	INGRESS_V1_9 (K8sApiKindType.INGRESS, K8sApiVerType.V1_9 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	INGRESS_V1_10(K8sApiKindType.INGRESS, K8sApiVerType.V1_10, K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	INGRESS_V1_11(K8sApiKindType.INGRESS, K8sApiVerType.V1_11, K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	INGRESS_V1_12(K8sApiKindType.INGRESS, K8sApiVerType.V1_12, K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	INGRESS_V1_13(K8sApiKindType.INGRESS, K8sApiVerType.V1_13, K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N"),
	INGRESS_V1_14(K8sApiKindType.INGRESS, K8sApiVerType.V1_14, K8sApiGroupType.NETWORKING, K8sApiType.V1BETA1, "Y"),
	INGRESS_V1_15(K8sApiKindType.INGRESS, K8sApiVerType.V1_15, K8sApiGroupType.NETWORKING, K8sApiType.V1BETA1, "Y"),
	INGRESS_V1_16(K8sApiKindType.INGRESS, K8sApiVerType.V1_16, K8sApiGroupType.NETWORKING, K8sApiType.V1BETA1, "Y"),
	INGRESS_V1_17(K8sApiKindType.INGRESS, K8sApiVerType.V1_17, K8sApiGroupType.NETWORKING, K8sApiType.V1BETA1, "Y"),
	INGRESS_V1_18(K8sApiKindType.INGRESS, K8sApiVerType.V1_18, K8sApiGroupType.NETWORKING, K8sApiType.V1BETA1, "Y"),
	INGRESS_V1_19(K8sApiKindType.INGRESS, K8sApiVerType.V1_19, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_20(K8sApiKindType.INGRESS, K8sApiVerType.V1_20, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_21(K8sApiKindType.INGRESS, K8sApiVerType.V1_21, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_22(K8sApiKindType.INGRESS, K8sApiVerType.V1_22, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_23(K8sApiKindType.INGRESS, K8sApiVerType.V1_23, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_24(K8sApiKindType.INGRESS, K8sApiVerType.V1_24, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_25(K8sApiKindType.INGRESS, K8sApiVerType.V1_25, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_26(K8sApiKindType.INGRESS, K8sApiVerType.V1_26, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_27(K8sApiKindType.INGRESS, K8sApiVerType.V1_27, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_28(K8sApiKindType.INGRESS, K8sApiVerType.V1_28, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_29(K8sApiKindType.INGRESS, K8sApiVerType.V1_29, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_30(K8sApiKindType.INGRESS, K8sApiVerType.V1_30, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_31(K8sApiKindType.INGRESS, K8sApiVerType.V1_31, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_32(K8sApiKindType.INGRESS, K8sApiVerType.V1_32, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_33(K8sApiKindType.INGRESS, K8sApiVerType.V1_33, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_34(K8sApiKindType.INGRESS, K8sApiVerType.V1_34, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),
	INGRESS_V1_35(K8sApiKindType.INGRESS, K8sApiVerType.V1_35, K8sApiGroupType.NETWORKING, K8sApiType.V1     , "Y"),

	/**
	 * CONFIG_MAP
	 */
	CONFIG_MAP_V1_6 (K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_6 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	CONFIG_MAP_V1_7 (K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_7 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	CONFIG_MAP_V1_8 (K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_8 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	CONFIG_MAP_V1_9 (K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_9 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	CONFIG_MAP_V1_10(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_10, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	CONFIG_MAP_V1_11(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_11, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	CONFIG_MAP_V1_12(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_12, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	CONFIG_MAP_V1_13(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_13, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	CONFIG_MAP_V1_14(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_14, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_15(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_15, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_16(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_16, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_17(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_17, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_18(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_18, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_19(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_19, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_20(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_20, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_21(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_21, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_22(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_22, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_23(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_23, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_24(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_24, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_25(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_25, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_26(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_26, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_27(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_27, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_28(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_28, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_29(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_29, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_30(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_30, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_31(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_31, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_32(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_32, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_33(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_33, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_34(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_34, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	CONFIG_MAP_V1_35(K8sApiKindType.CONFIG_MAP, K8sApiVerType.V1_35, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),

	/**
	 * SECRET
	 */
	SECRET_V1_6 (K8sApiKindType.SECRET, K8sApiVerType.V1_6 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SECRET_V1_7 (K8sApiKindType.SECRET, K8sApiVerType.V1_7 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SECRET_V1_8 (K8sApiKindType.SECRET, K8sApiVerType.V1_8 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SECRET_V1_9 (K8sApiKindType.SECRET, K8sApiVerType.V1_9 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SECRET_V1_10(K8sApiKindType.SECRET, K8sApiVerType.V1_10, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SECRET_V1_11(K8sApiKindType.SECRET, K8sApiVerType.V1_11, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SECRET_V1_12(K8sApiKindType.SECRET, K8sApiVerType.V1_12, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SECRET_V1_13(K8sApiKindType.SECRET, K8sApiVerType.V1_13, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	SECRET_V1_14(K8sApiKindType.SECRET, K8sApiVerType.V1_14, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_15(K8sApiKindType.SECRET, K8sApiVerType.V1_15, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_16(K8sApiKindType.SECRET, K8sApiVerType.V1_16, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_17(K8sApiKindType.SECRET, K8sApiVerType.V1_17, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_18(K8sApiKindType.SECRET, K8sApiVerType.V1_18, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_19(K8sApiKindType.SECRET, K8sApiVerType.V1_19, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_20(K8sApiKindType.SECRET, K8sApiVerType.V1_20, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_21(K8sApiKindType.SECRET, K8sApiVerType.V1_21, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_22(K8sApiKindType.SECRET, K8sApiVerType.V1_22, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_23(K8sApiKindType.SECRET, K8sApiVerType.V1_23, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_24(K8sApiKindType.SECRET, K8sApiVerType.V1_24, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_25(K8sApiKindType.SECRET, K8sApiVerType.V1_25, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_26(K8sApiKindType.SECRET, K8sApiVerType.V1_26, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_27(K8sApiKindType.SECRET, K8sApiVerType.V1_27, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_28(K8sApiKindType.SECRET, K8sApiVerType.V1_28, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_29(K8sApiKindType.SECRET, K8sApiVerType.V1_29, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_30(K8sApiKindType.SECRET, K8sApiVerType.V1_30, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_31(K8sApiKindType.SECRET, K8sApiVerType.V1_31, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_32(K8sApiKindType.SECRET, K8sApiVerType.V1_32, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_33(K8sApiKindType.SECRET, K8sApiVerType.V1_33, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_34(K8sApiKindType.SECRET, K8sApiVerType.V1_34, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	SECRET_V1_35(K8sApiKindType.SECRET, K8sApiVerType.V1_35, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),

	/**
	 * PERSISTENT_VOLUME_CLAIM
	 */
	PERSISTENT_VOLUME_CLAIM_V1_6 (K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_6 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_CLAIM_V1_7 (K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_7 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_CLAIM_V1_8 (K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_8 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_CLAIM_V1_9 (K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_9 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_CLAIM_V1_10(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_10, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_CLAIM_V1_11(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_11, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_CLAIM_V1_12(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_12, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_CLAIM_V1_13(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_13, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_CLAIM_V1_14(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_14, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_15(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_15, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_16(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_16, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_17(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_17, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_18(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_18, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_19(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_19, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_20(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_20, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_21(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_21, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_22(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_22, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_23(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_23, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_24(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_24, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_25(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_25, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_26(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_26, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_27(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_27, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_28(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_28, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_29(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_29, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_30(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_30, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_31(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_31, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_32(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_32, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_33(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_33, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_34(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_34, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_CLAIM_V1_35(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerType.V1_35, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),

	/**
	 * PERSISTENT_VOLUME
	 */
	PERSISTENT_VOLUME_V1_6 (K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_6 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_V1_7 (K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_7 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_V1_8 (K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_8 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_V1_9 (K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_9 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_V1_10(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_10, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_V1_11(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_11, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_V1_12(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_12, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_V1_13(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_13, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	PERSISTENT_VOLUME_V1_14(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_14, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_15(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_15, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_16(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_16, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_17(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_17, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_18(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_18, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_19(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_19, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_20(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_20, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_21(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_21, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_22(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_22, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_23(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_23, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_24(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_24, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_25(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_25, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_26(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_26, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_27(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_27, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_28(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_28, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_29(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_29, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_30(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_30, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_31(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_31, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_32(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_32, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_33(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_33, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_34(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_34, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	PERSISTENT_VOLUME_V1_35(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerType.V1_35, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),

	/**
	 * HORIZONTAL_POD_AUTOSCALER
	 */
	HORIZONTAL_POD_AUTOSCALER_V1_6 (K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_6 , K8sApiGroupType.AUTOSCALING, K8sApiType.V1     , "N"),
	HORIZONTAL_POD_AUTOSCALER_V1_7 (K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_7 , K8sApiGroupType.AUTOSCALING, K8sApiType.V1     , "N"),
	HORIZONTAL_POD_AUTOSCALER_V1_8 (K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_8 , K8sApiGroupType.AUTOSCALING, K8sApiType.V1     , "N"),
	HORIZONTAL_POD_AUTOSCALER_V1_9 (K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_9 , K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA1, "N"),
	HORIZONTAL_POD_AUTOSCALER_V1_10(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_10, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA1, "N"),
	HORIZONTAL_POD_AUTOSCALER_V1_11(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_11, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA1, "N"),
	HORIZONTAL_POD_AUTOSCALER_V1_12(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_12, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA1, "N"),
	HORIZONTAL_POD_AUTOSCALER_V1_13(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_13, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "N"),
	HORIZONTAL_POD_AUTOSCALER_V1_14(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_14, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_15(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_15, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_16(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_16, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_17(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_17, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_18(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_18, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_19(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_19, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_20(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_20, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_21(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_21, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_22(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_22, K8sApiGroupType.AUTOSCALING, K8sApiType.V2BETA2, "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_23(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_23, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_24(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_24, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_25(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_25, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_26(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_26, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_27(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_27, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_28(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_28, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_29(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_29, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_30(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_30, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_31(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_31, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_32(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_32, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_33(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_33, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_34(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_34, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),
	HORIZONTAL_POD_AUTOSCALER_V1_35(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerType.V1_35, K8sApiGroupType.AUTOSCALING, K8sApiType.V2     , "Y"),

	/**
	 * STORAGE_CLASS
	 */
	STORAGE_CLASS_V1_6 (K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_6 , K8sApiGroupType.STORAGE, K8sApiType.V1, "N"),
	STORAGE_CLASS_V1_7 (K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_7 , K8sApiGroupType.STORAGE, K8sApiType.V1, "N"),
	STORAGE_CLASS_V1_8 (K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_8 , K8sApiGroupType.STORAGE, K8sApiType.V1, "N"),
	STORAGE_CLASS_V1_9 (K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_9 , K8sApiGroupType.STORAGE, K8sApiType.V1, "N"),
	STORAGE_CLASS_V1_10(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_10, K8sApiGroupType.STORAGE, K8sApiType.V1, "N"),
	STORAGE_CLASS_V1_11(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_11, K8sApiGroupType.STORAGE, K8sApiType.V1, "N"),
	STORAGE_CLASS_V1_12(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_12, K8sApiGroupType.STORAGE, K8sApiType.V1, "N"),
	STORAGE_CLASS_V1_13(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_13, K8sApiGroupType.STORAGE, K8sApiType.V1, "N"),
	STORAGE_CLASS_V1_14(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_14, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_15(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_15, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_16(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_16, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_17(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_17, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_18(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_18, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_19(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_19, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_20(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_20, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_21(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_21, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_22(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_22, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_23(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_23, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_24(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_24, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_25(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_25, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_26(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_26, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_27(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_27, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_28(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_28, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_29(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_29, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_30(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_30, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_31(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_31, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_32(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_32, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_33(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_33, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_34(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_34, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),
	STORAGE_CLASS_V1_35(K8sApiKindType.STORAGE_CLASS, K8sApiVerType.V1_35, K8sApiGroupType.STORAGE, K8sApiType.V1, "Y"),

	/**
	 * CUSTOM_RESOURCE_DEFINITION
	 */
	CUSTOM_RESOURCE_DEFINITION_V1_6 (K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_6 , K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "N"),
	CUSTOM_RESOURCE_DEFINITION_V1_7 (K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_7 , K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "N"),
	CUSTOM_RESOURCE_DEFINITION_V1_8 (K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_8 , K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "N"),
	CUSTOM_RESOURCE_DEFINITION_V1_9 (K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_9 , K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "N"),
	CUSTOM_RESOURCE_DEFINITION_V1_10(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_10, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "N"),
	CUSTOM_RESOURCE_DEFINITION_V1_11(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_11, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "N"),
	CUSTOM_RESOURCE_DEFINITION_V1_12(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_12, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "N"),
	CUSTOM_RESOURCE_DEFINITION_V1_13(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_13, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "N"),
	CUSTOM_RESOURCE_DEFINITION_V1_14(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_14, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_15(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_15, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1BETA1, "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_16(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_16, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_17(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_17, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_18(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_18, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_19(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_19, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_20(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_20, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_21(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_21, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_22(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_22, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_23(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_23, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_24(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_24, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_25(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_25, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_26(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_26, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_27(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_27, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_28(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_28, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_29(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_29, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_30(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_30, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_31(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_31, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_32(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_32, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_33(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_33, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_34(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_34, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),
	CUSTOM_RESOURCE_DEFINITION_V1_35(K8sApiKindType.CUSTOM_RESOURCE_DEFINITION, K8sApiVerType.V1_35, K8sApiGroupType.API_EXTENSIONS, K8sApiType.V1     , "Y"),

	/**
	 * RESOURCE_QUOTA
	 */
	RESOURCE_QUOTA_V1_6 (K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_6 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	RESOURCE_QUOTA_V1_7 (K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_7 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	RESOURCE_QUOTA_V1_8 (K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_8 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	RESOURCE_QUOTA_V1_9 (K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_9 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	RESOURCE_QUOTA_V1_10(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_10, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	RESOURCE_QUOTA_V1_11(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_11, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	RESOURCE_QUOTA_V1_12(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_12, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	RESOURCE_QUOTA_V1_13(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_13, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	RESOURCE_QUOTA_V1_14(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_14, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_15(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_15, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_16(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_16, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_17(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_17, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_18(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_18, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_19(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_19, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_20(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_20, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_21(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_21, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_22(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_22, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_23(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_23, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_24(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_24, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_25(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_25, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_26(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_26, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_27(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_27, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_28(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_28, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_29(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_29, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_30(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_30, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_31(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_31, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_32(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_32, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_33(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_33, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_34(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_34, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	RESOURCE_QUOTA_V1_35(K8sApiKindType.RESOURCE_QUOTA, K8sApiVerType.V1_35, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),

	/**
	 * LIMIT_RANGE
	 */
	LIMIT_RANGE_V1_6 (K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_6 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	LIMIT_RANGE_V1_7 (K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_7 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	LIMIT_RANGE_V1_8 (K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_8 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	LIMIT_RANGE_V1_9 (K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_9 , K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	LIMIT_RANGE_V1_10(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_10, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	LIMIT_RANGE_V1_11(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_11, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	LIMIT_RANGE_V1_12(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_12, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	LIMIT_RANGE_V1_13(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_13, K8sApiGroupType.CORE, K8sApiType.V1, "N"),
	LIMIT_RANGE_V1_14(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_14, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_15(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_15, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_16(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_16, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_17(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_17, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_18(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_18, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_19(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_19, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_20(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_20, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_21(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_21, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_22(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_22, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_23(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_23, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_24(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_24, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_25(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_25, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_26(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_26, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_27(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_27, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_28(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_28, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_29(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_29, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_30(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_30, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_31(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_31, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_32(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_32, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_33(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_33, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_34(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_34, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),
	LIMIT_RANGE_V1_35(K8sApiKindType.LIMIT_RANGE, K8sApiVerType.V1_35, K8sApiGroupType.CORE, K8sApiType.V1, "Y"),

	/**
	 * NETWORK_POLICY
	 */
	NETWORK_POLICY_V1_6 (K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_6 , K8sApiGroupType.NETWORKING, K8sApiType.V1, "N"),
	NETWORK_POLICY_V1_7 (K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_7 , K8sApiGroupType.NETWORKING, K8sApiType.V1, "N"),
	NETWORK_POLICY_V1_8 (K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_8 , K8sApiGroupType.NETWORKING, K8sApiType.V1, "N"),
	NETWORK_POLICY_V1_9 (K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_9 , K8sApiGroupType.NETWORKING, K8sApiType.V1, "N"),
	NETWORK_POLICY_V1_10(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_10, K8sApiGroupType.NETWORKING, K8sApiType.V1, "N"),
	NETWORK_POLICY_V1_11(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_11, K8sApiGroupType.NETWORKING, K8sApiType.V1, "N"),
	NETWORK_POLICY_V1_12(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_12, K8sApiGroupType.NETWORKING, K8sApiType.V1, "N"),
	NETWORK_POLICY_V1_13(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_13, K8sApiGroupType.NETWORKING, K8sApiType.V1, "N"),
	NETWORK_POLICY_V1_14(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_14, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_15(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_15, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_16(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_16, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_17(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_17, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_18(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_18, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_19(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_19, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_20(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_20, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_21(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_21, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_22(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_22, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_23(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_23, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_24(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_24, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_25(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_25, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_26(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_26, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_27(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_27, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_28(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_28, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_29(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_29, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_30(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_30, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_31(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_31, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_32(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_32, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_33(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_33, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_34(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_34, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),
	NETWORK_POLICY_V1_35(K8sApiKindType.NETWORK_POLICY, K8sApiVerType.V1_35, K8sApiGroupType.NETWORKING, K8sApiType.V1, "Y"),

	/**
	 * Pod Security Policy
	 */
	  POD_SECURITY_POLICY_V1_6 (K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_6 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N")
	, POD_SECURITY_POLICY_V1_7 (K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_7 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N")
	, POD_SECURITY_POLICY_V1_8 (K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_8 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N")
	, POD_SECURITY_POLICY_V1_9 (K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_9 , K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N")
	, POD_SECURITY_POLICY_V1_10(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_10, K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N")
	, POD_SECURITY_POLICY_V1_11(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_11, K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N")
	, POD_SECURITY_POLICY_V1_12(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_12, K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N")
	, POD_SECURITY_POLICY_V1_13(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_13, K8sApiGroupType.EXTENSIONS, K8sApiType.V1BETA1, "N")
	, POD_SECURITY_POLICY_V1_14(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_14, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_15(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_15, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_16(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_16, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_17(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_17, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_18(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_18, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_19(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_19, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_20(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_20, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_21(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_21, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_22(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_22, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_23(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_23, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
	, POD_SECURITY_POLICY_V1_24(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_24, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "Y")
//	, POD_SECURITY_POLICY_V1_25(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_25, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_26(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_26, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_27(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_27, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_28(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_28, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_29(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_29, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_30(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_30, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_31(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_31, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_32(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_32, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_33(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_33, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_34(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_34, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")
//	, POD_SECURITY_POLICY_V1_35(K8sApiKindType.POD_SECURITY_POLICY, K8sApiVerType.V1_35, K8sApiGroupType.POLICY    , K8sApiType.V1BETA1, "N")

	;

	@Getter
	private K8sApiKindType kindType;

	@Getter
	private K8sApiVerType verType;

	@Getter
	private K8sApiGroupType groupType;

	@Getter
	private K8sApiType apiType;

	@Getter
	private String useYn;

	K8sApiVerKindType(K8sApiKindType kindType, K8sApiVerType verType, K8sApiGroupType groupType, K8sApiType apiType, String useYn) {
		this.kindType = kindType;
		this.verType = verType;
		this.groupType = groupType;
		this.apiType = apiType;
		this.useYn = useYn;
	}

	public static K8sApiVerKindType getApiType(K8sApiKindType kindType, K8sApiVerType verType){
		return Arrays.stream(K8sApiVerKindType.values()).filter(vk -> (BooleanUtils.toBoolean(vk.getVerType().getUseYn()) && vk.getKindType() == kindType && vk.getVerType() == verType))
				.findFirst()
				.orElseGet(() ->null);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
