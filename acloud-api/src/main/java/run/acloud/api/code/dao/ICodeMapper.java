package run.acloud.api.code.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.code.vo.SubCodeVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
public interface ICodeMapper {
	List<CodeVO> getCodes(String groupId);
	
	List<SubCodeVO> getSubCodes(@Param("groupId") String groupId, @Param("subGroupId") String subGroupId);

	CodeVO getCode(@Param("groupId") String groupId, @Param("code") String code);

	int addCode(CodeVO code);
	int editCodeForLicense(CodeVO code);
}
