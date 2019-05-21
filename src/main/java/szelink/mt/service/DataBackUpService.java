package szelink.mt.service;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import szelink.mt.dao.DataBackUpRepository;
import szelink.mt.entity.DataBackUpInfo;

import java.io.File;
import java.io.IOException;
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

    /**
     * 获取上一次备份的文件信息
     * @return
     */
    public DataBackUpInfo getLastInfo() {
        return dao.getLastInfo();
    }

    /**
     * 删除上一次备份的文件相关信息(删除数据库中的信息以及备份的文件)
     */
    public void removeLastInfo(DataBackUpInfo last) {
        // 1.删除数据库中的信息
        dao.deleteById(last.getId());
        // 2.删除备份的文件
        try {
            FileUtils.forceDelete(new File(last.getFilePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
