package io.metersphere.service;

import io.metersphere.base.domain.LoadTestReportResult;
import io.metersphere.base.domain.LoadTestReportWithBLOBs;
import io.metersphere.base.domain.Quota;
import io.metersphere.base.mapper.QuotaMapper;
import io.metersphere.base.mapper.ext.ExtLoadTestReportResultMapper;
import io.metersphere.base.mapper.ext.ExtQuotaMapper;
import io.metersphere.commons.constants.ReportKeys;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.JSON;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.dto.ReportTimeInfo;
import io.metersphere.dto.TestOverview;
import io.metersphere.i18n.Translator;
import io.metersphere.quota.constants.QuotaCheckType;
import io.metersphere.quota.service.BaseQuotaManageService;
import io.metersphere.quota.service.BaseQuotaService;
import io.metersphere.request.TestPlanRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author lyh
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class QuotaService {

    @Resource
    private BaseQuotaService baseQuotaService;
    @Resource
    private ExtQuotaMapper extQuotaMapper;
    @Resource
    private BaseQuotaManageService baseQuotaManageService;
    @Resource
    private QuotaMapper quotaMapper;
    @Resource
    private ExtLoadTestReportResultMapper extLoadTestReportResultMapper;

    /**
     * 性能测试配额检查
     * @param request 压力配置
     * @param checkPerformance 是：检查创建数量配额 / 否：检查并发数和时间
     */
    @Transactional(noRollbackFor = MSException.class, rollbackFor = Exception.class)
    public void checkLoadTestQuota(TestPlanRequest request, boolean checkPerformance) {
        String loadConfig = request.getLoadConfiguration();
        int threadNum = 0;
        long duration = 0;
        if (loadConfig != null) {
            threadNum = getIntegerValue(loadConfig, "TargetLevel");
            duration = getIntegerValue(loadConfig, "duration");
        }
        String projectId = request.getProjectId();
        if (checkPerformance) {
            this.checkPerformance(projectId);
        } else {
            checkMaxThread(projectId, threadNum);
            checkDuration(projectId, duration);
        }
    }

    /**
     * 检查vumUsed配额
     * 未超过：返回本次执行预计消耗的配额
     * 超过：抛出异常
     * @param request 压力配置
     * @param projectId 性能测试所属项目ID
     * @return 本次执行预计消耗的配额
     */
    @Transactional(noRollbackFor = MSException.class, rollbackFor = Exception.class)
    public BigDecimal checkVumUsed(TestPlanRequest request, String projectId) {
        BigDecimal toVumUsed = this.calcVum(request.getLoadConfiguration());
        if (toVumUsed.compareTo(BigDecimal.ZERO) == 0) {
            return toVumUsed;
        }

        Quota pjQuota = baseQuotaManageService.getProjectQuota(projectId);
        String pjWarningMsg = Translator.get("quota_vum_used_excess_project");
        boolean isContinue = this.doCheckVumUsed(pjQuota, toVumUsed, pjWarningMsg);

        if (isContinue) {
            String workspaceId = baseQuotaService.queryWorkspaceId(projectId);
            if (StringUtils.isBlank(workspaceId)) {
                return toVumUsed;
            }
            String wsWarningMsg = Translator.get("quota_vum_used_excess_workspace");
            Quota wsQuota = baseQuotaManageService.getWorkspaceQuota(workspaceId);
            if (wsQuota == null) {
                return toVumUsed;
            }
            // 获取工作空间下已经消耗的vum
            Quota qt = extQuotaMapper.getProjectQuotaSum(workspaceId);
            if (qt == null || qt.getVumUsed() == null) {
                wsQuota.setVumUsed(BigDecimal.ZERO);
            } else {
                wsQuota.setVumUsed(qt.getVumUsed());
            }
            this.doCheckVumUsed(wsQuota, toVumUsed, wsWarningMsg);
        }

        return toVumUsed;
    }

    /**
     * 更新VumUsed配额
     * @param projectId 项目ID
     * @param vumUsed 预计使用数量
     */
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void updateVumUsed(String projectId, BigDecimal vumUsed) {
        if (vumUsed == null) {
            LogUtil.info("update vum count fail. vum count is null.");
            return;
        }
        Quota dbPjQuota = extQuotaMapper.getProjectQuota(projectId);
        Quota newPjQuota = this.newPjQuota(projectId, vumUsed);
        this.doUpdateVumUsed(dbPjQuota, newPjQuota);
    }

    /**
     * 回退因主动停止或者测试启动后中途执行异常扣除的VumUsed配额
     * @param report 性能测试报告
     * @return 预计回退数量
     */
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public BigDecimal getReduceVumUsed(LoadTestReportWithBLOBs report) {
        String reportId = report.getId();
        List<LoadTestReportResult> timeInfos = queryReportResult(reportId, ReportKeys.TimeInfo.toString());
        List<LoadTestReportResult> overviews = queryReportResult(reportId, ReportKeys.Overview.toString());

        // 预计使用的数量
        String loadConfig = report.getLoadConfiguration();
        BigDecimal toUsed = calcVum(loadConfig);
        if (CollectionUtils.isEmpty(timeInfos) || CollectionUtils.isEmpty(overviews)) {
            LogUtil.error("reduce vum used error. load test report time info is null.");
            return toUsed;
        }

        ReportTimeInfo timeInfo = parseReportTimeInfo(timeInfos.get(0));
        TestOverview overview = parseOverview(overviews.get(0));

        long duration = timeInfo.getDuration();
        String maxUserStr = overview.getMaxUsers();
        int maxUsers = 0;
        try {
            maxUsers = Integer.parseInt(maxUserStr);
        } catch (Exception e) {
            LogUtil.warn("get max user of overview error.");
        }

        if (duration == 0 || maxUsers == 0) {
            return toUsed;
        }

        // 已经使用的数量
        BigDecimal used = calcVum(maxUsers, duration);
        // 实际使用值比预计值大，不回退，否则回退差值
        return used.compareTo(toUsed) >= 0 ? BigDecimal.ZERO : toUsed.subtract(used);
    }

    private TestOverview parseOverview(LoadTestReportResult reportResult) {
        String content = reportResult.getReportValue();
        TestOverview overview = new TestOverview();
        try {
            overview = JSON.parseObject(content, TestOverview.class);
        } catch (Exception e) {
            LogUtil.error("parse test overview error.");
            LogUtil.error(e.getMessage(), e);
        }
        return overview;
    }

    private List<LoadTestReportResult> queryReportResult(String id, String key) {
        return extLoadTestReportResultMapper.selectByIdAndKey(id, key);
    }

    private ReportTimeInfo parseReportTimeInfo(LoadTestReportResult reportResult) {
        String content = reportResult.getReportValue();
        ReportTimeInfo timeInfo = new ReportTimeInfo();
        try {
            timeInfo = JSON.parseObject(content, ReportTimeInfo.class);
        } catch (Exception e) {
            // 兼容字符串和数字
            Map jsonObject = JSON.parseObject(content, Map.class);
            String startTime = (String) jsonObject.get("startTime");
            String endTime = (String) jsonObject.get("endTime");
            String duration = (String) jsonObject.get("duration");

            SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            try {
                timeInfo.setStartTime(df.parse(startTime).getTime());
                timeInfo.setEndTime(df.parse(endTime).getTime());
                timeInfo.setDuration(Long.parseLong(duration));
            } catch (Exception parseException) {
                LogUtil.error("reduce vum error. parse time info error. " + parseException.getMessage());
            }
        }
        return timeInfo;
    }

    private int getIntegerValue(String loadConfiguration, String key) {
        int s = 0;
        try {
            List jsonArray = JSON.parseArray(loadConfiguration);
            this.filterDeleteAndEnabled(jsonArray);
            for (int i = 0; i < jsonArray.size(); i++) {
                if (jsonArray.get(i) instanceof List) {
                    List o = (List) jsonArray.get(i);
                    for (int j = 0; j < o.size(); j++) {
                        Map b = (Map) o.get(j);
                        if (StringUtils.equals((String) b.get("key"), key)) {
                            s += (int) b.get("value");
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error("get load configuration integer value error.");
            LogUtil.error(e.getMessage(), e);
        }
        return s;
    }

    private void checkPerformance(String projectId) {
        baseQuotaService.checkQuota(extQuotaMapper::countLoadTest, QuotaCheckType.LOAD, projectId,
                Translator.get("quota_performance_excess_project"),
                Translator.get("quota_performance_excess_workspace"));
    }

    private void checkMaxThread(String projectId, int threadNum) {
        // 增量为0的检查
        baseQuotaService.checkQuota(p -> (long) (threadNum - 1), QuotaCheckType.MAX_THREAD, projectId,
                Translator.get("quota_max_threads_excess_project"),
                Translator.get("quota_max_threads_excess_workspace"));
    }

    private void checkDuration(String projectId, long duration) {
        // 增量为0的检查
        baseQuotaService.checkQuota(p -> duration - 1, QuotaCheckType.DURATION, projectId,
                Translator.get("quota_duration_excess_project"),
                Translator.get("quota_duration_excess_workspace"));
    }

    private BigDecimal calcVum(String loadConfig) {
        BigDecimal vum = BigDecimal.ZERO;
        try {
            List jsonArray = JSON.parseArray(loadConfig);
            this.filterDeleteAndEnabled(jsonArray);
            for (Object value : jsonArray) {
                if (value instanceof List) {
                    List o = (List) value;
                    int thread = 0;
                    long duration = 0;
                    for (Object item : o) {
                        Map b = (Map) item;
                        if (StringUtils.equals((String) b.get("key"), "TargetLevel")) {
                            thread += (int) b.get("value");
                            break;
                        }
                    }
                    for (int j = 0; j < o.size(); j++) {
                        Map b = (Map) o.get(j);
                        if (StringUtils.equals((String) b.get("key"), "duration")) {
                            duration += (int) b.get("value");
                            break;
                        }
                    }
                    // 每个ThreadGroup消耗的vum单独计算
                    vum = vum.add(this.calcVum(thread, duration));
                }
            }
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
        }
        return vum;
    }

    private void filterDeleteAndEnabled(List jsonArray) {
        Iterator<Object> iterator = jsonArray.iterator();
        outer:
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof List<?>) {
                List<?> o = (List<?>) next;
                for (Object o1 : o) {
                    Map jsonObject = (Map) o1;
                    if (StringUtils.equals((String) jsonObject.get("key"), "deleted")) {
                        String value = (String) jsonObject.get("value");
                        if (StringUtils.equals(value, "true")) {
                            iterator.remove();
                            continue outer;
                        }
                    }
                }
                for (Object o1 : o) {
                    Map jsonObject = (Map) o1;
                    if (StringUtils.equals((String) jsonObject.get("key"), "enabled")) {
                        String value = (String) jsonObject.get("value");
                        if (StringUtils.equals(value, "false")) {
                            iterator.remove();
                            continue outer;
                        }
                    }
                }
            }
        }
    }

    private BigDecimal calcVum(int thread, long duration) {
        double used = thread * duration * 1.00 / 60;
        DecimalFormat df = new DecimalFormat("#.00000");
        return new BigDecimal(df.format(used));
    }

    private boolean doCheckVumUsed(Quota quota, BigDecimal toVumUsed, String errorMsg) {
        if (quota == null) {
            return true;
        }
        // 如果vumTotal为NULL是不限制
        if (baseQuotaService.isValid(quota, quota.getVumTotal())) {
            BigDecimal vumTotal = quota.getVumTotal();
            BigDecimal used = quota.getVumUsed();
            if (used == null) {
                used = BigDecimal.ZERO;
            }
            if (used.add(toVumUsed).compareTo(vumTotal) > 0) {
                MSException.throwException(errorMsg);
            }
            return false;
        }
        return true;
    }

    private Quota newPjQuota(String projectId, BigDecimal vumUsed) {
        Quota quota = new Quota();
        quota.setId(UUID.randomUUID().toString());
        quota.setUpdateTime(System.currentTimeMillis());
        quota.setUseDefault(false);
        quota.setVumUsed(vumUsed);
        quota.setProjectId(projectId);
        quota.setWorkspaceId(null);
        return quota;
    }

    private void doUpdateVumUsed(Quota dbQuota, Quota newQuota) {
        if (dbQuota == null || StringUtils.isBlank(dbQuota.getId())) {
            quotaMapper.insert(newQuota);
        } else {
            BigDecimal vumUsed = dbQuota.getVumUsed();
            if (vumUsed == null) {
                vumUsed = BigDecimal.ZERO;
            }
            if (newQuota == null || newQuota.getVumUsed() == null) {
                return;
            }
            BigDecimal toSetVum = vumUsed.add(newQuota.getVumUsed());
            if (toSetVum.compareTo(BigDecimal.ZERO) < 0) {
                LogUtil.info("update vum used warning. vum value: " + toSetVum);
                toSetVum = BigDecimal.ZERO;
            }
            LogUtil.info("update vum used add value: " + newQuota.getVumUsed());
            dbQuota.setVumUsed(toSetVum);
            quotaMapper.updateByPrimaryKeySelective(dbQuota);
        }
    }
}
