/*
 * Harbor API
 * These APIs provide services for manipulating Harbor project.
 *
 * OpenAPI spec version: 2.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package run.acloud.commons.client.harbor.v2.api;

import com.google.gson.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Response;
import run.acloud.commons.client.harbor.v2.*;
import run.acloud.commons.client.harbor.v2.model.OverallHealthStatus;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthApi {
    private HarborApiClientV2 apiClient;

    public HealthApi() {
        this(Configuration.getDefaultApiClient());
    }

    public HealthApi(HarborApiClientV2 apiClient) {
        this.apiClient = apiClient;
    }

    public HarborApiClientV2 getApiClient() {
        return apiClient;
    }

    public void setApiClient(HarborApiClientV2 apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for getHealth
     * @param xRequestId An unique ID for the request (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public Call getHealthCall(String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/health";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        if (xRequestId != null)
        localVarHeaderParams.put("X-Request-Id", apiClient.parameterToString(xRequestId));

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basic" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private Call getHealthValidateBeforeCall(String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        

        Call call = getHealthCall(xRequestId, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Check the status of Harbor components
     * Check the status of Harbor components
     * @param xRequestId An unique ID for the request (optional)
     * @return OverallHealthStatus
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public OverallHealthStatus getHealth(String xRequestId) throws ApiException {
        ApiResponse<OverallHealthStatus> resp = getHealthWithHttpInfo(xRequestId);
        return resp.getData();
    }

    /**
     * Check the status of Harbor components
     * Check the status of Harbor components
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse&lt;OverallHealthStatus&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<OverallHealthStatus> getHealthWithHttpInfo(String xRequestId) throws ApiException {
        Call call = getHealthValidateBeforeCall(xRequestId, null, null);
        Type localVarReturnType = new TypeToken<OverallHealthStatus>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Check the status of Harbor components (asynchronously)
     * Check the status of Harbor components
     * @param xRequestId An unique ID for the request (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public Call getHealthAsync(String xRequestId, final ApiCallback<OverallHealthStatus> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        Call call = getHealthValidateBeforeCall(xRequestId, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<OverallHealthStatus>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}