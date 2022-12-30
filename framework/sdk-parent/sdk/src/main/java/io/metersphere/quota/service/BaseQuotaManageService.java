package io.metersphere.quota.service;

import io.metersphere.base.domain.Project;
import io.metersphere.base.domain.Quota;
import io.metersphere.base.mapper.ProjectMapper;
import io.metersphere.base.mapper.QuotaMapper;
import io.metersphere.base.mapper.ext.ExtQuotaMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.quota.constants.QuotaConstants;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lyh
 */

@Service
@Transactional(rollbackFor = Exception.class)
public class BaseQuotaManageService {

    @Resource
    private QuotaMapper quotaMapper;
    @Resource
    private ExtQuotaMapper extQuotaMapper;
    @Resource
    private ProjectMapper projectMapper;


    public Quota getProjectQuota(String projectId) {
        Quota quota = extQuotaMapper.getProjectQuota(projectId);
        if (quota != null && BooleanUtils.isTrue(quota.getUseDefault())) {
            Project project = projectMapper.selectByPrimaryKey(projectId);
            if (project == null || StringUtils.isBlank(project.getWorkspaceId())) {
                MSException.throwException("project is null or workspace_id of project is null");
            }
            return getProjectDefaultQuota(project.getWorkspaceId());
        }
        return quota;
    }

    public Quota getProjectDefaultQuota(String workspaceId) {
        if (StringUtils.isBlank(workspaceId)) {
            return new Quota();
        }
        Quota quota = quotaMapper.selectByPrimaryKey(QuotaConstants.prefix + workspaceId);
        if (quota == null) {
            quota = new Quota();
            quota.setId(QuotaConstants.prefix + workspaceId);
        }
        return quota;
    }

    public Quota getWorkspaceQuota(String workspaceId) {
        Quota quota = extQuotaMapper.getWorkspaceQuota(workspaceId);
        if (quota != null && BooleanUtils.isTrue(quota.getUseDefault())) {
            return getDefaultQuota(QuotaConstants.DefaultType.workspace);
        }
        return quota;
    }

    public Quota getDefaultQuota(QuotaConstants.DefaultType type) {
        Quota quota = quotaMapper.selectByPrimaryKey(type.name());
        if (quota == null) {
            quota = new Quota();
            quota.setId(type.name());
        }
        return quota;
    }

}
