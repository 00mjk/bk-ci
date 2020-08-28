package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.process.service.PipelineSettingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class PipelineInfoService @Autowired constructor(
    val pipelineService: PipelineService,
    val pipelineIdGenerator: PipelineIdGenerator,
    val pipelineRepositoryService: PipelineRepositoryService,
    val pipelineSettingService: PipelineSettingService,
    val pipelinePermissionService: PipelinePermissionService
) {

    fun exportPipeline(userId: String, projectId: String, pipelineId: String): PipelineModelAndSetting? {
        pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT,
                message = "用户($userId)无权限在工程($projectId)下导出流水线"
        )
        val settingInfo = getSettingInfo(projectId, pipelineId, userId)
        val model = pipelineRepositoryService.getModel(pipelineId)
        if (settingInfo == null || model == null) {
            return null
        }
        return PipelineModelAndSetting(
                model = model,
                setting = settingInfo!!
        )
    }

    fun uploadPipeline(userId: String, projectId: String, pipelineId: String?, pipelineModelAndSetting: PipelineModelAndSetting): String? {
        val permissionCheck = pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                permission = AuthPermission.CREATE
        )
        if (!permissionCheck) {
            logger.warn("$userId|$projectId|$pipelineId uploadPipeline permission check fail")
            throw RuntimeException()
        }
        val newPipelineId = pipelineId ?: pipelineIdGenerator.getNextId()
        pipelineService.saveAll(
                userId = userId,
                projectId = projectId,
                pipelineId = newPipelineId,
                model = pipelineModelAndSetting.model,
                setting = pipelineModelAndSetting.setting,
                checkPermission = true,
                checkTemplate = false,
                channelCode = ChannelCode.BS
        )
        return newPipelineId
    }

    private fun getSettingInfo(projectId: String, pipelineId: String, userId: String): PipelineSetting? {
        val settingInfo = pipelineSettingService.getSetting(pipelineId) ?: return null

        val hasPermission = pipelinePermissionService.isProjectUser(userId = userId, projectId = projectId, group = BkAuthGroup.MANAGER)

        val successType = settingInfo.successType.split(",").filter { i -> i.isNotBlank() }
                .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
        val failType = settingInfo.failType.split(",").filter { i -> i.isNotBlank() }
                .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()

        return PipelineSetting(
                projectId = settingInfo.projectId,
                pipelineId = settingInfo.pipelineId,
                pipelineName = settingInfo.name,
                desc = settingInfo.desc,
                runLockType = PipelineRunLockType.valueOf(settingInfo.runLockType),
                successSubscription = Subscription(
                        types = successType,
                        groups = settingInfo.successGroup.split(",").toSet(),
                        users = settingInfo.successReceiver,
                        wechatGroupFlag = settingInfo.successWechatGroupFlag,
                        wechatGroup = settingInfo.successWechatGroup,
                        wechatGroupMarkdownFlag = settingInfo.successWechatGroupMarkdownFlag,
                        detailFlag = settingInfo.successDetailFlag,
                        content = settingInfo.successContent ?: ""
                ),
                failSubscription = Subscription(
                        types = failType,
                        groups = settingInfo.failGroup.split(",").toSet(),
                        users = settingInfo.failReceiver,
                        wechatGroupFlag = settingInfo.failWechatGroupFlag,
                        wechatGroup = settingInfo.failWechatGroup,
                        wechatGroupMarkdownFlag = settingInfo.failWechatGroupMarkdownFlag,
                        detailFlag = settingInfo.failDetailFlag,
                        content = settingInfo.failContent ?: ""
                ),
                labels = emptyList(),
                waitQueueTimeMinute = DateTimeUtil.secondToMinute(settingInfo.waitQueueTimeSecond),
                maxQueueSize = settingInfo.maxQueueSize,
                hasPermission = hasPermission
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}