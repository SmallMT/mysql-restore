package szelink.mt.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import szelink.mt.config.DataBackUpConfig;
import szelink.mt.entity.BinlogEventInfo;
import szelink.mt.entity.DataBackUpInfo;
import szelink.mt.service.BinlogEventService;
import szelink.mt.service.DataBackUpService;
import szelink.mt.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author mt
 * mysql 数据库备份控制器
 */
@Controller
@RequestMapping("/bak")
public class BackUpController {

    private static final Logger logger = LoggerFactory.getLogger(BackUpController.class);

    /**
     * 此list中存放了恢复数据时,用户选择排除掉(不执行的sql) id
     */
    private static final List<String> excludeSqls = new LinkedList<>();

    /**
     * 排除掉的sql 列表在session中的键名
     */
    private static final String SESSION_EXCLUDE_SQLS = "excludeSqls";

    private static String REFERENCE_ID = "";

    @Autowired
    @Qualifier("dataBackUpService")
    private DataBackUpService backUpservice;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RepairData repair;

    @Autowired
    @Qualifier("binlogEventService")
    private BinlogEventService eventService;

    @Autowired
    @Qualifier("backUpConfig")
    private DataBackUpConfig config;

    /**
     * 已备份过的数据信息的列表页面
     * @return
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<DataBackUpInfo> infos = backUpservice.list();
        model.addAttribute("infos", infos);
        return "backuplist";
    }


    @GetMapping("/showbinlog")
    @ResponseBody
    public String showBinlog() throws UnsupportedEncodingException {
        String sql = "select * from t_event_info_nov where id=  '8e8174f565de48d89048ff6e0de1b2d8'";
//        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(sql);
        Map<String,Object> map = jdbcTemplate.queryForMap(sql);
        byte[] buf = (byte[]) map.get("SQL");
        return "show binlog page";
    }

    @GetMapping("/repair")
    public String repair() {
        // 查找最近一次整体备份的文件
        DataBackUpInfo dataBackUpInfo = backUpservice.getOldestInfo();
        BinlogEventInfo reference = eventService.queryEventInfoById(REFERENCE_ID);
        String filePath = dataBackUpInfo.getFilePath();
        File hasBackupFile = new File(dataBackUpInfo.getFilePath());
        if (excludeSqls.size() == 0) {
            // 没有选择排除的sql语句
            repair.repair(hasBackupFile, dataBackUpInfo.getBinlogFileName(), dataBackUpInfo.getPosition(),
                    reference.getBinlogFileName(), reference.getEndPosition());
        } else {
            // 选择了排除不执行的sql语句
        }
        // 2.导出需要执行的sql语句
        // 3.根据最近的一次备份来进行数据恢复
        // 4.清空h2数据库中的event信息

//        File zipFile = new File("E:\\databak\\2018-12-12-10-52-57.zip");
//        repair.repair(zipFile, "mysql-bin.000001", 120L,
//                "mysql-bin.000001", 726L);
        return "redirect:/bak/events";
    }

    /**
     * 列表查询mysql 最近执行了的 15条sql语句
     * @return
     */
    @GetMapping("/events")
    public String listEvent(HttpServletRequest request, Model model) {
        int skip = Integer.parseInt(request.getParameter("skip") == null ? "0" : request.getParameter("skip"));
        int num = Integer.parseInt(request.getParameter("num") == null ? "10" : request.getParameter("num"));
        int curr = Integer.parseInt(request.getParameter("curr") == null ? "1" : request.getParameter("curr"));
        Long allEventCounts = eventService.countsAll();
        List<BinlogEventInfo> events = eventService.recentEventInfo(skip, num);
        Map<String,String> idSqlPair = new HashMap<>(16);
        dealWithSql(events, idSqlPair);
        model.addAttribute("idSqlPair", idSqlPair);
        model.addAttribute("events", events);
        model.addAttribute("count", allEventCounts);
        model.addAttribute("curr", curr);
        return "eventList";
    }

