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

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Response;
import run.acloud.commons.client.harbor.v2.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OidcApi {
    private HarborApiClientV2 apiClient;

    public OidcApi() {
        this(Configuration.getDefaultApiClient());
    }

    public OidcApi(HarborApiClientV2 apiClient) {
        this.apiClient = apiClient;
    }

    public HarborApiClientV2 getApiClient() {
        return apiClient;
    }

    public void setApiClient(HarborApiClientV2 apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for pingOIDC
     * @param endpoint Request body for OIDC endpoint to be tested. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public Call pingOIDCCall(Object endpoint, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = endpoint;

        // create path and map variables
        String localVarPath = "/system/oidc/ping";

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
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private Call pingOIDCValidateBeforeCall(Object endpoint, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'endpoint' is set
        if (endpoint == null) {
            throw new ApiException("Missing the required parameter 'endpoint' when calling pingOIDC(Async)");
        }
        

        Call call = pingOIDCCall(endpoint, xRequestId, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Test the OIDC endpoint.
     * Test the OIDC endpoint, the setting of the endpoint is provided in the request.  This API can only be called by system admin. 
     * @param endpoint Request body for OIDC endpoint to be tested. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public void pingOIDC(Object endpoint, String xRequestId) throws ApiException {
        pingOIDCWithHttpInfo(endpoint, xRequestId);
    }

    /**
     * Test the OIDC endpoint.
     * Test the OIDC endpoint, the setting of the endpoint is provided in the request.  This API can only be called by system admin. 
     * @param endpoint Request body for OIDC endpoint to be tested. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<Void> pingOIDCWithHttpInfo(Object endpoint, String xRequestId) throws ApiException {
        Call call = pingOIDCValidateBeforeCall(endpoint, xRequestId, null, null);
        return apiClient.execute(call);
    }

    /**
     * Test the OIDC endpoint. (asynchronously)
     * Test the OIDC endpoint, the setting of the endpoint is provided in the request.  This API can only be called by system admin. 
     * @param endpoint Request body for OIDC endpoint to be tested. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public Call pingOIDCAsync(Object endpoint, String xRequestId, final ApiCallback<Void> callback) throws ApiException {

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

        Call call = pingOIDCValidateBeforeCall(endpoint, xRequestId, progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }
}