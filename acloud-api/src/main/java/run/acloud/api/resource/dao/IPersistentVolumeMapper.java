package run.acloud.api.resource.dao;

import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.resource.vo.PersistentVolumeClaimGuiVO;
import run.acloud.api.resource.vo.PersistentVolumeParamterVO;
import run.acloud.api.resource.vo.PersistentVolumeVO;

import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 20.
 */
public interface IPersistentVolumeMapper {
    List<PersistentVolumeVO> getPersistentVolumes();
    List<PersistentVolumeVO> getPersistentVolumesOfCluster(Integer clusterSeq);
    Integer addPersistentVolume(PersistentVolumeVO pv);
    void addPersistentVolumeParameters(List<PersistentVolumeParamterVO> paramters);
    PersistentVolumeVO getPersistentVolume(Integer volumeSeq);
    PersistentVolumeVO getPersistentVolumeInClusterByName(Map<String, Object> params);
    Integer updatePersistentVolumeUseYn(PersistentVolumeVO pv);
    Integer updatePersistentVolumeBoundYn(PersistentVolumeVO pv);

    Integer addPersistentVolumeClaim(PersistentVolumeClaimGuiVO pvc);
    List<PersistentVolumeClaimGuiVO> getPersistentVolumeClaimsOfCluster(Integer clusterSeq);
    PersistentVolumeClaimGuiVO getPersistentVolumeClaim(Integer claimSeq);

    List<CodeVO> getVolumePluginCodes();
}