    /**
     * 大型sql语句在页面中无法直接查看
     * 以文件下载的形式导出大型的sql语句
     * @param binlog
     * @param startPos
     * @param response
     */
    @GetMapping("/bigsql")
    @ResponseBody
    public void loadBigSql(@RequestParam("binlog") String binlog, @RequestParam("startPos") Long startPos, HttpServletResponse response) {
        // 使用工具导出大型sql文件到指定文件中
        String bigSqlDir = config.getPath() + File.separator + "bigsql";
        File bigSqlDirFile = new File(bigSqlDir);
        if (!bigSqlDirFile.exists()) {
            bigSqlDirFile.mkdir();
        }
        String sqlFileName = binlog + "-" + startPos + ".sql";
        String sqlFile = bigSqlDir + File.separator + sqlFileName;
        response.addHeader("Content-Disposition", "attachment;fileName=" + sqlFileName);
        // 下载文件
        byte[] buf = new byte[1024];
        InputStream in = null;
        OutputStream out = null;
        int len;
        try {
            BinlogEventUtil.exportBinlogSql(binlog, startPos, sqlFile);
            in = new FileInputStream(new File(sqlFile));
            out = response.getOutputStream();
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0 ,len);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 前台以ajax的形式根据 event id来查询某个binlog事件的具体信息
     * @param eventId
     * @param response
     */
    @GetMapping("/event")
    @ResponseBody
    public void queryEventAjax(@RequestParam("id") String eventId, HttpServletResponse response) {
        BinlogEventInfo event = eventService.queryEventInfoById(eventId);
        // 处理sql语句
        JSONObject obj = JSON.parseObject(JSONObject.toJSONString(event));
        String sql = event.getSql() == null ? "sql语句过大无法显示!!" : new String(event.getSql(), StandardCharsets.UTF_8);
        obj.put("sql", sql);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        obj.put("binlogDate", sdf.format(event.getBinlogDate()));
        PrintWriter writer = null;
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            writer = response.getWriter();
            writer.write(obj.toJSONString());
            writer.flush();
        } catch (IOException e) {
            logger.error("==========向页面写event数据失败==========");
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * 查询某个binlog事件之前的事件信息(15条信息)
     * @param eventId
     * @return
     */
    @GetMapping("/eventBefore")
    public String queryEventsBefore(@RequestParam("id") String eventId, Model model, HttpServletRequest request) {
        // 分页参数
        int skip = Integer.parseInt(request.getParameter("skip") == null ? "0" : request.getParameter("skip"));
        int num = Integer.parseInt(request.getParameter("num") == null ? "10" : request.getParameter("num"));
        int curr = Integer.parseInt(request.getParameter("curr") == null ? "1" : request.getParameter("curr"));
        // 参照事件点,会以该事件为参照点,查询该点之前的事件
        BinlogEventInfo reference = eventService.queryEventInfoById(eventId);
        Long beforeNum = eventService.eventsBeforeCount(reference);
        List<BinlogEventInfo> befores = eventService.queryEventsBefore(reference, skip, num);
        Map<String,String> idSqlPair = new HashMap<>(16);
        dealWithSql(befores, idSqlPair);
        REFERENCE_ID = eventId;
        model.addAttribute("refId", eventId);
        model.addAttribute("events", befores);
        model.addAttribute("idSqlPair", idSqlPair);
        model.addAttribute("curr", curr);
        model.addAttribute("count", beforeNum);
        HttpSession session = request.getSession();
        if (session.getAttribute(SESSION_EXCLUDE_SQLS) == null) {
            session.setAttribute(SESSION_EXCLUDE_SQLS, excludeSqls);
        }
        return "beforeEventList";
    }

    /**
     * 将页面中用户选择排除的id存入列表中
     * @param id 用户选择的id
     */
    @GetMapping("/exclude")
    @ResponseBody
    public void dealWithExcludeSqls(@RequestParam("id") String id) {
        if (excludeSqls.contains(id)) {
            excludeSqls.remove(id);
        } else {
            excludeSqls.add(id);
        }
    }

    @GetMapping("/cleanSql")
    @ResponseBody
    public void cleanExcludeSqls() {
        excludeSqls.clear();
    }

    /**
     * 备份数据库下面的每个database
     * @return
     */
    @GetMapping("/each/database")
    @ResponseBody
    public String bakEachDB() {
        // 1.备份一份原数据库文件
        try {
            DataBackUpUtil.backup(config.getOriginPath(), config.getPath(), "origin");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String suffix = ".zip";
        if (CommonUtil.isWindowsOs()) {
            suffix = ".zip";
        } else {
            suffix = ".tar.gz";
        }
        // 2.分别导出每一个数据库压缩文件
        List<String> databases = JdbcUtils.databases();
        try {
            for (String database : databases) {
                JdbcUtils.exportOneDB(database, config.getOriginPath(), config.getPath(), config.getPath() + "origin" + suffix);
            }
        } catch (IOException e) {
            logger.error("========== 导出数据库文件失败!!! ==========");
            e.printStackTrace();
        }

        return "导出数据库文件成功!!!";
    }

    private void dealWithSql(List<BinlogEventInfo> events, Map<String,String> idSqlPair) {
        if (events != null && !events.isEmpty()) {
            // 处理二进制的sql语句
            for (BinlogEventInfo event : events) {
                if (!event.isBigSql()) {
                    // 当前sql为小型sql,直接从库中获取
                    byte[] sqlBytes = event.getSql();
                    event.setSql(null);
                    idSqlPair.put(event.getId(), new String(sqlBytes, StandardCharsets.UTF_8));
                }
                // 大型sql语句,数据库中没有存放,需要去原始binlog文件中拿取
            }
        }
    }


}


