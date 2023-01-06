package io.metersphere.service;

import io.metersphere.base.domain.*;
import io.metersphere.base.mapper.ProjectMapper;
import io.metersphere.base.mapper.QuotaMapper;
import io.metersphere.base.mapper.WorkspaceMapper;
import io.metersphere.base.mapper.ext.ExtQuotaMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.i18n.Translator;
import io.metersphere.quota.constants.QuotaCheckType;
import io.metersphere.quota.dto.CountDto;
import io.metersphere.quota.constants.QuotaConstants;
import io.metersphere.quota.service.BaseQuotaManageService;
import io.metersphere.quota.service.BaseQuotaService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class QuotaService {

    @Resource
    private WorkspaceMapper workspaceMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private BaseQuotaManageService baseQuotaManageService;
    @Resource
    private BaseQuotaService baseQuotaService;
    @Resource
    private ExtQuotaMapper extQuotaMapper;
    @Resource
    private QuotaMapper quotaMapper;

    /**
     * 检查工作空间项目数量配额
     * @param workspaceId 工作空间ID
     */
    public void checkWorkspaceProject(String workspaceId) {
        baseQuotaService.doCheckQuota(baseQuotaManageService.getWorkspaceQuota(workspaceId),
                QuotaCheckType.PROJECT, Translator.get("quota_project_excess_project"),
                extQuotaMapper.countWorkspaceProject(workspaceId) + 1);
    }

    /**
     * 检查向某资源添加人员时是否超额
     * @param addMemberMap 资源ID:添加用户ID列表
     * @param type 检查类型 PROJECT/WORKSPACE
     */
    public void checkMemberCount(Map<String, List<String>> addMemberMap, String type) {
        if (addMemberMap == null || addMemberMap.keySet().size() == 0) {
            return;
        }
        if (!StringUtils.equals(type, QuotaCheckType.CHECK_PROJECT) && !StringUtils.equals(type, QuotaCheckType.CHECK_WORKSPACE)) {
            return;
        }
        List<String> sourceIds = new ArrayList<>(addMemberMap.keySet());
        List<Quota> quotas = extQuotaMapper.listQuotaBySourceIds(sourceIds);

        Quota defaultQuota = null;
        if (StringUtils.equals(QuotaCheckType.CHECK_WORKSPACE, type)) {
            defaultQuota = baseQuotaManageService.getDefaultQuota(QuotaConstants.DefaultType.workspace);
        }

        Map<String, Integer> quotaMap = new HashMap<>();
        for (Quota quota : quotas) {
            String key;
            if (StringUtils.equals(QuotaCheckType.CHECK_PROJECT, type)) {
                key = quota.getProjectId();
            } else {
                key = quota.getWorkspaceId();
            }
            if (StringUtils.isBlank(key)) {
                continue;
            }

            if (BooleanUtils.isTrue(quota.getUseDefault())) {
                if (StringUtils.equals(QuotaCheckType.CHECK_PROJECT, type)) {
                    Project project = projectMapper.selectByPrimaryKey(key);
                    if (project == null || StringUtils.isBlank(project.getWorkspaceId())) {
                        continue;
                    }
                    defaultQuota = baseQuotaManageService.getProjectDefaultQuota(project.getWorkspaceId());
                }
                if (defaultQuota == null) {
                    continue;
                }
                quota = defaultQuota;
            }

            if (quota.getMember() == null || quota.getMember() == 0) {
                continue;
            }
            quotaMap.put(key, quota.getMember());
        }

        Set<String> set = quotaMap.keySet();
        if (set.isEmpty()) {
            return;
        }

        Map<String, Integer> memberCountMap = this.getDBMemberCountMap(addMemberMap, type);
        Map<String, String> sourceNameMap = this.getNameMap(sourceIds, type);

        this.doCheckMemberCount(quotaMap, memberCountMap, sourceNameMap, addMemberMap, type);
    }

    private void doCheckMemberCount(Map<String, Integer> quotaMap, Map<String, Integer> memberCountMap,
                                    Map<String, String> sourceNameMap, Map<String, List<String>> addMemberMap, String checkType) {
        Set<String> set = quotaMap.keySet();
        StringBuilder builder = new StringBuilder();
        for (String sourceId : set) {
            // 没有配额限制的跳过
            if (!addMemberMap.containsKey(sourceId)) {
                continue;
            }
            // 当前已存在人员数量
            Integer dbCount = memberCountMap.getOrDefault(sourceId, 0);
            int toAddCount = addMemberMap.get(sourceId) == null ? 0 : addMemberMap.get(sourceId).size();
            // 添加人员之后判断配额
            if (dbCount + toAddCount > quotaMap.get(sourceId)) {
                builder.append(sourceNameMap.get(sourceId));
                builder.append(" ");
            }
        }
        if (builder.length() > 0) {
            builder.append("超出成员数量配额");
            if (StringUtils.equals(QuotaCheckType.CHECK_WORKSPACE, checkType)) {
                builder.append("(工作空间成员配额将其下所有项目成员也计算在内)");
            }
            MSException.throwException(builder.toString());
        }
    }

    private Map<String, Integer> getDBMemberCountMap(Map<String, List<String>> addMemberMap, String type) {
        List<String> sourceIds = new ArrayList<>(addMemberMap.keySet());
        Map<String, Integer> memberCountMap = new HashMap<>();
        if (StringUtils.equals(QuotaCheckType.CHECK_WORKSPACE, type)) {
            // 检查工作空间配额时将其下所有项目成员也计算在其中
            ProjectExample projectExample = new ProjectExample();
            for (String sourceId : sourceIds) {
                projectExample.clear();
                projectExample.createCriteria().andWorkspaceIdEqualTo(sourceId);
                List<Project> projects = projectMapper.selectByExample(projectExample);
                List<String> ids = projects.stream().map(Project::getId).collect(Collectors.toList());
                ids.add(sourceId);
                List<String> memberIds = addMemberMap.getOrDefault(sourceId, new ArrayList<>());
                long count = extQuotaMapper.listUserByWorkspaceAndProjectIds(ids, memberIds);
                memberCountMap.put(sourceId, (int) count);
            }
        } else if (StringUtils.equals(QuotaCheckType.CHECK_PROJECT, type)) {
            List<CountDto> list = extQuotaMapper.listUserBySourceIds(sourceIds);
            memberCountMap = list.stream().collect(Collectors.toMap(CountDto::getSourceId, CountDto::getCount));
        }
        return memberCountMap;
    }


    /**
     * 如果有该项目配额，修改为使用工作空间下项目默认配额
     * 无该配额，新建配额并默认使用工作空间下项目默认配额
     * @return 操作后的配额
     */
    public Quota projectUseDefaultQuota(String projectId) {
        Quota pjQuota = extQuotaMapper.getProjectQuota(projectId);
        if (pjQuota != null) {
            pjQuota.setUseDefault(true);
            quotaMapper.updateByPrimaryKeySelective(pjQuota);
            return pjQuota;
        } else {
            Quota quota = new Quota();
            quota.setId(UUID.randomUUID().toString());
            quota.setUseDefault(true);
            quota.setProjectId(projectId);
            quota.setWorkspaceId(null);
            quota.setUpdateTime(System.currentTimeMillis());
            quotaMapper.insert(quota);
            return quota;
        }
    }

    /**
     * 如果有该工作空间配额，修改为使用系统默认配额，
     * 无该配额，新建配额并默认使用系统配额
     * @param workspaceId 工作空间ID
     * @return 操作后的配额
     */
    public Quota workspaceUseDefaultQuota(String workspaceId) {
        Quota wsQuota = extQuotaMapper.getWorkspaceQuota(workspaceId);
        if (wsQuota != null) {
            wsQuota.setUseDefault(true);
            quotaMapper.updateByPrimaryKeySelective(wsQuota);
            return wsQuota;
        } else {
            Quota quota = new Quota();
            quota.setId(UUID.randomUUID().toString());
            quota.setUseDefault(true);
            quota.setProjectId(null);
            quota.setWorkspaceId(workspaceId);
            quota.setUpdateTime(System.currentTimeMillis());
            quotaMapper.insert(quota);
            return quota;
        }
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

    private Map<String, String> getNameMap(List<String> sourceIds, String type) {
        Map<String, String> nameMap = new HashMap<>(16);
        if (CollectionUtils.isEmpty(sourceIds)) {
            return nameMap;
        }
        if (StringUtils.equals(QuotaCheckType.CHECK_PROJECT, type)) {
            ProjectExample projectExample = new ProjectExample();
            projectExample.createCriteria().andIdIn(sourceIds);
            List<Project> projects = projectMapper.selectByExample(projectExample);
            nameMap = projects.stream().collect(Collectors.toMap(Project::getId, Project::getName));
        } else if (StringUtils.equals(QuotaCheckType.CHECK_WORKSPACE, type)) {
            WorkspaceExample workspaceExample = new WorkspaceExample();
            workspaceExample.createCriteria().andIdIn(sourceIds);
            List<Workspace> workspaces = workspaceMapper.selectByExample(workspaceExample);
            nameMap = workspaces.stream().collect(Collectors.toMap(Workspace::getId, Workspace::getName));
        }
        return nameMap;
    }

}
