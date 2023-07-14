package run.acloud.api.catalog.dao;

import run.acloud.api.catalog.vo.PackageDeployHistoryVO;

import java.util.List;
import java.util.Map;

/**
 * @author gun@acornsoft.io
 * Created on 2019. 12. 16.
 */
public interface IPackageMapper {
    List<PackageDeployHistoryVO> getPackageDeployHistories(Map<String, Object> params) throws Exception;
    PackageDeployHistoryVO getPackageDeployHistory(Map<String, Object> params) throws Exception;
    int addPackageDeployHistory(PackageDeployHistoryVO packageDeployHistory) throws Exception;
}
