package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "템플릿버전 삭제 모델")
public class TemplateVersionDelVO {

	private Integer templateSeq; // 템플릿번호
	private Integer templateVersionSeqForDel; // 삭제할 템플릿버전번호
	private String latestYnForDel; // 삭제할 템플릿버전 최신여부
	private Integer latestTemplateVersionSeq; // 최신 템플릿버전번호
	private Integer versionTotalCount; // 템플릿 버전 총 갯수
	
}
