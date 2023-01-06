package io.metersphere.quota.service;

import io.metersphere.base.domain.Project;
import io.metersphere.base.domain.ProjectExample;
import io.metersphere.base.domain.Quota;
import io.metersphere.base.mapper.ProjectMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.commons.utils.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.metersphere.quota.constants.QuotaCheckType.*;

/**
 * @author lyh
 */

@Service
@Transactional(rollbackFor = Exception.class)
public class BaseQuotaService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private BaseQuotaManageService baseQuotaManageService;

    public boolean isValid(Quota quota, Object obj) {
        boolean sign = quota != null;
        if (obj instanceof Integer) {
            return sign && this.isValid((Integer) obj);
        } else if (obj instanceof String) {
            return sign && this.isValid((String) obj);
        } else if (obj instanceof BigDecimal) {
            return sign && this.isValid((BigDecimal) obj);
        }
        return false;
    }

    public boolean isValid(Integer value) {
        return value != null && value > 0;
    }

    public boolean isValid(String value) {
        return StringUtils.isNotBlank(value);
    }

    public boolean isValid(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    public String queryWorkspaceId(String projectId) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        if (project == null) {
            return null;
        }
        return project.getWorkspaceId();
    }

    /**
     * 检查资源池
     * @return 资源池名称
     */
    public Set<String> getQuotaResourcePools() {
        Set<String> pools = new HashSet<>();
        String projectId = SessionUtils.getCurrentProjectId();
        Quota pjQuota = baseQuotaManageService.getProjectQuota(projectId);
        if (pjQuota != null) {
            if (isValid(pjQuota, pjQuota.getResourcePool())) {
                pools.addAll(Arrays.asList(pjQuota.getResourcePool().split(",")));
                return pools;
            }
        }

        String workspaceId = this.queryWorkspaceId(projectId);
        if (StringUtils.isBlank(workspaceId)) {
            return pools;
        }
        Quota wsQuota = baseQuotaManageService.getWorkspaceQuota(workspaceId);
        if (wsQuota != null) {
            if (isValid(wsQuota, wsQuota.getResourcePool())) {
                pools.addAll(Arrays.asList(wsQuota.getResourcePool().split(",")));
            }
        }
        return pools;
    }

    /**
     * 工作空间下被限制使用的资源池
     * @param workspaceId 工作空间ID
     * @return 资源池名称Set
     */
    public Set<String> getQuotaWsResourcePools(String workspaceId) {
        Set<String> pools = new HashSet<>();
        Quota wsQuota = baseQuotaManageService.getWorkspaceQuota(workspaceId);
        if (wsQuota != null) {
            if (isValid(wsQuota, wsQuota.getResourcePool())) {
                pools.addAll(Arrays.asList(wsQuota.getResourcePool().split(",")));
            }
        }
        return pools;
    }


    /**
     * 增量为1的配额检查方法
     *
     * @param queryFunc        查询已存在数量的方法
     * @param checkType        检查配额类型
     * @param projectId        项目ID
     * @param projectMessage   项目超出配额警告信息
     * @param workspaceMessage 工作空间超出配额警告信息
     */
    public void checkQuota(Function<List<String>, Long> queryFunc, String checkType, String projectId, String projectMessage, String workspaceMessage) {
        if (queryFunc == null) {
            LogUtil.info("param warning. function is null");
            return;
        }

        if (StringUtils.isBlank(projectId)) {
            return;
        }

        // 检查项目配额
        Quota qt = baseQuotaManageService.getProjectQuota(projectId);
        boolean isContinue = true;
        long count;
        if (qt != null) {
            count = queryFunc.apply(Collections.singletonList(projectId));
            // 数量+1后检查
            isContinue = this.doCheckQuota(qt, checkType, projectMessage, count + 1);
        }

        // 检查是否有工作空间限额
        if (isContinue) {
            String workspaceId = this.queryWorkspaceId(projectId);
            if (StringUtils.isBlank(workspaceId)) {
                return;
            }
            Quota quota = baseQuotaManageService.getWorkspaceQuota(workspaceId);
            if (quota == null) {
                return;
            }
            count = queryFunc.apply(this.queryProjectIdsByWorkspaceId(workspaceId));
            this.doCheckQuota(quota, checkType, workspaceMessage, count + 1);
        }
    }

    public boolean doCheckQuota(Quota quota, String checkType, String errorMsg, long queryCount) {
        if (quota == null) {
            return true;
        }
        // todo
        Object quotaCount = null;
        if (StringUtils.equals(API, checkType)) {
            quotaCount = quota.getApi();
        } else if (StringUtils.equals(LOAD, checkType)) {
            quotaCount = quota.getPerformance();
        } else if (StringUtils.equals(DURATION, checkType)) {
            quotaCount = quota.getDuration();
        } else if (StringUtils.equals(MAX_THREAD, checkType)) {
            quotaCount = quota.getMaxThreads();
        } else if (StringUtils.equals(MEMBER, checkType)) {
            quotaCount = quota.getMember();
        } else if (StringUtils.equals(PROJECT, checkType)) {
            quotaCount = quota.getProject();
        } else {
            LogUtil.error("doCheckQuota get quota field fail, don't have type: " + checkType);
        }

        if (isValid(quota, quotaCount)) {
            long count = Long.parseLong(String.valueOf(quotaCount));
            if (queryCount > count) {
                MSException.throwException(errorMsg);
            }
            return false;
        }
        return true;
    }

    private List<String> queryProjectIdsByWorkspaceId(String workspaceId) {
        ProjectExample example = new ProjectExample();
        example.createCriteria().andWorkspaceIdEqualTo(workspaceId);
        List<Project> projects = projectMapper.selectByExample(example);
        return projects.stream().map(Project::getId).collect(Collectors.toList());
    }

}
