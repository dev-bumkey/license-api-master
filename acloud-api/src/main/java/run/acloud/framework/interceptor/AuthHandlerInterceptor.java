package run.acloud.framework.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

@Slf4j
@Component
public class AuthHandlerInterceptor extends HandlerInterceptorAdapter{
	@Override
    public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3) throws Exception {
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,  Object arg2, ModelAndView arg3) throws Exception {
    	        
    }
    
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        try {
            Integer userSeq = StringUtils.isNotBlank(request.getHeader("user-id")) ? Integer.parseInt(request.getHeader("user-id")) : null;
            Integer userServiceSeq = StringUtils.isNotBlank(request.getHeader("user-workspace")) ? Integer.parseInt(request.getHeader("user-workspace")) : null;
            ExecutingContextVO ctx = new ExecutingContextVO();
            ctx.setUserSeq(userSeq);
            ctx.setUserRole(request.getHeader("user-role"));
            ctx.setUserServiceSeq(userServiceSeq);
            ctx.setUserGrant(request.getHeader("user-grant"));

            request.setAttribute("ctx", ctx);

            AuthUtils.checkAuth(ctx);
            ContextHolder.exeContext(ctx);

            /**
             * Check Authorization
             */
            AuthUtils.isResourceAccessible(request);

        }
        catch (DataAccessException de) {
           throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            if (!this.isConnectionClose(request)) {
                throw ce;
            }
        }
        catch(Exception e) {
            if (!this.isConnectionClose(request)) {
               throw new CocktailException("Not authorized!!", e, ExceptionType.NotAuthorized);
            }
            else {
                log.debug("This request is close connection!!");
            }
        }

		return true;
	}

	private boolean isConnectionClose(HttpServletRequest request) {
	    boolean isConnectionClose = false;
	    if(request != null){
	        String connectionValue = StringUtils.defaultString(request.getHeader("Connection"));

	        if(StringUtils.equalsIgnoreCase(connectionValue, "close")){
	            isConnectionClose = true;
            }
        }

        return isConnectionClose;
    }
}
