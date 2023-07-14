package run.acloud.protobuf.chart;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@jakarta.annotation.Generated(
    value = "by gRPC proto compiler (version 1.16.1)",
    comments = "Source: v1/chart/chart.proto")
public final class ChartAPIGrpc {

  private ChartAPIGrpc() {}

  public static final String SERVICE_NAME = "chart.ChartAPI";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.RepoChartsRequest,
      run.acloud.protobuf.chart.Package.RepoChartsResponse> getSearchRepoChartsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchRepoCharts",
      requestType = run.acloud.protobuf.chart.Package.RepoChartsRequest.class,
      responseType = run.acloud.protobuf.chart.Package.RepoChartsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.RepoChartsRequest,
      run.acloud.protobuf.chart.Package.RepoChartsResponse> getSearchRepoChartsMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.RepoChartsRequest, run.acloud.protobuf.chart.Package.RepoChartsResponse> getSearchRepoChartsMethod;
    if ((getSearchRepoChartsMethod = ChartAPIGrpc.getSearchRepoChartsMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getSearchRepoChartsMethod = ChartAPIGrpc.getSearchRepoChartsMethod) == null) {
          ChartAPIGrpc.getSearchRepoChartsMethod = getSearchRepoChartsMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.RepoChartsRequest, run.acloud.protobuf.chart.Package.RepoChartsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "SearchRepoCharts"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.RepoChartsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.RepoChartsResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("SearchRepoCharts"))
                  .build();
          }
        }
     }
     return getSearchRepoChartsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.ChartVersionsRequest,
      run.acloud.protobuf.chart.Package.ChartVersionsResponse> getChartVersionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ChartVersions",
      requestType = run.acloud.protobuf.chart.Package.ChartVersionsRequest.class,
      responseType = run.acloud.protobuf.chart.Package.ChartVersionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.ChartVersionsRequest,
      run.acloud.protobuf.chart.Package.ChartVersionsResponse> getChartVersionsMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.ChartVersionsRequest, run.acloud.protobuf.chart.Package.ChartVersionsResponse> getChartVersionsMethod;
    if ((getChartVersionsMethod = ChartAPIGrpc.getChartVersionsMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getChartVersionsMethod = ChartAPIGrpc.getChartVersionsMethod) == null) {
          ChartAPIGrpc.getChartVersionsMethod = getChartVersionsMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.ChartVersionsRequest, run.acloud.protobuf.chart.Package.ChartVersionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "ChartVersions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.ChartVersionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.ChartVersionsResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("ChartVersions"))
                  .build();
          }
        }
     }
     return getChartVersionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.GetChartVersionRequest,
      run.acloud.protobuf.chart.Package.ChartVersionDetailResponse> getChartVersionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ChartVersion",
      requestType = run.acloud.protobuf.chart.Package.GetChartVersionRequest.class,
      responseType = run.acloud.protobuf.chart.Package.ChartVersionDetailResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.GetChartVersionRequest,
      run.acloud.protobuf.chart.Package.ChartVersionDetailResponse> getChartVersionMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.GetChartVersionRequest, run.acloud.protobuf.chart.Package.ChartVersionDetailResponse> getChartVersionMethod;
    if ((getChartVersionMethod = ChartAPIGrpc.getChartVersionMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getChartVersionMethod = ChartAPIGrpc.getChartVersionMethod) == null) {
          ChartAPIGrpc.getChartVersionMethod = getChartVersionMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.GetChartVersionRequest, run.acloud.protobuf.chart.Package.ChartVersionDetailResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "ChartVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.GetChartVersionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.ChartVersionDetailResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("ChartVersion"))
                  .build();
          }
        }
     }
     return getChartVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      run.acloud.protobuf.chart.Package.ReposResponse> getReposMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Repos",
      requestType = com.google.protobuf.Empty.class,
      responseType = run.acloud.protobuf.chart.Package.ReposResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      run.acloud.protobuf.chart.Package.ReposResponse> getReposMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, run.acloud.protobuf.chart.Package.ReposResponse> getReposMethod;
    if ((getReposMethod = ChartAPIGrpc.getReposMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getReposMethod = ChartAPIGrpc.getReposMethod) == null) {
          ChartAPIGrpc.getReposMethod = getReposMethod = 
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, run.acloud.protobuf.chart.Package.ReposResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "Repos"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.ReposResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("Repos"))
                  .build();
          }
        }
     }
     return getReposMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmListRequest,
      run.acloud.protobuf.chart.Package.HelmListResponse> getHelmListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelmList",
      requestType = run.acloud.protobuf.chart.Package.HelmListRequest.class,
      responseType = run.acloud.protobuf.chart.Package.HelmListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmListRequest,
      run.acloud.protobuf.chart.Package.HelmListResponse> getHelmListMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmListRequest, run.acloud.protobuf.chart.Package.HelmListResponse> getHelmListMethod;
    if ((getHelmListMethod = ChartAPIGrpc.getHelmListMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getHelmListMethod = ChartAPIGrpc.getHelmListMethod) == null) {
          ChartAPIGrpc.getHelmListMethod = getHelmListMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.HelmListRequest, run.acloud.protobuf.chart.Package.HelmListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "HelmList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmListResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("HelmList"))
                  .build();
          }
        }
     }
     return getHelmListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmStatusRequest,
      run.acloud.protobuf.chart.Package.HelmStatusResponse> getHelmStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelmStatus",
      requestType = run.acloud.protobuf.chart.Package.HelmStatusRequest.class,
      responseType = run.acloud.protobuf.chart.Package.HelmStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmStatusRequest,
      run.acloud.protobuf.chart.Package.HelmStatusResponse> getHelmStatusMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmStatusRequest, run.acloud.protobuf.chart.Package.HelmStatusResponse> getHelmStatusMethod;
    if ((getHelmStatusMethod = ChartAPIGrpc.getHelmStatusMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getHelmStatusMethod = ChartAPIGrpc.getHelmStatusMethod) == null) {
          ChartAPIGrpc.getHelmStatusMethod = getHelmStatusMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.HelmStatusRequest, run.acloud.protobuf.chart.Package.HelmStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "HelmStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmStatusResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("HelmStatus"))
                  .build();
          }
        }
     }
     return getHelmStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmInstallRequest,
      run.acloud.protobuf.chart.Package.HelmInstallResponse> getHelmInstallMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelmInstall",
      requestType = run.acloud.protobuf.chart.Package.HelmInstallRequest.class,
      responseType = run.acloud.protobuf.chart.Package.HelmInstallResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmInstallRequest,
      run.acloud.protobuf.chart.Package.HelmInstallResponse> getHelmInstallMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmInstallRequest, run.acloud.protobuf.chart.Package.HelmInstallResponse> getHelmInstallMethod;
    if ((getHelmInstallMethod = ChartAPIGrpc.getHelmInstallMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getHelmInstallMethod = ChartAPIGrpc.getHelmInstallMethod) == null) {
          ChartAPIGrpc.getHelmInstallMethod = getHelmInstallMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.HelmInstallRequest, run.acloud.protobuf.chart.Package.HelmInstallResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "HelmInstall"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmInstallRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmInstallResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("HelmInstall"))
                  .build();
          }
        }
     }
     return getHelmInstallMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmUpgradeRequest,
      run.acloud.protobuf.chart.Package.HelmUpgradeResponse> getHelmUpgradeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelmUpgrade",
      requestType = run.acloud.protobuf.chart.Package.HelmUpgradeRequest.class,
      responseType = run.acloud.protobuf.chart.Package.HelmUpgradeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmUpgradeRequest,
      run.acloud.protobuf.chart.Package.HelmUpgradeResponse> getHelmUpgradeMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmUpgradeRequest, run.acloud.protobuf.chart.Package.HelmUpgradeResponse> getHelmUpgradeMethod;
    if ((getHelmUpgradeMethod = ChartAPIGrpc.getHelmUpgradeMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getHelmUpgradeMethod = ChartAPIGrpc.getHelmUpgradeMethod) == null) {
          ChartAPIGrpc.getHelmUpgradeMethod = getHelmUpgradeMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.HelmUpgradeRequest, run.acloud.protobuf.chart.Package.HelmUpgradeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "HelmUpgrade"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmUpgradeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmUpgradeResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("HelmUpgrade"))
                  .build();
          }
        }
     }
     return getHelmUpgradeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmRollbackRequest,
      run.acloud.protobuf.chart.Package.HelmRollbackResponse> getHelmRollbackMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelmRollback",
      requestType = run.acloud.protobuf.chart.Package.HelmRollbackRequest.class,
      responseType = run.acloud.protobuf.chart.Package.HelmRollbackResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmRollbackRequest,
      run.acloud.protobuf.chart.Package.HelmRollbackResponse> getHelmRollbackMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmRollbackRequest, run.acloud.protobuf.chart.Package.HelmRollbackResponse> getHelmRollbackMethod;
    if ((getHelmRollbackMethod = ChartAPIGrpc.getHelmRollbackMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getHelmRollbackMethod = ChartAPIGrpc.getHelmRollbackMethod) == null) {
          ChartAPIGrpc.getHelmRollbackMethod = getHelmRollbackMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.HelmRollbackRequest, run.acloud.protobuf.chart.Package.HelmRollbackResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "HelmRollback"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmRollbackRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmRollbackResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("HelmRollback"))
                  .build();
          }
        }
     }
     return getHelmRollbackMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmHistoryRequest,
      run.acloud.protobuf.chart.Package.HelmHistoryResponse> getHelmHistoryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelmHistory",
      requestType = run.acloud.protobuf.chart.Package.HelmHistoryRequest.class,
      responseType = run.acloud.protobuf.chart.Package.HelmHistoryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmHistoryRequest,
      run.acloud.protobuf.chart.Package.HelmHistoryResponse> getHelmHistoryMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmHistoryRequest, run.acloud.protobuf.chart.Package.HelmHistoryResponse> getHelmHistoryMethod;
    if ((getHelmHistoryMethod = ChartAPIGrpc.getHelmHistoryMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getHelmHistoryMethod = ChartAPIGrpc.getHelmHistoryMethod) == null) {
          ChartAPIGrpc.getHelmHistoryMethod = getHelmHistoryMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.HelmHistoryRequest, run.acloud.protobuf.chart.Package.HelmHistoryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "HelmHistory"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmHistoryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmHistoryResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("HelmHistory"))
                  .build();
          }
        }
     }
     return getHelmHistoryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmUninstallRequest,
      run.acloud.protobuf.chart.Package.HelmUninstallResponse> getHelmUninstallMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HelmUninstall",
      requestType = run.acloud.protobuf.chart.Package.HelmUninstallRequest.class,
      responseType = run.acloud.protobuf.chart.Package.HelmUninstallResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmUninstallRequest,
      run.acloud.protobuf.chart.Package.HelmUninstallResponse> getHelmUninstallMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.chart.Package.HelmUninstallRequest, run.acloud.protobuf.chart.Package.HelmUninstallResponse> getHelmUninstallMethod;
    if ((getHelmUninstallMethod = ChartAPIGrpc.getHelmUninstallMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getHelmUninstallMethod = ChartAPIGrpc.getHelmUninstallMethod) == null) {
          ChartAPIGrpc.getHelmUninstallMethod = getHelmUninstallMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.chart.Package.HelmUninstallRequest, run.acloud.protobuf.chart.Package.HelmUninstallResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "HelmUninstall"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmUninstallRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.chart.Package.HelmUninstallResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("HelmUninstall"))
                  .build();
          }
        }
     }
     return getHelmUninstallMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getHealthyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Healthy",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getHealthyMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getHealthyMethod;
    if ((getHealthyMethod = ChartAPIGrpc.getHealthyMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getHealthyMethod = ChartAPIGrpc.getHealthyMethod) == null) {
          ChartAPIGrpc.getHealthyMethod = getHealthyMethod = 
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "Healthy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("Healthy"))
                  .build();
          }
        }
     }
     return getHealthyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getReadinessMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Readiness",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getReadinessMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getReadinessMethod;
    if ((getReadinessMethod = ChartAPIGrpc.getReadinessMethod) == null) {
      synchronized (ChartAPIGrpc.class) {
        if ((getReadinessMethod = ChartAPIGrpc.getReadinessMethod) == null) {
          ChartAPIGrpc.getReadinessMethod = getReadinessMethod = 
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "chart.ChartAPI", "Readiness"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new ChartAPIMethodDescriptorSupplier("Readiness"))
                  .build();
          }
        }
     }
     return getReadinessMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ChartAPIStub newStub(io.grpc.Channel channel) {
    return new ChartAPIStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ChartAPIBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ChartAPIBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ChartAPIFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ChartAPIFutureStub(channel);
  }

  /**
   */
  public static abstract class ChartAPIImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * chart 목록 조회
     * </pre>
     */
    public void searchRepoCharts(run.acloud.protobuf.chart.Package.RepoChartsRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.RepoChartsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSearchRepoChartsMethod(), responseObserver);
    }

    /**
     * <pre>
     * chart 버젼 목록 조회
     * </pre>
     */
    public void chartVersions(run.acloud.protobuf.chart.Package.ChartVersionsRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ChartVersionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getChartVersionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * chart 상세 내용 조회
     * </pre>
     */
    public void chartVersion(run.acloud.protobuf.chart.Package.GetChartVersionRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ChartVersionDetailResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getChartVersionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Helm Repository List
     * </pre>
     */
    public void repos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ReposResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getReposMethod(), responseObserver);
    }

    /**
     * <pre>
     * example : helm list -n cocktail-addon
     * </pre>
     */
    public void helmList(run.acloud.protobuf.chart.Package.HelmListRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmListResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHelmListMethod(), responseObserver);
    }

    /**
     * <pre>
     * example : helm status RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public void helmStatus(run.acloud.protobuf.chart.Package.HelmStatusRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmStatusResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHelmStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * example : helm install [NAME] [CHART]
     * </pre>
     */
    public void helmInstall(run.acloud.protobuf.chart.Package.HelmInstallRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmInstallResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHelmInstallMethod(), responseObserver);
    }

    /**
     * <pre>
     * example : helm upgrade [NAME] [CHART]
     * </pre>
     */
    public void helmUpgrade(run.acloud.protobuf.chart.Package.HelmUpgradeRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmUpgradeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHelmUpgradeMethod(), responseObserver);
    }

    /**
     * <pre>
     * example : helm rollback &lt;RELEASE&gt; [REVISION]
     * </pre>
     */
    public void helmRollback(run.acloud.protobuf.chart.Package.HelmRollbackRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmRollbackResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHelmRollbackMethod(), responseObserver);
    }

    /**
     * <pre>
     * example : helm uninstall RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public void helmHistory(run.acloud.protobuf.chart.Package.HelmHistoryRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmHistoryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHelmHistoryMethod(), responseObserver);
    }

    /**
     * <pre>
     * example : helm uninstall RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public void helmUninstall(run.acloud.protobuf.chart.Package.HelmUninstallRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmUninstallResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHelmUninstallMethod(), responseObserver);
    }

    /**
     */
    public void healthy(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getHealthyMethod(), responseObserver);
    }

    /**
     */
    public void readiness(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getReadinessMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSearchRepoChartsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.RepoChartsRequest,
                run.acloud.protobuf.chart.Package.RepoChartsResponse>(
                  this, METHODID_SEARCH_REPO_CHARTS)))
          .addMethod(
            getChartVersionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.ChartVersionsRequest,
                run.acloud.protobuf.chart.Package.ChartVersionsResponse>(
                  this, METHODID_CHART_VERSIONS)))
          .addMethod(
            getChartVersionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.GetChartVersionRequest,
                run.acloud.protobuf.chart.Package.ChartVersionDetailResponse>(
                  this, METHODID_CHART_VERSION)))
          .addMethod(
            getReposMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                run.acloud.protobuf.chart.Package.ReposResponse>(
                  this, METHODID_REPOS)))
          .addMethod(
            getHelmListMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.HelmListRequest,
                run.acloud.protobuf.chart.Package.HelmListResponse>(
                  this, METHODID_HELM_LIST)))
          .addMethod(
            getHelmStatusMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.HelmStatusRequest,
                run.acloud.protobuf.chart.Package.HelmStatusResponse>(
                  this, METHODID_HELM_STATUS)))
          .addMethod(
            getHelmInstallMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.HelmInstallRequest,
                run.acloud.protobuf.chart.Package.HelmInstallResponse>(
                  this, METHODID_HELM_INSTALL)))
          .addMethod(
            getHelmUpgradeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.HelmUpgradeRequest,
                run.acloud.protobuf.chart.Package.HelmUpgradeResponse>(
                  this, METHODID_HELM_UPGRADE)))
          .addMethod(
            getHelmRollbackMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.HelmRollbackRequest,
                run.acloud.protobuf.chart.Package.HelmRollbackResponse>(
                  this, METHODID_HELM_ROLLBACK)))
          .addMethod(
            getHelmHistoryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.HelmHistoryRequest,
                run.acloud.protobuf.chart.Package.HelmHistoryResponse>(
                  this, METHODID_HELM_HISTORY)))
          .addMethod(
            getHelmUninstallMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.chart.Package.HelmUninstallRequest,
                run.acloud.protobuf.chart.Package.HelmUninstallResponse>(
                  this, METHODID_HELM_UNINSTALL)))
          .addMethod(
            getHealthyMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_HEALTHY)))
          .addMethod(
            getReadinessMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_READINESS)))
          .build();
    }
  }

  /**
   */
  public static final class ChartAPIStub extends io.grpc.stub.AbstractStub<ChartAPIStub> {
    private ChartAPIStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ChartAPIStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChartAPIStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ChartAPIStub(channel, callOptions);
    }

    /**
     * <pre>
     * chart 목록 조회
     * </pre>
     */
    public void searchRepoCharts(run.acloud.protobuf.chart.Package.RepoChartsRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.RepoChartsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSearchRepoChartsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * chart 버젼 목록 조회
     * </pre>
     */
    public void chartVersions(run.acloud.protobuf.chart.Package.ChartVersionsRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ChartVersionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getChartVersionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * chart 상세 내용 조회
     * </pre>
     */
    public void chartVersion(run.acloud.protobuf.chart.Package.GetChartVersionRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ChartVersionDetailResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getChartVersionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Helm Repository List
     * </pre>
     */
    public void repos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ReposResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReposMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * example : helm list -n cocktail-addon
     * </pre>
     */
    public void helmList(run.acloud.protobuf.chart.Package.HelmListRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmListResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHelmListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * example : helm status RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public void helmStatus(run.acloud.protobuf.chart.Package.HelmStatusRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmStatusResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHelmStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * example : helm install [NAME] [CHART]
     * </pre>
     */
    public void helmInstall(run.acloud.protobuf.chart.Package.HelmInstallRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmInstallResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHelmInstallMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * example : helm upgrade [NAME] [CHART]
     * </pre>
     */
    public void helmUpgrade(run.acloud.protobuf.chart.Package.HelmUpgradeRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmUpgradeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHelmUpgradeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * example : helm rollback &lt;RELEASE&gt; [REVISION]
     * </pre>
     */
    public void helmRollback(run.acloud.protobuf.chart.Package.HelmRollbackRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmRollbackResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHelmRollbackMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * example : helm uninstall RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public void helmHistory(run.acloud.protobuf.chart.Package.HelmHistoryRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmHistoryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHelmHistoryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * example : helm uninstall RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public void helmUninstall(run.acloud.protobuf.chart.Package.HelmUninstallRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmUninstallResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHelmUninstallMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void healthy(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHealthyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void readiness(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadinessMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ChartAPIBlockingStub extends io.grpc.stub.AbstractStub<ChartAPIBlockingStub> {
    private ChartAPIBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ChartAPIBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChartAPIBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ChartAPIBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * chart 목록 조회
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.RepoChartsResponse searchRepoCharts(run.acloud.protobuf.chart.Package.RepoChartsRequest request) {
      return blockingUnaryCall(
          getChannel(), getSearchRepoChartsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * chart 버젼 목록 조회
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.ChartVersionsResponse chartVersions(run.acloud.protobuf.chart.Package.ChartVersionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getChartVersionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * chart 상세 내용 조회
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.ChartVersionDetailResponse chartVersion(run.acloud.protobuf.chart.Package.GetChartVersionRequest request) {
      return blockingUnaryCall(
          getChannel(), getChartVersionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Helm Repository List
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.ReposResponse repos(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getReposMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * example : helm list -n cocktail-addon
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.HelmListResponse helmList(run.acloud.protobuf.chart.Package.HelmListRequest request) {
      return blockingUnaryCall(
          getChannel(), getHelmListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * example : helm status RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.HelmStatusResponse helmStatus(run.acloud.protobuf.chart.Package.HelmStatusRequest request) {
      return blockingUnaryCall(
          getChannel(), getHelmStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * example : helm install [NAME] [CHART]
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.HelmInstallResponse helmInstall(run.acloud.protobuf.chart.Package.HelmInstallRequest request) {
      return blockingUnaryCall(
          getChannel(), getHelmInstallMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * example : helm upgrade [NAME] [CHART]
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.HelmUpgradeResponse helmUpgrade(run.acloud.protobuf.chart.Package.HelmUpgradeRequest request) {
      return blockingUnaryCall(
          getChannel(), getHelmUpgradeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * example : helm rollback &lt;RELEASE&gt; [REVISION]
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.HelmRollbackResponse helmRollback(run.acloud.protobuf.chart.Package.HelmRollbackRequest request) {
      return blockingUnaryCall(
          getChannel(), getHelmRollbackMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * example : helm uninstall RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.HelmHistoryResponse helmHistory(run.acloud.protobuf.chart.Package.HelmHistoryRequest request) {
      return blockingUnaryCall(
          getChannel(), getHelmHistoryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * example : helm uninstall RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public run.acloud.protobuf.chart.Package.HelmUninstallResponse helmUninstall(run.acloud.protobuf.chart.Package.HelmUninstallRequest request) {
      return blockingUnaryCall(
          getChannel(), getHelmUninstallMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty healthy(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getHealthyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty readiness(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getReadinessMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ChartAPIFutureStub extends io.grpc.stub.AbstractStub<ChartAPIFutureStub> {
    private ChartAPIFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ChartAPIFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChartAPIFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ChartAPIFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * chart 목록 조회
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.RepoChartsResponse> searchRepoCharts(
        run.acloud.protobuf.chart.Package.RepoChartsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSearchRepoChartsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * chart 버젼 목록 조회
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.ChartVersionsResponse> chartVersions(
        run.acloud.protobuf.chart.Package.ChartVersionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getChartVersionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * chart 상세 내용 조회
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.ChartVersionDetailResponse> chartVersion(
        run.acloud.protobuf.chart.Package.GetChartVersionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getChartVersionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Helm Repository List
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.ReposResponse> repos(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getReposMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * example : helm list -n cocktail-addon
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.HelmListResponse> helmList(
        run.acloud.protobuf.chart.Package.HelmListRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHelmListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * example : helm status RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.HelmStatusResponse> helmStatus(
        run.acloud.protobuf.chart.Package.HelmStatusRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHelmStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * example : helm install [NAME] [CHART]
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.HelmInstallResponse> helmInstall(
        run.acloud.protobuf.chart.Package.HelmInstallRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHelmInstallMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * example : helm upgrade [NAME] [CHART]
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.HelmUpgradeResponse> helmUpgrade(
        run.acloud.protobuf.chart.Package.HelmUpgradeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHelmUpgradeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * example : helm rollback &lt;RELEASE&gt; [REVISION]
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.HelmRollbackResponse> helmRollback(
        run.acloud.protobuf.chart.Package.HelmRollbackRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHelmRollbackMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * example : helm uninstall RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.HelmHistoryResponse> helmHistory(
        run.acloud.protobuf.chart.Package.HelmHistoryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHelmHistoryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * example : helm uninstall RELEASE_NAME -n cocktail-addon
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.chart.Package.HelmUninstallResponse> helmUninstall(
        run.acloud.protobuf.chart.Package.HelmUninstallRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHelmUninstallMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> healthy(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getHealthyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> readiness(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getReadinessMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEARCH_REPO_CHARTS = 0;
  private static final int METHODID_CHART_VERSIONS = 1;
  private static final int METHODID_CHART_VERSION = 2;
  private static final int METHODID_REPOS = 3;
  private static final int METHODID_HELM_LIST = 4;
  private static final int METHODID_HELM_STATUS = 5;
  private static final int METHODID_HELM_INSTALL = 6;
  private static final int METHODID_HELM_UPGRADE = 7;
  private static final int METHODID_HELM_ROLLBACK = 8;
  private static final int METHODID_HELM_HISTORY = 9;
  private static final int METHODID_HELM_UNINSTALL = 10;
  private static final int METHODID_HEALTHY = 11;
  private static final int METHODID_READINESS = 12;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ChartAPIImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ChartAPIImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEARCH_REPO_CHARTS:
          serviceImpl.searchRepoCharts((run.acloud.protobuf.chart.Package.RepoChartsRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.RepoChartsResponse>) responseObserver);
          break;
        case METHODID_CHART_VERSIONS:
          serviceImpl.chartVersions((run.acloud.protobuf.chart.Package.ChartVersionsRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ChartVersionsResponse>) responseObserver);
          break;
        case METHODID_CHART_VERSION:
          serviceImpl.chartVersion((run.acloud.protobuf.chart.Package.GetChartVersionRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ChartVersionDetailResponse>) responseObserver);
          break;
        case METHODID_REPOS:
          serviceImpl.repos((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.ReposResponse>) responseObserver);
          break;
        case METHODID_HELM_LIST:
          serviceImpl.helmList((run.acloud.protobuf.chart.Package.HelmListRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmListResponse>) responseObserver);
          break;
        case METHODID_HELM_STATUS:
          serviceImpl.helmStatus((run.acloud.protobuf.chart.Package.HelmStatusRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmStatusResponse>) responseObserver);
          break;
        case METHODID_HELM_INSTALL:
          serviceImpl.helmInstall((run.acloud.protobuf.chart.Package.HelmInstallRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmInstallResponse>) responseObserver);
          break;
        case METHODID_HELM_UPGRADE:
          serviceImpl.helmUpgrade((run.acloud.protobuf.chart.Package.HelmUpgradeRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmUpgradeResponse>) responseObserver);
          break;
        case METHODID_HELM_ROLLBACK:
          serviceImpl.helmRollback((run.acloud.protobuf.chart.Package.HelmRollbackRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmRollbackResponse>) responseObserver);
          break;
        case METHODID_HELM_HISTORY:
          serviceImpl.helmHistory((run.acloud.protobuf.chart.Package.HelmHistoryRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmHistoryResponse>) responseObserver);
          break;
        case METHODID_HELM_UNINSTALL:
          serviceImpl.helmUninstall((run.acloud.protobuf.chart.Package.HelmUninstallRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.chart.Package.HelmUninstallResponse>) responseObserver);
          break;
        case METHODID_HEALTHY:
          serviceImpl.healthy((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_READINESS:
          serviceImpl.readiness((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ChartAPIBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ChartAPIBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return run.acloud.protobuf.chart.Package.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ChartAPI");
    }
  }

  private static final class ChartAPIFileDescriptorSupplier
      extends ChartAPIBaseDescriptorSupplier {
    ChartAPIFileDescriptorSupplier() {}
  }

  private static final class ChartAPIMethodDescriptorSupplier
      extends ChartAPIBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ChartAPIMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ChartAPIGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ChartAPIFileDescriptorSupplier())
              .addMethod(getSearchRepoChartsMethod())
              .addMethod(getChartVersionsMethod())
              .addMethod(getChartVersionMethod())
              .addMethod(getReposMethod())
              .addMethod(getHelmListMethod())
              .addMethod(getHelmStatusMethod())
              .addMethod(getHelmInstallMethod())
              .addMethod(getHelmUpgradeMethod())
              .addMethod(getHelmRollbackMethod())
              .addMethod(getHelmHistoryMethod())
              .addMethod(getHelmUninstallMethod())
              .addMethod(getHealthyMethod())
              .addMethod(getReadinessMethod())
              .build();
        }
      }
    }
    return result;
  }
}
