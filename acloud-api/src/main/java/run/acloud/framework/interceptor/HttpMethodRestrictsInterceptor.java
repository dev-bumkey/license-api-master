package run.acloud.framework.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
public class HttpMethodRestrictsInterceptor extends HandlerInterceptorAdapter{
	@Override
    public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3) throws Exception {
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,  Object arg2, ModelAndView arg3) throws Exception {
    	        
    }
    
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        String theMethod = request.getMethod();

        if ( HttpMethod.GET.matches(theMethod) || HttpMethod.POST.matches(theMethod) || HttpMethod.PUT.matches(theMethod) || HttpMethod.DELETE.matches(theMethod) ) {
            // GET, POST, PUT, DELETE methods are allowed
            return true;
        }
        else {
            // everything else is not allowed
            throw new HttpRequestMethodNotSupportedException(theMethod);
        }
	}
}
