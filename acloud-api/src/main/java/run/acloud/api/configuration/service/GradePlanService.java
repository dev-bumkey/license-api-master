package run.acloud.api.configuration.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.configuration.dao.IGradePlanChargeBaseMapper;
import run.acloud.api.configuration.dao.IGradePlanMapper;
import run.acloud.api.configuration.enums.AccountType;
import run.acloud.api.configuration.vo.AccountGradeVO;
import run.acloud.api.configuration.vo.GradePlanChargeBaseVO;
import run.acloud.api.configuration.vo.GradePlanVO;

import java.util.List;

@Slf4j
@Service
public class GradePlanService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;


    public List<GradePlanVO> getGradePlans(AccountType accountType) throws Exception{
        IGradePlanMapper gradePlanDao = sqlSession.getMapper(IGradePlanMapper.class);
        return gradePlanDao.getGradePlans(accountType);
    }

    public GradePlanVO getGradePlan(Integer gradeSeq) throws Exception{
        IGradePlanMapper gradePlanDao = sqlSession.getMapper(IGradePlanMapper.class);
        return gradePlanDao.getGradePlan(gradeSeq);
    }

    public int getUsedGradePlanCount(Integer gradeSeq) throws Exception{
        IGradePlanMapper gradePlanDao = sqlSession.getMapper(IGradePlanMapper.class);
        return gradePlanDao.getUsedGradePlanCount(gradeSeq);
    }

    @Transactional(transactionManager = "transactionManager")
    public int addGradePlan(GradePlanVO gradePlan) throws Exception{
        IGradePlanMapper gradePlanDao = sqlSession.getMapper(IGradePlanMapper.class);
        return gradePlanDao.addGradePlan(gradePlan);
    }

    @Transactional(transactionManager = "transactionManager")
    public int editGradePlan(GradePlanVO gradePlan) throws Exception{
        IGradePlanMapper gradePlanDao = sqlSession.getMapper(IGradePlanMapper.class);
        return gradePlanDao.editGradePlan(gradePlan);
    }

    @Transactional(transactionManager = "transactionManager")
    public int removeGradePlan(GradePlanVO gradePlan) throws Exception{
        IGradePlanMapper gradePlanDao = sqlSession.getMapper(IGradePlanMapper.class);
        return gradePlanDao.removeGradePlan(gradePlan);
    }

    public List<GradePlanChargeBaseVO> getGradePlanChargeBases(Integer gradeSeq) throws Exception{
        IGradePlanChargeBaseMapper gradePlanChargeBaseDao = sqlSession.getMapper(IGradePlanChargeBaseMapper.class);
        return gradePlanChargeBaseDao.getGradePlanChargeBases(gradeSeq);
    }

    public GradePlanChargeBaseVO getGradePlanChargeBase(Integer chargeBaseSeq) throws Exception{
        IGradePlanChargeBaseMapper gradePlanChargeBaseDao = sqlSession.getMapper(IGradePlanChargeBaseMapper.class);
        return gradePlanChargeBaseDao.getGradePlanChargeBase(chargeBaseSeq);
    }

    @Transactional(transactionManager = "transactionManager")
    public int addGradePlanChargeBases(GradePlanChargeBaseVO gradePlanChargeBase) throws Exception{
        IGradePlanChargeBaseMapper gradePlanChargeBaseDao = sqlSession.getMapper(IGradePlanChargeBaseMapper.class);
        return gradePlanChargeBaseDao.addGradePlanChargeBase(gradePlanChargeBase);
    }

    @Transactional(transactionManager = "transactionManager")
    public int editGradePlanChargeBases(GradePlanChargeBaseVO gradePlanChargeBase) throws Exception{
        IGradePlanChargeBaseMapper gradePlanChargeBaseDao = sqlSession.getMapper(IGradePlanChargeBaseMapper.class);
        return gradePlanChargeBaseDao.editGradePlanChargeBase(gradePlanChargeBase);
    }

    @Transactional(transactionManager = "transactionManager")
    public int removeGradePlanChargeBases(GradePlanChargeBaseVO gradePlanChargeBase) throws Exception{
        IGradePlanChargeBaseMapper gradePlanChargeBaseDao = sqlSession.getMapper(IGradePlanChargeBaseMapper.class);
        return gradePlanChargeBaseDao.removeGradePlanChargeBase(gradePlanChargeBase);
    }

    /**
     * is Single Tenancy Plan
     * - 싱글 테넌시 플랜은 grade_plan.grade_name 필드의 값이 "STP" 인 것으로 정의 (Hojae, coolingi, 2021.01.27)
     * @param gradeSeq
     * @return
     * @throws Exception
     */
    public boolean isSingleTenancy(Integer gradeSeq) throws Exception {
        GradePlanVO gradePlan = this.getGradePlan(gradeSeq);
        return this.isSingleTenancy(gradePlan);
    }

    /**
     * is Single Tenancy Plan
     * @param gradePlan
     * @return
     * @throws Exception
     */
    public boolean isSingleTenancy(GradePlanVO gradePlan) throws Exception {
        if(gradePlan != null) {
            return this.isSingleTenancy(gradePlan.getGradeName());
        }

        return false;
    }

    /**
     * is Single Tenancy Plan
     * @param gradeName
     * @return
     * @throws Exception
     */
    public boolean isSingleTenancy(String gradeName) throws Exception {
        if(StringUtils.isNotBlank(gradeName)) {
            if (gradeName.startsWith("STP")) {
                return true;
            }
        }

        return false;
    }

    /**
     * is Single Tenancy Plan
     * @param accountGrade
     * @return
     * @throws Exception
     */
    public boolean isSingleTenancy(AccountGradeVO accountGrade) throws Exception {
        if(accountGrade != null) {
            if(StringUtils.isNotBlank(accountGrade.getGradeName())) {
                return this.isSingleTenancy(accountGrade.getGradeName());
            }

            if(accountGrade.getGradeSeq() != null && accountGrade.getGradeSeq() > 0) {
                return this.isSingleTenancy(accountGrade.getGradeSeq());
            }
        }

        return false;
    }
}
