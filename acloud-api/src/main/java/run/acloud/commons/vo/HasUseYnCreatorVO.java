package run.acloud.commons.vo;

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
public class HasUseYnCreatorVO extends BaseVO {
	
	protected String useYn;
	
	protected String created;
	
	protected Integer creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());

	@Builder
	@SuppressWarnings("unused")
	private HasUseYnCreatorVO(String useYn, String created, Integer creator) {
		this.useYn = useYn;
		this.created = created;
		this.creator = Optional.ofNullable(this.creator).orElseGet(() ->ContextHolder.exeContext().getUserSeq());
	}
}
