package run.acloud.api.event.service;

/**
 * @author dy79@acornsoft.io Created on 2017. 5. 1.
 */
public interface IClientAction<T> {
	
	String getDestination() throws Exception;
	
	String getType();
	
	T getResult();
	
}
