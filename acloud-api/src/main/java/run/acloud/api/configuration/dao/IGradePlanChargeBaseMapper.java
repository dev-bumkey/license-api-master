package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.configuration.vo.GradePlanChargeBaseVO;

import java.util.List;

public interface IGradePlanChargeBaseMapper {

    List<GradePlanChargeBaseVO> getGradePlanChargeBases(@Param("gradeSeq") Integer gradeSeq) throws Exception;

    GradePlanChargeBaseVO getGradePlanChargeBase(@Param("chargeBaseSeq") Integer chargeBaseSeq) throws Exception;

    int addGradePlanChargeBase(GradePlanChargeBaseVO gradePlanChargeBase) throws Exception;

    int editGradePlanChargeBase(GradePlanChargeBaseVO gradePlanChargeBase) throws Exception;

    int removeGradePlanChargeBase(GradePlanChargeBaseVO gradePlanChargeBase) throws Exception;

}
