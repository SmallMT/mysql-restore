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
     * 获取时间最早的的备份信息
     * @return
     */
    @Query(value = "select * from t_backup_info order by backup_time limit 0,1", nativeQuery = true)
    DataBackUpInfo getOldestInfo();

    /**
     * 获取时间小于指定的时间的所有备份信息
     * @param date
     * @return
     */
    @Query(value = "select * from t_backup_info where backup_time < :d", nativeQuery = true)
    List<DataBackUpInfo> getInfosLessThan(@Param("d") Date date);

    /**
     * 获取上一次的备份信息
     * @param date 参照点(本次)的备份时间
     * @return
     */
    @Query(value = "select * from t_backup_info where backup_time < :d order by backup_time desc limit 0,1", nativeQuery = true)
    DataBackUpInfo getLastTimeInfo(@Param("d") Date date);

}
