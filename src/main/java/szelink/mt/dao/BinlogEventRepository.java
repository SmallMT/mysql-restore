package szelink.mt.dao;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import szelink.mt.entity.BinlogEventInfo;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * @author mt
 * binlog event dao层
 */
@Repository("binlogEventDao")
public interface BinlogEventRepository extends JpaRepository<BinlogEventInfo,String> {

    /**
     * 查询最大时间
     * @return
     */
    @Query(value = "select max(binlog_date) from t_event_info ", nativeQuery = true)
    Timestamp recentEventDate();

    /**
     * 根据时间查询一个event事件
     * @param date
     * @return
     */
    @Query(value = "select * from t_event_info where binlog_date = :d order by end_position desc limit 0,1", nativeQuery = true)
    BinlogEventInfo queryByBinlogDate(@Param("d") Date date);

    /**
     * 查询某个binlog文件的最近的一条记录
     * @param binlog
     * @return
     */
    @Query(value = "select * from t_event_info where binlog_file_name = :binlog order by end_position desc limit 0,1", nativeQuery = true)
    BinlogEventInfo recentBinlog(@Param("binlog") String binlog);

    /**
     * 查询binlog 名称为 binlog 并且开始位置大于等于 start,结束位置小于等于 end 的事件 每次查询2000条
     * @param binlog
     * @param start
     * @param end
     * @return
     */
    @Query(value = "select * from t_event_info where binlog_file_name = :binlog and start_position >= :startPos and end_position <= :endPos limit 0,2000 order by start_position asc", nativeQuery = true)
    List<BinlogEventInfo> queryEventBy(@Param("binlog") String binlog, @Param("startPos") Long start,
                                       @Param("endPos") Long end);
    /**
     * 查询最近的binlog 事件
     * @param skip 分页参数 需要跳过的事件数量
     * @param num 分页参数 需要查询的事件数量
     * @return
     */
    @Query(value = "select * from t_event_info order by binlog_file_name desc,end_position desc limit :skip,:num", nativeQuery = true)
    List<BinlogEventInfo> recentBinlog(@Param("skip") int skip,@Param("num") int num);

//    @Query(value = "select * from (select * from t_event_info where binlog_file_name <= :binlog) where (binlog_file_name = :binlog and start_position < :start) or (binlog_file_name < :binlog) order by binlog_file_name desc, start_position desc limit 0,15", nativeQuery = true)

    /**
     * 查询某个参照点之前的事件
     * @param binlog 参照点事件 binlogname
     * @param startPos 参照事件 起始点位
     * @param skip 分页参数 需要跳过的数量
     * @param num 分页参数 需要查询的数量
     * @return
     */
    @Query(value = "select * from (select * from t_event_info where (binlog_file_name = :binlog and start_position < :start) or binlog_file_name < :binlog) order by binlog_file_name desc,end_position desc limit :skip,:num", nativeQuery = true)
    List<BinlogEventInfo> eventsBefore(@Param("binlog") String binlog, @Param("start") Long startPos, @Param("skip") int skip, @Param("num") int num);

    /**
     * 查询某个参照点之前的事件数量
     * @param binlog 参照点 binlog name
     * @param startPos 参照点 起始点位
     * @return
     */
    @Query(value = "select count(1) from (select * from t_event_info where (binlog_file_name = :binlog and start_position < :start) or binlog_file_name < :binlog)", nativeQuery = true)
    Long eventsBeforeCount(@Param("binlog") String binlog, @Param("start") Long startPos);

    /**
     * 统计入库时间小于 saveTime 的记录的条数
     * @param saveTime 时间
     * @return
     */
    @Query(value = "select count(1) from t_event_info where save_time <= :saveTime", nativeQuery = true)
    int countEvents(@Param("saveTime") Long saveTime);

    /**
     *
     * @param saveTime
     * @param num
     * @return
     */
    @Query(value = "select * from t_event_info where save_time <= :saveTime order by save_time desc limit 15", nativeQuery = true)
    List<BinlogEventInfo> queryBySaveTime(Long saveTime, int num);
}
