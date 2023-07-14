package run.acloud.api.code.service;

import jakarta.annotation.Resource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.code.dao.ICodeMapper;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.code.vo.SubCodeVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io on 2017. 3. 13.
 */
@Service
public class CodeService {
	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;
	
	public List<CodeVO> getCodes(String groupId) {
		ICodeMapper dao = sqlSession.getMapper(ICodeMapper.class);

		switch (groupId) {
			case "USER_ROLE":
				return this.getCodesByEnum(groupId);
			default:
				return dao.getCodes(groupId);
		}
	}
	
	public List<SubCodeVO> getSubCodes(String groupId, String subGroupId) {
		ICodeMapper dao = sqlSession.getMapper(ICodeMapper.class);
        return dao.getSubCodes(groupId, subGroupId);
	}

	public CodeVO getCode(String groupId, String code) {
	    ICodeMapper dao = sqlSession.getMapper(ICodeMapper.class);
		switch (groupId) {
			case "USER_ROLE":
				return this.getCodeByEnum(groupId, code);
			default:
				return dao.getCode(groupId, code);
		}
    }

	public CodeVO getCodeByEnum(String groupId, String code) {
		switch (groupId) {
			case "USER_ROLE":
				return UserRole.getCodeVO(code);
			default:
				return null;
		}
    }

	public List<CodeVO> getCodesByEnum(String groupId) {
		switch (groupId) {
			case "USER_ROLE":
				return UserRole.getCodeVOs();
			default:
				return null;
		}
	}

	@Cacheable(value="resourcePrefix")
	public CodeVO getCodeResourcePrefix() {
	    ICodeMapper dao = sqlSession.getMapper(ICodeMapper.class);

	    return dao.getCode("COCKTAIL_CONFIG", "RESOURCE_PREFIX");
    }

	@Transactional(transactionManager = "transactionManager")
	public int addCode(CodeVO code) {
		ICodeMapper dao = sqlSession.getMapper(ICodeMapper.class);
		return dao.addCode(code);
	}

	@Transactional(transactionManager = "transactionManager")
	public int editCodeForTrialLicense(CodeVO code) {
		ICodeMapper dao = sqlSession.getMapper(ICodeMapper.class);
		return dao.editCodeForLicense(code);
	}
}