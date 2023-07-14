package run.acloud.api.pipelineflow.util;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import run.acloud.protobuf.pipeline.PipelineAPIServiceGrpc;
import run.acloud.protobuf.pipeline.PipelineAPIServiceProto;

import java.util.concurrent.TimeUnit;

@Slf4j
public class PipelineAPIServerClient {
    private final ManagedChannel channel;
    private final PipelineAPIServiceGrpc.PipelineAPIServiceBlockingStub blockingStub;

    /** Construct client connecting to server at {@code host:port}. */
    public PipelineAPIServerClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build());
    }

    /** Construct client for accessing BUILD API server using the existing channel. */
    PipelineAPIServerClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = PipelineAPIServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    // pipeline 실행 메서드
    public PipelineAPIServiceProto.Pipeline runPipeline (PipelineAPIServiceProto.Pipeline runRequest) {
        PipelineAPIServiceProto.Pipeline response;

        try {
            response = blockingStub.run(runRequest);
        } catch (StatusRuntimeException e) {
            log.error("fail runPipeline!!", e);
            throw e;
        }

        return response;
    }


    // build 취소시 호출
    public PipelineAPIServiceProto.Pipeline stopBuild (PipelineAPIServiceProto.Pipeline stopRequest) {
        PipelineAPIServiceProto.Pipeline response;

        try {
            response = blockingStub.stop(stopRequest);
        } catch (StatusRuntimeException e) {
            log.error("trace log ", e);
            throw e;
        }

        return response;
    }

    // build 삭제시
    public PipelineAPIServiceProto.PipelineTerminateResponse removeBuild(PipelineAPIServiceProto.PipelineTerminateRequest terminateRequest) {
        PipelineAPIServiceProto.PipelineTerminateResponse response;

        try {
            response = blockingStub.terminate(terminateRequest);
        } catch (StatusRuntimeException e) {
            log.error("trace log ", e);
            throw e;
        }

        return response;
    }

}
