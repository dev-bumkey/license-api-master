package run.acloud.commons.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import run.acloud.framework.context.ContextHolder;

import java.util.Optional;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 18.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HasCreatorVO extends BaseVO {

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	protected String created;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	protected Integer creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());

	@Builder
	@SuppressWarnings("unused")
	private HasCreatorVO(String created, Integer creator) {
		this.created = created;
		this.creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
	}
}
