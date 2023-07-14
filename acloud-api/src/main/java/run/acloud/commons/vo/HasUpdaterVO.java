package run.acloud.commons.vo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import run.acloud.framework.context.ContextHolder;

import java.util.Optional;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 25.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HasUpdaterVO extends BaseVO {
	
	protected String updated;

	protected Integer updater = Optional.ofNullable(this.updater).orElseGet(() ->ContextHolder.exeContext().getUserSeq());

	@Builder
	@SuppressWarnings("unused")
	private HasUpdaterVO(String updated, Integer updater) {
		this.updated = updated;
		this.updater = Optional.ofNullable(this.updater).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
	}
}
