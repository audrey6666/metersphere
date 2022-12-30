package io.metersphere.service.quota;

import io.metersphere.base.mapper.ext.ExtQuotaMapper;
import io.metersphere.i18n.Translator;
import io.metersphere.quota.constants.QuotaCheckType;
import io.metersphere.quota.service.BaseQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional(rollbackFor = Exception.class)
public class QuotaService {

    @Resource
    private BaseQuotaService baseQuotaService;
    @Resource
    private ExtQuotaMapper extQuotaMapper;

    public void checkAPIDefinitionQuota(String projectId) {
        baseQuotaService.checkQuota(extQuotaMapper::countAPIDefinition, QuotaCheckType.API, projectId,
                Translator.get("quota_api_excess_project"),
                Translator.get("quota_api_excess_workspace"));
    }

    public void checkAPIAutomationQuota(String projectId) {
        baseQuotaService.checkQuota(extQuotaMapper::countAPIAutomation, QuotaCheckType.API, projectId,
                Translator.get("quota_api_excess_project"),
                Translator.get("quota_api_excess_workspace"));
    }
}
