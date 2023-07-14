package run.acloud.commons.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterVolumeService;
import run.acloud.api.configuration.service.ProviderAccountService;
import run.acloud.commons.vo.CocktailInitVO;

@Slf4j
@Service
public class CocktailInitService {
//	@Resource(name = "cocktailSession")
//    private SqlSessionTemplate cocktailSqlSession;
//
//    @Resource(name = "buildSession")
//    private SqlSessionTemplate buildSqlSession;

    @Autowired
    private ProviderAccountService providerAccountService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterVolumeService clusterVolumeService;


    @Transactional(transactionManager = "transactionManager")
    public void initOfCocktail(String version, CocktailInitVO cocktailInit) throws Exception {

        if(StringUtils.equals("3.0.0", version)){
//            if(cocktailInit.getProviderAccount() != null){
//                cocktailInit.getProviderAccount().setCreator(1);
//                cocktailInit.getProviderAccount().setUpdater(1);
//                cocktailInit.getProviderAccount().setUseYn("Y");
//                providerAccountService.addProviderAccount(cocktailInit.getProviderAccount());
//            }
//
//            if(cocktailInit.getCluster() != null){
//                cocktailInit.getCluster().setCreator(1);
//                cocktailInit.getCluster().setUpdater(1);
//                cocktailInit.getCluster().setUseYn("Y");
//                clusterService.addCluster(cocktailInit.getCluster());
//            }
//
//            if(CollectionUtils.isNotEmpty(cocktailInit.getStorages())){
//                for(ClusterVolumeVO clusterVolumeRow : cocktailInit.getStorages()){
//                    clusterVolumeRow.setCreator(1);
//                    clusterVolumeRow.setUpdater(1);
//                    clusterVolumeRow.setUseYn("Y");
//                    clusterVolumeService.addClusterVolume(clusterVolumeRow, false);
//                }
//            }

        }
    }


}
