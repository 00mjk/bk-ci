package com.tencent.devops.process.engine.atom.vm.parser

import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.engine.service.store.StoreImageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @Description
 * @Date 2019/11/17
 * @Version 1.0
 */
@Component(value = "commonDispatchTypeParser")
class DispatchTypeParserImpl @Autowired constructor(
    private val storeImageService: StoreImageService
) : DispatchTypeParser {

    private val logger = LoggerFactory.getLogger(DispatchTypeParserImpl::class.java)

    override fun parse(userId: String, projectId: String, dispatchType: DispatchType) {
        if (dispatchType is StoreDispatchType) {
            // 凭证项目默认初始值为当前项目
            dispatchType.credentialProject = projectId
            if (dispatchType.imageType == ImageType.BKSTORE) {
                // 从商店获取镜像真实信息
                val imageRepoInfo = storeImageService.getImageRepoInfo(
                    userId = userId,
                    projectId = projectId,
                    imageCode = dispatchType.imageCode,
                    imageVersion = dispatchType.imageVersion,
                    defaultPrefix = ""
                )
                logger.info("DispatchTypeParserImpl:imageType==BKSTORE:imageBaseInfo=(${imageRepoInfo.sourceType.name},${imageRepoInfo.completeImageName},${imageRepoInfo.ticketId},${imageRepoInfo.ticketProject})")
                // 镜像来源替换为原始来源
                dispatchType.imageType = imageRepoInfo.sourceType
                dispatchType.value = imageRepoInfo.completeImageName
                dispatchType.credentialId = imageRepoInfo.ticketId
                dispatchType.credentialProject = imageRepoInfo.ticketProject
            } else {
                dispatchType.credentialProject = projectId
            }
        }
    }
}