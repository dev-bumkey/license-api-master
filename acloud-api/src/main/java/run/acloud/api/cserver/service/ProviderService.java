package run.acloud.api.cserver.service;

import org.springframework.stereotype.Service;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.api.resource.vo.ProviderVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 2. 16.
 */
@Service
public class ProviderService {

	public List<ProviderVO> getProviders() {
		return ProviderCode.getProviderList();
	}
	
	public List<CodeVO> getProviderCodes() throws Exception {
		return ProviderCode.getCodeList();
	}
}
