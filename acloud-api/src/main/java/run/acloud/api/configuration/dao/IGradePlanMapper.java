package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.configuration.enums.AccountType;
import run.acloud.api.configuration.vo.GradePlanVO;

import java.util.List;

public interface IGradePlanMapper {

    List<GradePlanVO> getGradePlans(@Param("accountType") AccountType accountType) throws Exception;

    GradePlanVO getGradePlan(@Param("gradeSeq") Integer gradeSeq) throws Exception;

    int addGradePlan(GradePlanVO gradePlan) throws Exception;

    int editGradePlan(GradePlanVO gradePlan) throws Exception;

    int removeGradePlan(GradePlanVO gradePlan) throws Exception;

    int getUsedGradePlanCount(@Param("gradeSeq") Integer gradeSeq) throws Exception;

}
