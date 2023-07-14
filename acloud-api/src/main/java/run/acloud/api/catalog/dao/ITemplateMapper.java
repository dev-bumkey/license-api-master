package run.acloud.api.catalog.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import run.acloud.api.catalog.vo.*;

import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 1. 10.
 */
@Repository
public interface ITemplateMapper {

	int checkDuplTemplateVersion(TemplateAddVO templateAdd);
	int checkDuplTemplateName(TemplateAddVO templateAdd);
	int addTemplate(TemplateAddVO templateAdd);
	int updateTemplateVersionForNonLatest(TemplateAddVO templateAdd);
	int addTemplateVersion(TemplateAddVO templateAdd);
	int addTemplateDeployment(TemplateDeploymentVO templateDeployment);
	List<TemplateListVO> getTemplateList(
			@Param("templateType") String templateType,
			@Param("templateShareType") String templateShareType,
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq
	);
	List<Integer> getTemplateVersionList(
		@Param("templateSeq") Integer templateSeq);
	TemplateDetailVO getTemplateDetail(
			@Param("templateSeq") Integer templateSeq,
			@Param("templateVersionSeq") Integer templateVersionSeq);
	TemplateVersionDelVO getTemplateVersionForDel(
			@Param("templateSeq") Integer templateSeq,
			@Param("templateVersionSeq") Integer templateVersionSeq);
	int deleteTemplateVersion(
			@Param("templateSeq") Integer templateSeq,
			@Param("templateVersionSeq") Integer templateVersionSeq,
			@Param("creator") Integer creator);
	int deleteTemplateVersionByService(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq,
			@Param("creator") Integer creator);
	int deleteTemplateByService(
			@Param("accountSeq") Integer accountSeq,
			@Param("serviceSeq") Integer serviceSeq,
			@Param("creator") Integer creator);
	int deleteTemplateByNoVersion(
			@Param("templateSeq") Integer templateSeq,
			@Param("creator") Integer creator);
	int updateTemplateVersionForLatest(
			@Param("templateSeq") Integer templateSeq,
			@Param("templateVersionSeq") Integer templateVersionSeq,
			@Param("creator") Integer creator);
	int updateTemplateVersion(TemplateEditVO templateEdit);
	int updateTemplateDeployment(TemplateDeploymentVO templateDeployment);
	int updateTemplateDeploymentAndType(TemplateDeploymentVO templateDeployment);
	List<TemplateDeploymentVO> getAllTemplateDeployments();

	TemplateVO getTemplate(
		@Param("templateSeq") Integer templateSeq,
		@Param("useYn") String useYn
	);

	int getSystemTemplatePermission(Map<String, Object> params);

	int getWorkspaceTemplatePermission(Map<String, Object> params);
}
