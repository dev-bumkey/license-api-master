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
 * Created by dy79@acornsoft.io on 2017. 2. 17.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HasUseYnVO extends BaseVO {

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	protected String useYn;
	
	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	protected String created;
	
	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	protected Integer creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	protected String updated;
	
	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	protected Integer updater = Optional.ofNullable(this.updater).orElseGet(() ->ContextHolder.exeContext().getUserSeq());

	@Builder
	@SuppressWarnings("unused")
	private HasUseYnVO(String useYn, String created, Integer creator, String updated, Integer updater) {
		this.useYn = useYn;
		this.created = created;
		this.creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
		this.updated = updated;
		this.updater = Optional.ofNullable(this.updater).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
	}
}
