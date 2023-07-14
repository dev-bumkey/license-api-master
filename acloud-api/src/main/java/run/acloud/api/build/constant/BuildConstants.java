package run.acloud.api.build.constant;

import java.io.File;

public class BuildConstants {

	public static final String FILE_SEPARATOR = File.separator;
	public static final String COCKTAIL_BUILD_IMAGE_TAG_FORMAT = "%s:%s";

	public static final String DEFAULT_NAT_CLIENT_ID = "api-server";

	/** BUILD LOG SUBJECT
	 * @update 20230530, coolingi, pub/sub 연동을 위한 logId 규칙 변경
	 **/
	public static final String SUBJECT_PRE_FIXED = "%s-%d.%d"; // ResourcePrefix-BuildSeq.BuildRunSeq,
	public static final String SUBJECT_STEP_LOG  = "%s.%d-%s"; // {SUBJECT_PRE_FIXED}.BuildStepRunSeq-{lower case of stepType}

	/** Create Image Default Title **/
	public static final String CREATE_IMAGE_STEP_DEFAULT_TITLE = "Build & Push";


}
