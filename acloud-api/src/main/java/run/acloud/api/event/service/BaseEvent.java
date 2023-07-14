package run.acloud.api.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 25.
 */
@Slf4j
@Component
public abstract class BaseEvent{

    public void exceptionHandle(String msg, Exception e, ExecutingContextVO context) {
        HttpServletRequest request = null;
        try {
            ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            request = sra.getRequest();
        } catch (IllegalStateException e1) {
            log.error("BaseEvent : IllegalStateException", e);
        }

        CocktailException ce = new CocktailException(msg, e, ExceptionType.InternalError);
        log.error(ExceptionMessageUtils.setCommonResult(request, context.getResult(), ce, false), e);
    }

}
