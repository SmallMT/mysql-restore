package szelink.mt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import szelink.mt.dao.BinlogEventRepository;
import szelink.mt.entity.BinlogEventInfo;
import szelink.mt.entity.BinlogFileInfo;
import szelink.mt.util.JdbcUtils;

import java.util.List;

/**
 * @author mt
 * binlog event service 层
 */
@Service("binlogEventService")
@Transactional(rollbackFor = Exception.class)
public class BinlogEventService {

    @Autowired
    @Qualifier("binlogEventDao")
    private BinlogEventRepository dao;

    /**
     * 保存一条event信息
     * @param info
     */
    public void save(BinlogEventInfo info) {
        dao.save(info);
    }

    /**
     * @return
     */
    public BinlogEventInfo recentEvent() {
        List<BinlogFileInfo> indexs = JdbcUtils.binlogIndexInfo();
        String recentBinlog = indexs.get(indexs.size() - 1).getBinlogName();
        return dao.recentBinlog(recentBinlog);
    }

    /**
     * 根据id查询某个event信息
     * @param eventId
     * @return
     */
    public BinlogEventInfo queryEventInfoById(String eventId) {
        return dao.findById(eventId).get();
    }

    /**
     * 查询最近 n条event事件
     * @param num 要查看的数量
     * @return
     */
    public List<BinlogEventInfo> recentEventInfo(int skip, int num) {
        List<BinlogEventInfo> infos = dao.recentBinlog(skip, num);
        return infos;
    }

    /**
     * 查询 参照点之前的事件信息
     * @param reference 参照点
     * @param skip 分页参数 跳过的指定数量
     * @param num 分页参数,需要查询的数量
     * @return
     */
    public List<BinlogEventInfo> queryEventsBefore(BinlogEventInfo reference, int skip, int num) {
        return dao.eventsBefore(reference.getBinlogFileName(), reference.getStartPosition(), skip, num);
    }

    /**
     * 查询某个参照点之前的所有的数据的数量
     * @param reference 参照点
     * @return 某个参照点之前的事件数量
     */
    public Long eventsBeforeCount(BinlogEventInfo reference) {
        return dao.eventsBeforeCount(reference.getBinlogFileName(), reference.getStartPosition());
    }

    /**
     * 统计所有的event个数
     * @return
     */
    public Long countsAll() {
        return dao.count();
    }

    /**
     * 清空所有的event
     */
    public void clear() {
        dao.deleteAll();
    }
}
