/*
 * Harbor API
 * These APIs provide services for manipulating Harbor project.
 *
 * OpenAPI spec version: 1.10.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package run.acloud.commons.client.harbor.v1;

import java.util.List;
import java.util.Map;


public interface ApiCallback<T> {

    void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders);


    void onSuccess(T result, int statusCode, Map<String, List<String>> responseHeaders);


    void onUploadProgress(long bytesWritten, long contentLength, boolean done);


    void onDownloadProgress(long bytesRead, long contentLength, boolean done);
}
