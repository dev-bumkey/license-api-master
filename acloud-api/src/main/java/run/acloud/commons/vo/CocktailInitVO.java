package run.acloud.commons.vo;

import lombok.Data;
import lombok.NonNull;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.configuration.vo.ClusterAddVO;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.configuration.vo.ProviderAccountVO;

import java.io.Serializable;
import java.util.List;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 18.
 */
@Data
public class CocktailInitVO implements Serializable {

    @NotBlank
    private String token;

    @NonNull
    private ProviderAccountVO providerAccount;

    @NonNull
    private ClusterAddVO cluster;

    @NonNull
    private List<ClusterVolumeVO> storages;
}
