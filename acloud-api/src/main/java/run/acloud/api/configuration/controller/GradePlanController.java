package run.acloud.api.configuration.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.enums.AccountType;
import run.acloud.api.configuration.service.GradePlanService;
import run.acloud.api.configuration.vo.GradePlanVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

@Tag(name = "GradePlan", description = "GradePlan 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/grade")
@RestController
@Validated
public class GradePlanController {
    @Autowired
    private GradePlanService gradePlanService;


    @Operation(summary = "GradePlan 생성", description = "GradePlan를 생성한다.")
    @PostMapping(value = "")
    public void addGradePlan( @Parameter(description = "GradePlan 생성", required = true) @RequestBody GradePlanVO gradePlanVO ) throws Exception {
        gradePlanService.addGradePlan(gradePlanVO);
    }

    @Operation(summary = "GradePlan 수정", description = "GradePlan를 수정한다.")
    @PutMapping(value = "/{gradeSeq}")
    public void editGradePlan(
            @PathVariable Integer gradeSeq,
            @Parameter(description = "GradePlan 모델", required = true) @RequestBody GradePlanVO gradePlanVO
    ) throws Exception {
        if(gradeSeq != null){
            gradePlanVO.setGradeSeq(gradeSeq);
            gradePlanService.editGradePlan(gradePlanVO);
        }
    }

    @Operation(summary = "GradePlan 삭제", description = "GradePlan를 삭제한다.")
    @DeleteMapping(value = "/{gradeSeq}")
    public void removeGradePlan( @PathVariable Integer gradeSeq ) throws Exception {
        GradePlanVO gradePlanVO = gradePlanService.getGradePlan(gradeSeq);

        if(gradePlanVO != null){
            // 해당 grade plan을 사용하고 있는지 체크
            int usedCount = gradePlanService.getUsedGradePlanCount(gradeSeq);

            if(usedCount > 0) {
                throw new CocktailException("Can't delete the grade plan being used by account!", ExceptionType.CommonDeleteFail);
            }

            GradePlanVO gradePlan = new GradePlanVO();
            gradePlan.setGradeSeq(gradeSeq);
            gradePlanService.removeGradePlan(gradePlan);
        }
    }

    @Operation(summary = "GradePlan 전체 목록 조회", description = "GradePlan 전체 목록을 조회한다.")
    @GetMapping(value = "/")
    public List<GradePlanVO> getGradePlans() throws Exception {
        List<GradePlanVO> gradePlanList = gradePlanService.getGradePlans(null);
        return gradePlanList;
    }

    @Operation(summary = "GradePlan 상세 조회", description = "GradePlan 상세 정보 조회한다.")
    @GetMapping(value = "/{gradeSeq}")
    public GradePlanVO getGradePlan( @Parameter(description = "Grade Plan Sequence", required = true) @PathVariable Integer gradeSeq) throws Exception {
        GradePlanVO gradePlan = gradePlanService.getGradePlan(gradeSeq);
        return gradePlan;
    }

    @Operation(summary = "AccountType 에 해당하는 GradePlan 목록 조회", description = "AccountType 에 해당하는 GradePlan 목록을 조회한다.")
    @GetMapping(value = "/accountType/{accountType}")
    public List<GradePlanVO> getGradePlansByAccountType( @Parameter(description = "계정 타입", required = true) @PathVariable AccountType accountType) throws Exception {
        List<GradePlanVO> gradePlanList = gradePlanService.getGradePlans(accountType);
        return gradePlanList;
    }

}
