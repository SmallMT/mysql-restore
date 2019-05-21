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
     * 查询某个binlog文件的最近的一条记录
     * @param binlog
     * @return
     */
    @Query(value = "select * from t_event_info where binlog_file_name = :binlog order by end_position desc limit 0,1", nativeQuery = true)
    BinlogEventInfo recentBinlog(@Param("binlog") String binlog);


    /**
     * 查询最近的binlog 事件
     * @param skip 分页参数 需要跳过的事件数量
     * @param num 分页参数 需要查询的事件数量
     * @return
     */
    @Query(value = "select * from t_event_info order by binlog_file_name desc,end_position desc limit :skip,:num", nativeQuery = true)
    List<BinlogEventInfo> recentBinlog(@Param("skip") int skip,@Param("num") int num);

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

}
