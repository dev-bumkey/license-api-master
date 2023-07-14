package run.acloud.api.catalog.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import run.acloud.commons.util.Utils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;
import run.acloud.protobuf.chart.ChartAPIGrpc;
import run.acloud.protobuf.chart.Package;

import java.util.concurrent.TimeUnit;

@Slf4j
public class ChartApiGrpcClient {
    private final ManagedChannel channel;
    private final ChartAPIGrpc.ChartAPIBlockingStub blockingStub;

    /** Construct client connecting to server at {@code host:port}. */
    public ChartApiGrpcClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build());
    }

    /** Construct client for accessing server using the existing channel. */
    ChartApiGrpcClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = ChartAPIGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** chartVersions */
    public Package.ChartVersionsResponse chartVersions(Package.ChartVersionsRequest request) {
        Package.ChartVersionsResponse response;

        try {
            response = blockingStub.chartVersions(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response;
    }

    /** chartVersion */
    public Package.ChartVersionDetails chartVersion(Package.GetChartVersionRequest request) {
        Package.ChartVersionDetailResponse response;

        try {
            response = blockingStub.chartVersion(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response.getResult();
    }

    /** helmList */
    public Package.HelmListResponse helmList(Package.HelmListRequest request) {
        Package.HelmListResponse response;

        try {
            response = blockingStub.helmList(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response;
    }

    /** helmStatus */
    public Package.HelmStatusResponse helmStatus(Package.HelmStatusRequest request) {
        Package.HelmStatusResponse response;

        try {
            response = blockingStub.helmStatus(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response;
    }

    /** helmHistory */
    public Package.HelmHistoryResponse helmHistory(Package.HelmHistoryRequest request) {
        Package.HelmHistoryResponse response;

        try {
            response = blockingStub.helmHistory(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response;
    }

    /** helmInstall */
    public Package.HelmInstallResponse helmInstall(Package.HelmInstallRequest request) {
        Package.HelmInstallResponse response;

        try {
            response = blockingStub.helmInstall(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response;
    }

    /** helmUninstall */
    public Package.HelmUninstallResponse helmUninstall(Package.HelmUninstallRequest request) {
        Package.HelmUninstallResponse response;

        try {
            response = blockingStub.helmUninstall(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response;
    }

    /** helmUpgrade */
    public Package.HelmUpgradeResponse helmUpgrade(Package.HelmUpgradeRequest request) {
        Package.HelmUpgradeResponse response;

        try {
            response = blockingStub.helmUpgrade(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response;
    }

    /** helmRollback*/
    public Package.HelmRollbackResponse helmRollback(Package.HelmRollbackRequest request) {
        Package.HelmRollbackResponse response;

        try {
            response = blockingStub.helmRollback(request);
        } catch (StatusRuntimeException e) {
            this.printStatusRuntimeException(e);
            throw e;
        }

        return response;
    }

    /** Print Error **/
    private void printStatusRuntimeException(StatusRuntimeException e) {
        CocktailException ce = new CocktailException("Package service connection failed.", e, ExceptionType.ExternalApiFail_PackageApi);
        log.error(ExceptionMessageUtils.setCommonResult(Utils.getCurrentRequest(), null, ce, false));
    }
}
