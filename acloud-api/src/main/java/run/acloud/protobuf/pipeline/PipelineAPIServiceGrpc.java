package run.acloud.protobuf.pipeline;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.16.1)",
    comments = "Source: v1beta1/pipeline/pipeline.proto")
public final class PipelineAPIServiceGrpc {

  private PipelineAPIServiceGrpc() {}

  public static final String SERVICE_NAME = "pipeline.PipelineAPIService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> getRunMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Run",
      requestType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline.class,
      responseType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> getRunMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> getRunMethod;
    if ((getRunMethod = PipelineAPIServiceGrpc.getRunMethod) == null) {
      synchronized (PipelineAPIServiceGrpc.class) {
        if ((getRunMethod = PipelineAPIServiceGrpc.getRunMethod) == null) {
          PipelineAPIServiceGrpc.getRunMethod = getRunMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "pipeline.PipelineAPIService", "Run"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline.getDefaultInstance()))
                  .setSchemaDescriptor(new PipelineAPIServiceMethodDescriptorSupplier("Run"))
                  .build();
          }
        }
     }
     return getRunMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> getStopMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Stop",
      requestType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline.class,
      responseType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> getStopMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> getStopMethod;
    if ((getStopMethod = PipelineAPIServiceGrpc.getStopMethod) == null) {
      synchronized (PipelineAPIServiceGrpc.class) {
        if ((getStopMethod = PipelineAPIServiceGrpc.getStopMethod) == null) {
          PipelineAPIServiceGrpc.getStopMethod = getStopMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "pipeline.PipelineAPIService", "Stop"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline.getDefaultInstance()))
                  .setSchemaDescriptor(new PipelineAPIServiceMethodDescriptorSupplier("Stop"))
                  .build();
          }
        }
     }
     return getStopMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Delete",
      requestType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest.class,
      responseType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse> getDeleteMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse> getDeleteMethod;
    if ((getDeleteMethod = PipelineAPIServiceGrpc.getDeleteMethod) == null) {
      synchronized (PipelineAPIServiceGrpc.class) {
        if ((getDeleteMethod = PipelineAPIServiceGrpc.getDeleteMethod) == null) {
          PipelineAPIServiceGrpc.getDeleteMethod = getDeleteMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "pipeline.PipelineAPIService", "Delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PipelineAPIServiceMethodDescriptorSupplier("Delete"))
                  .build();
          }
        }
     }
     return getDeleteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse> getInitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Init",
      requestType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest.class,
      responseType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse> getInitMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse> getInitMethod;
    if ((getInitMethod = PipelineAPIServiceGrpc.getInitMethod) == null) {
      synchronized (PipelineAPIServiceGrpc.class) {
        if ((getInitMethod = PipelineAPIServiceGrpc.getInitMethod) == null) {
          PipelineAPIServiceGrpc.getInitMethod = getInitMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "pipeline.PipelineAPIService", "Init"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PipelineAPIServiceMethodDescriptorSupplier("Init"))
                  .build();
          }
        }
     }
     return getInitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse> getTerminateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Terminate",
      requestType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest.class,
      responseType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse> getTerminateMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse> getTerminateMethod;
    if ((getTerminateMethod = PipelineAPIServiceGrpc.getTerminateMethod) == null) {
      synchronized (PipelineAPIServiceGrpc.class) {
        if ((getTerminateMethod = PipelineAPIServiceGrpc.getTerminateMethod) == null) {
          PipelineAPIServiceGrpc.getTerminateMethod = getTerminateMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "pipeline.PipelineAPIService", "Terminate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PipelineAPIServiceMethodDescriptorSupplier("Terminate"))
                  .build();
          }
        }
     }
     return getTerminateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong> getHealthCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HealthCheck",
      requestType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping.class,
      responseType = run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping,
      run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong> getHealthCheckMethod() {
    io.grpc.MethodDescriptor<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong> getHealthCheckMethod;
    if ((getHealthCheckMethod = PipelineAPIServiceGrpc.getHealthCheckMethod) == null) {
      synchronized (PipelineAPIServiceGrpc.class) {
        if ((getHealthCheckMethod = PipelineAPIServiceGrpc.getHealthCheckMethod) == null) {
          PipelineAPIServiceGrpc.getHealthCheckMethod = getHealthCheckMethod = 
              io.grpc.MethodDescriptor.<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping, run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "pipeline.PipelineAPIService", "HealthCheck"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong.getDefaultInstance()))
                  .setSchemaDescriptor(new PipelineAPIServiceMethodDescriptorSupplier("HealthCheck"))
                  .build();
          }
        }
     }
     return getHealthCheckMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PipelineAPIServiceStub newStub(io.grpc.Channel channel) {
    return new PipelineAPIServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PipelineAPIServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PipelineAPIServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PipelineAPIServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PipelineAPIServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class PipelineAPIServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void run(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> responseObserver) {
      asyncUnimplementedUnaryCall(getRunMethod(), responseObserver);
    }

    /**
     */
    public void stop(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> responseObserver) {
      asyncUnimplementedUnaryCall(getStopMethod(), responseObserver);
    }

    /**
     */
    public void delete(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }

    /**
     */
    public void init(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getInitMethod(), responseObserver);
    }

    /**
     */
    public void terminate(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTerminateMethod(), responseObserver);
    }

    /**
     */
    public void healthCheck(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong> responseObserver) {
      asyncUnimplementedUnaryCall(getHealthCheckMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRunMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline,
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline>(
                  this, METHODID_RUN)))
          .addMethod(
            getStopMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline,
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline>(
                  this, METHODID_STOP)))
          .addMethod(
            getDeleteMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest,
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse>(
                  this, METHODID_DELETE)))
          .addMethod(
            getInitMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest,
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse>(
                  this, METHODID_INIT)))
          .addMethod(
            getTerminateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest,
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse>(
                  this, METHODID_TERMINATE)))
          .addMethod(
            getHealthCheckMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping,
                run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong>(
                  this, METHODID_HEALTH_CHECK)))
          .build();
    }
  }

  /**
   */
  public static final class PipelineAPIServiceStub extends io.grpc.stub.AbstractStub<PipelineAPIServiceStub> {
    private PipelineAPIServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PipelineAPIServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PipelineAPIServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PipelineAPIServiceStub(channel, callOptions);
    }

    /**
     */
    public void run(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRunMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void stop(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getStopMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void delete(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void init(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void terminate(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTerminateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void healthCheck(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping request,
        io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PipelineAPIServiceBlockingStub extends io.grpc.stub.AbstractStub<PipelineAPIServiceBlockingStub> {
    private PipelineAPIServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PipelineAPIServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PipelineAPIServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PipelineAPIServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline run(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline request) {
      return blockingUnaryCall(
          getChannel(), getRunMethod(), getCallOptions(), request);
    }

    /**
     */
    public run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline stop(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline request) {
      return blockingUnaryCall(
          getChannel(), getStopMethod(), getCallOptions(), request);
    }

    /**
     */
    public run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse delete(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }

    /**
     */
    public run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse init(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest request) {
      return blockingUnaryCall(
          getChannel(), getInitMethod(), getCallOptions(), request);
    }

    /**
     */
    public run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse terminate(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest request) {
      return blockingUnaryCall(
          getChannel(), getTerminateMethod(), getCallOptions(), request);
    }

    /**
     */
    public run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong healthCheck(run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping request) {
      return blockingUnaryCall(
          getChannel(), getHealthCheckMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PipelineAPIServiceFutureStub extends io.grpc.stub.AbstractStub<PipelineAPIServiceFutureStub> {
    private PipelineAPIServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PipelineAPIServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PipelineAPIServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PipelineAPIServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> run(
        run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline request) {
      return futureUnaryCall(
          getChannel().newCall(getRunMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline> stop(
        run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline request) {
      return futureUnaryCall(
          getChannel().newCall(getStopMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse> delete(
        run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse> init(
        run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getInitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse> terminate(
        run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTerminateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong> healthCheck(
        run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping request) {
      return futureUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_RUN = 0;
  private static final int METHODID_STOP = 1;
  private static final int METHODID_DELETE = 2;
  private static final int METHODID_INIT = 3;
  private static final int METHODID_TERMINATE = 4;
  private static final int METHODID_HEALTH_CHECK = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PipelineAPIServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PipelineAPIServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_RUN:
          serviceImpl.run((run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline>) responseObserver);
          break;
        case METHODID_STOP:
          serviceImpl.stop((run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pipeline>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineDeleteResponse>) responseObserver);
          break;
        case METHODID_INIT:
          serviceImpl.init((run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineInitResponse>) responseObserver);
          break;
        case METHODID_TERMINATE:
          serviceImpl.terminate((run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateRequest) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.PipelineTerminateResponse>) responseObserver);
          break;
        case METHODID_HEALTH_CHECK:
          serviceImpl.healthCheck((run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Ping) request,
              (io.grpc.stub.StreamObserver<run.acloud.protobuf.pipeline.PipelineAPIServiceProto.Pong>) responseObserver);
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

  private static abstract class PipelineAPIServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PipelineAPIServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return run.acloud.protobuf.pipeline.PipelineAPIServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PipelineAPIService");
    }
  }

  private static final class PipelineAPIServiceFileDescriptorSupplier
      extends PipelineAPIServiceBaseDescriptorSupplier {
    PipelineAPIServiceFileDescriptorSupplier() {}
  }

  private static final class PipelineAPIServiceMethodDescriptorSupplier
      extends PipelineAPIServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PipelineAPIServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (PipelineAPIServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PipelineAPIServiceFileDescriptorSupplier())
              .addMethod(getRunMethod())
              .addMethod(getStopMethod())
              .addMethod(getDeleteMethod())
              .addMethod(getInitMethod())
              .addMethod(getTerminateMethod())
              .addMethod(getHealthCheckMethod())
              .build();
        }
      }
    }
    return result;
  }
}
