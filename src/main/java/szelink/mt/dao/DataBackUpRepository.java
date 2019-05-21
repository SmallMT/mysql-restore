package szelink.mt.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import szelink.mt.entity.DataBackUpInfo;

import java.util.Date;
import java.util.List;

/**
 * @author mt
 * mysql 备份数据dao
 */
@Repository("bakinfoDao")
public interface DataBackUpRepository extends JpaRepository<DataBackUpInfo,String> {

    /**
     * 返回最近一次备份文件的信息
     * @return
     */
    @Query(value = "select * from t_backup_info order by backup_time asc limit 0,1", nativeQuery = true)
    DataBackUpInfo getLastInfo();

}
