package szelink.mt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import szelink.mt.dao.DataBackUpRepository;
import szelink.mt.entity.DataBackUpInfo;

import java.util.Date;
import java.util.List;

/**
 * @author mt
 * mysql 备份数据service
 */
@Service("dataBackUpService")
@Transactional(rollbackFor = Exception.class)
public class DataBackUpService {

    @Autowired
    @Qualifier("bakinfoDao")
    private DataBackUpRepository dao;

    /**
     * 插入一条已经备份了的文件信息
     * @param info
     */
    public void save(DataBackUpInfo info) {
        dao.save(info);
    }

    /**
     * 查询所有已经备份的文件信息
     */
    public List<DataBackUpInfo> list() {
        Sort sort = new Sort(Sort.Direction.DESC, "backUpTime");
        return dao.findAll(sort);
    }

    public void deleteById(String id) {
        dao.deleteById(id);
    }

    public int count() {
        Long count = dao.count();
        return count.intValue();
    }

    public DataBackUpInfo getOldestInfo() {
        return dao.getOldestInfo();
    }

    public List<DataBackUpInfo> getInfosLessThan(Date date) {
        return dao.getInfosLessThan(date);
    }

    public DataBackUpInfo getLastTimeInfo(Date date) {
        return dao.getLastTimeInfo(date);
    }

}
