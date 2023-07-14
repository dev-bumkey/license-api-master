package run.acloud.commons.vo;

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
public class HasUpserterVO extends BaseVO {

	protected String created;
	
	protected Integer creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
	
	protected String updated;
	
	protected Integer updater = Optional.ofNullable(this.updater).orElseGet(() ->ContextHolder.exeContext().getUserSeq());

	@Builder
	@SuppressWarnings("unused")
	private HasUpserterVO(String created, Integer creator, String updated, Integer updater) {
		this.created = created;
		this.creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
		this.updated = updated;
		this.updater = Optional.ofNullable(this.updater).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
	}
}
