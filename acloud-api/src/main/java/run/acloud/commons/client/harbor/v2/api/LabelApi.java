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
import run.acloud.commons.client.harbor.v2.model.Label;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabelApi {
    private HarborApiClientV2 apiClient;

    public LabelApi() {
        this(Configuration.getDefaultApiClient());
    }

    public LabelApi(HarborApiClientV2 apiClient) {
        this.apiClient = apiClient;
    }

    public HarborApiClientV2 getApiClient() {
        return apiClient;
    }

    public void setApiClient(HarborApiClientV2 apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for createLabel
     * @param label The json object of label. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public Call createLabelCall(Label label, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = label;

        // create path and map variables
        String localVarPath = "/labels";

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
    private Call createLabelValidateBeforeCall(Label label, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'label' is set
        if (label == null) {
            throw new ApiException("Missing the required parameter 'label' when calling createLabel(Async)");
        }
        

        Call call = createLabelCall(label, xRequestId, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Post creates a label
     * This endpoint let user creates a label. 
     * @param label The json object of label. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public void createLabel(Label label, String xRequestId) throws ApiException {
        createLabelWithHttpInfo(label, xRequestId);
    }

    /**
     * Post creates a label
     * This endpoint let user creates a label. 
     * @param label The json object of label. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<Void> createLabelWithHttpInfo(Label label, String xRequestId) throws ApiException {
        Call call = createLabelValidateBeforeCall(label, xRequestId, null, null);
        return apiClient.execute(call);
    }

    /**
     * Post creates a label (asynchronously)
     * This endpoint let user creates a label. 
     * @param label The json object of label. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public Call createLabelAsync(Label label, String xRequestId, final ApiCallback<Void> callback) throws ApiException {

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

        Call call = createLabelValidateBeforeCall(label, xRequestId, progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }
    /**
     * Build call for deleteLabel
     * @param labelId Label ID (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public Call deleteLabelCall(Long labelId, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/labels/{label_id}"
            .replaceAll("\\{" + "label_id" + "\\}", apiClient.escapeString(labelId.toString()));

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
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private Call deleteLabelValidateBeforeCall(Long labelId, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'labelId' is set
        if (labelId == null) {
            throw new ApiException("Missing the required parameter 'labelId' when calling deleteLabel(Async)");
        }
        

        Call call = deleteLabelCall(labelId, xRequestId, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete the label specified by ID.
     * Delete the label specified by ID. 
     * @param labelId Label ID (required)
     * @param xRequestId An unique ID for the request (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public void deleteLabel(Long labelId, String xRequestId) throws ApiException {
        deleteLabelWithHttpInfo(labelId, xRequestId);
    }

    /**
     * Delete the label specified by ID.
     * Delete the label specified by ID. 
     * @param labelId Label ID (required)
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<Void> deleteLabelWithHttpInfo(Long labelId, String xRequestId) throws ApiException {
        Call call = deleteLabelValidateBeforeCall(labelId, xRequestId, null, null);
        return apiClient.execute(call);
    }

    /**
     * Delete the label specified by ID. (asynchronously)
     * Delete the label specified by ID. 
     * @param labelId Label ID (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public Call deleteLabelAsync(Long labelId, String xRequestId, final ApiCallback<Void> callback) throws ApiException {

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

        Call call = deleteLabelValidateBeforeCall(labelId, xRequestId, progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }
    /**
     * Build call for getLabelByID
     * @param labelId Label ID (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public Call getLabelByIDCall(Long labelId, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/labels/{label_id}"
            .replaceAll("\\{" + "label_id" + "\\}", apiClient.escapeString(labelId.toString()));

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
    private Call getLabelByIDValidateBeforeCall(Long labelId, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'labelId' is set
        if (labelId == null) {
            throw new ApiException("Missing the required parameter 'labelId' when calling getLabelByID(Async)");
        }
        

        Call call = getLabelByIDCall(labelId, xRequestId, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get the label specified by ID.
     * This endpoint let user get the label by specific ID. 
     * @param labelId Label ID (required)
     * @param xRequestId An unique ID for the request (optional)
     * @return Label
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public Label getLabelByID(Long labelId, String xRequestId) throws ApiException {
        ApiResponse<Label> resp = getLabelByIDWithHttpInfo(labelId, xRequestId);
        return resp.getData();
    }

    /**
     * Get the label specified by ID.
     * This endpoint let user get the label by specific ID. 
     * @param labelId Label ID (required)
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse&lt;Label&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<Label> getLabelByIDWithHttpInfo(Long labelId, String xRequestId) throws ApiException {
        Call call = getLabelByIDValidateBeforeCall(labelId, xRequestId, null, null);
        Type localVarReturnType = new TypeToken<Label>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get the label specified by ID. (asynchronously)
     * This endpoint let user get the label by specific ID. 
     * @param labelId Label ID (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public Call getLabelByIDAsync(Long labelId, String xRequestId, final ApiCallback<Label> callback) throws ApiException {

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

        Call call = getLabelByIDValidateBeforeCall(labelId, xRequestId, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<Label>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for listLabels
     * @param xRequestId An unique ID for the request (optional)
     * @param q Query string to query resources. Supported query patterns are \&quot;exact match(k&#x3D;v)\&quot;, \&quot;fuzzy match(k&#x3D;~v)\&quot;, \&quot;range(k&#x3D;[min~max])\&quot;, \&quot;list with union releationship(k&#x3D;{v1 v2 v3})\&quot; and \&quot;list with intersetion relationship(k&#x3D;(v1 v2 v3))\&quot;. The value of range and list can be string(enclosed by \&quot; or &#39;), integer or time(in format \&quot;2020-04-09 02:36:00\&quot;). All of these query patterns should be put in the query string \&quot;q&#x3D;xxx\&quot; and splitted by \&quot;,\&quot;. e.g. q&#x3D;k1&#x3D;v1,k2&#x3D;~v2,k3&#x3D;[min~max] (optional)
     * @param sort Sort the resource list in ascending or descending order. e.g. sort by field1 in ascending orderr and field2 in descending order with \&quot;sort&#x3D;field1,-field2\&quot; (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param name The label name. (optional)
     * @param scope The label scope. Valid values are g and p. g for global labels and p for project labels. (optional)
     * @param projectId Relevant project ID, required when scope is p. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public Call listLabelsCall(String xRequestId, String q, String sort, Long page, Long pageSize, String name, String scope, Long projectId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/labels";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (q != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("q", q));
        if (sort != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("sort", sort));
        if (page != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("page", page));
        if (pageSize != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("page_size", pageSize));
        if (name != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("name", name));
        if (scope != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("scope", scope));
        if (projectId != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("project_id", projectId));

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
    private Call listLabelsValidateBeforeCall(String xRequestId, String q, String sort, Long page, Long pageSize, String name, String scope, Long projectId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        

        Call call = listLabelsCall(xRequestId, q, sort, page, pageSize, name, scope, projectId, progressListener, progressRequestListener);
        return call;

    }

    /**
     * List labels according to the query strings.
     * This endpoint let user list labels by name, scope and project_id 
     * @param xRequestId An unique ID for the request (optional)
     * @param q Query string to query resources. Supported query patterns are \&quot;exact match(k&#x3D;v)\&quot;, \&quot;fuzzy match(k&#x3D;~v)\&quot;, \&quot;range(k&#x3D;[min~max])\&quot;, \&quot;list with union releationship(k&#x3D;{v1 v2 v3})\&quot; and \&quot;list with intersetion relationship(k&#x3D;(v1 v2 v3))\&quot;. The value of range and list can be string(enclosed by \&quot; or &#39;), integer or time(in format \&quot;2020-04-09 02:36:00\&quot;). All of these query patterns should be put in the query string \&quot;q&#x3D;xxx\&quot; and splitted by \&quot;,\&quot;. e.g. q&#x3D;k1&#x3D;v1,k2&#x3D;~v2,k3&#x3D;[min~max] (optional)
     * @param sort Sort the resource list in ascending or descending order. e.g. sort by field1 in ascending orderr and field2 in descending order with \&quot;sort&#x3D;field1,-field2\&quot; (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param name The label name. (optional)
     * @param scope The label scope. Valid values are g and p. g for global labels and p for project labels. (optional)
     * @param projectId Relevant project ID, required when scope is p. (optional)
     * @return List&lt;Label&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public List<Label> listLabels(String xRequestId, String q, String sort, Long page, Long pageSize, String name, String scope, Long projectId) throws ApiException {
        ApiResponse<List<Label>> resp = listLabelsWithHttpInfo(xRequestId, q, sort, page, pageSize, name, scope, projectId);
        return resp.getData();
    }

    /**
     * List labels according to the query strings.
     * This endpoint let user list labels by name, scope and project_id 
     * @param xRequestId An unique ID for the request (optional)
     * @param q Query string to query resources. Supported query patterns are \&quot;exact match(k&#x3D;v)\&quot;, \&quot;fuzzy match(k&#x3D;~v)\&quot;, \&quot;range(k&#x3D;[min~max])\&quot;, \&quot;list with union releationship(k&#x3D;{v1 v2 v3})\&quot; and \&quot;list with intersetion relationship(k&#x3D;(v1 v2 v3))\&quot;. The value of range and list can be string(enclosed by \&quot; or &#39;), integer or time(in format \&quot;2020-04-09 02:36:00\&quot;). All of these query patterns should be put in the query string \&quot;q&#x3D;xxx\&quot; and splitted by \&quot;,\&quot;. e.g. q&#x3D;k1&#x3D;v1,k2&#x3D;~v2,k3&#x3D;[min~max] (optional)
     * @param sort Sort the resource list in ascending or descending order. e.g. sort by field1 in ascending orderr and field2 in descending order with \&quot;sort&#x3D;field1,-field2\&quot; (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param name The label name. (optional)
     * @param scope The label scope. Valid values are g and p. g for global labels and p for project labels. (optional)
     * @param projectId Relevant project ID, required when scope is p. (optional)
     * @return ApiResponse&lt;List&lt;Label&gt;&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<List<Label>> listLabelsWithHttpInfo(String xRequestId, String q, String sort, Long page, Long pageSize, String name, String scope, Long projectId) throws ApiException {
        Call call = listLabelsValidateBeforeCall(xRequestId, q, sort, page, pageSize, name, scope, projectId, null, null);
        Type localVarReturnType = new TypeToken<List<Label>>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * List labels according to the query strings. (asynchronously)
     * This endpoint let user list labels by name, scope and project_id 
     * @param xRequestId An unique ID for the request (optional)
     * @param q Query string to query resources. Supported query patterns are \&quot;exact match(k&#x3D;v)\&quot;, \&quot;fuzzy match(k&#x3D;~v)\&quot;, \&quot;range(k&#x3D;[min~max])\&quot;, \&quot;list with union releationship(k&#x3D;{v1 v2 v3})\&quot; and \&quot;list with intersetion relationship(k&#x3D;(v1 v2 v3))\&quot;. The value of range and list can be string(enclosed by \&quot; or &#39;), integer or time(in format \&quot;2020-04-09 02:36:00\&quot;). All of these query patterns should be put in the query string \&quot;q&#x3D;xxx\&quot; and splitted by \&quot;,\&quot;. e.g. q&#x3D;k1&#x3D;v1,k2&#x3D;~v2,k3&#x3D;[min~max] (optional)
     * @param sort Sort the resource list in ascending or descending order. e.g. sort by field1 in ascending orderr and field2 in descending order with \&quot;sort&#x3D;field1,-field2\&quot; (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param name The label name. (optional)
     * @param scope The label scope. Valid values are g and p. g for global labels and p for project labels. (optional)
     * @param projectId Relevant project ID, required when scope is p. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public Call listLabelsAsync(String xRequestId, String q, String sort, Long page, Long pageSize, String name, String scope, Long projectId, final ApiCallback<List<Label>> callback) throws ApiException {

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

        Call call = listLabelsValidateBeforeCall(xRequestId, q, sort, page, pageSize, name, scope, projectId, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<List<Label>>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateLabel
     * @param labelId Label ID (required)
     * @param label The updated label json object. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public Call updateLabelCall(Long labelId, Label label, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = label;

        // create path and map variables
        String localVarPath = "/labels/{label_id}"
            .replaceAll("\\{" + "label_id" + "\\}", apiClient.escapeString(labelId.toString()));

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
        return apiClient.buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private Call updateLabelValidateBeforeCall(Long labelId, Label label, String xRequestId, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'labelId' is set
        if (labelId == null) {
            throw new ApiException("Missing the required parameter 'labelId' when calling updateLabel(Async)");
        }
        
        // verify the required parameter 'label' is set
        if (label == null) {
            throw new ApiException("Missing the required parameter 'label' when calling updateLabel(Async)");
        }
        

        Call call = updateLabelCall(labelId, label, xRequestId, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update the label properties.
     * This endpoint let user update label properties. 
     * @param labelId Label ID (required)
     * @param label The updated label json object. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public void updateLabel(Long labelId, Label label, String xRequestId) throws ApiException {
        updateLabelWithHttpInfo(labelId, label, xRequestId);
    }

    /**
     * Update the label properties.
     * This endpoint let user update label properties. 
     * @param labelId Label ID (required)
     * @param label The updated label json object. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<Void> updateLabelWithHttpInfo(Long labelId, Label label, String xRequestId) throws ApiException {
        Call call = updateLabelValidateBeforeCall(labelId, label, xRequestId, null, null);
        return apiClient.execute(call);
    }

    /**
     * Update the label properties. (asynchronously)
     * This endpoint let user update label properties. 
     * @param labelId Label ID (required)
     * @param label The updated label json object. (required)
     * @param xRequestId An unique ID for the request (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public Call updateLabelAsync(Long labelId, Label label, String xRequestId, final ApiCallback<Void> callback) throws ApiException {

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

        Call call = updateLabelValidateBeforeCall(labelId, label, xRequestId, progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }
}
