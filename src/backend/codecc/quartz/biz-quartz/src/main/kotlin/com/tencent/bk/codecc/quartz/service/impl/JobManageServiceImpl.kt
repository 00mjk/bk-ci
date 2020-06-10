package com.tencent.bk.codecc.quartz.service.impl

import com.tencent.bk.codecc.quartz.dao.JobCompensateRepository
import com.tencent.bk.codecc.quartz.dao.JobInstanceRepository
import com.tencent.bk.codecc.quartz.model.JobCompensateEntity
import com.tencent.bk.codecc.quartz.model.JobInstanceEntity
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto
import com.tencent.bk.codecc.quartz.pojo.JobInfoVO
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.quartz.service.JobManageService
import com.tencent.devops.common.api.util.UUIDUtil
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class JobManageServiceImpl @Autowired constructor(
    private val jobInstanceRepository: JobInstanceRepository,
    private val jobCompensateRepository: JobCompensateRepository
) : JobManageService {

    companion object {
        private val logger = LoggerFactory.getLogger(JobManageServiceImpl::class.java)
    }

    //确保操作是线程安全的
    private val jobNameList = mutableListOf<JobInstanceEntity>()

    override fun findAllJobs(): List<JobInstanceEntity> {
        val jobInstanceList = jobInstanceRepository.findAll()
        jobNameList.clear()
        jobNameList.addAll(jobInstanceList)
        return jobInstanceList
    }

    override fun findCachedJobs(): List<JobInstanceEntity> {
        return jobNameList
    }

    override fun saveJob(jobExternalDto: JobExternalDto): JobInstanceEntity {
        val jobInstance = JobInstanceEntity()
        val cronMd5 = DigestUtils.md5Hex(jobExternalDto.cronExpression)
        val key = "${UUIDUtil.generate()}_$cronMd5"
        jobInstance.jobName = if(jobExternalDto.jobName.isNullOrBlank()) key else jobExternalDto.jobName
        jobInstance.triggerName = if(jobExternalDto.jobName.isNullOrBlank()) key else jobExternalDto.jobName
        jobInstance.classUrl = jobExternalDto.classUrl
        jobInstance.className = jobExternalDto.className
        jobInstance.cronExpression = jobExternalDto.cronExpression
        jobInstance.jobParam = jobExternalDto.jobCustomParam
        jobInstance.createdBy = "sysadmin"
        jobInstance.createdDate = System.currentTimeMillis()
        jobInstance.updatedBy = "sysadmin"
        jobInstance.updatedDate = System.currentTimeMillis()
        return jobInstanceRepository.save(jobInstance)
    }

    override fun findJobsByName(jobNames: List<String>): List<JobInstanceEntity> {
        return jobInstanceRepository.findByJobNameIn(jobNames)
    }

    override fun findJobByName(jobName: String): JobInstanceEntity {
        return jobInstanceRepository.findByJobName(jobName)
    }

    override fun saveJobs(jobInstances: List<JobInstanceEntity>): List<JobInstanceEntity> {
        jobInstances.forEach {
            it.updatedBy = "sysadmin"
            it.updatedDate = System.currentTimeMillis()
        }
        return jobInstanceRepository.save(jobInstances)
    }

    override fun saveJobCompensate(jobCompensateEntity: JobCompensateEntity): JobCompensateEntity {
        jobCompensateEntity.createdBy = "sysadmin"
        jobCompensateEntity.createdDate = System.currentTimeMillis()
        jobCompensateEntity.updatedBy = "sysadmin"
        jobCompensateEntity.updatedDate = System.currentTimeMillis()
        return jobCompensateRepository.save(jobCompensateEntity)
    }

    override fun saveJob(jobInstance: JobInstanceEntity): JobInstanceEntity {
        jobInstance.updatedBy = "sysadmin"
        jobInstance.updatedDate = System.currentTimeMillis()
        return jobInstanceRepository.save(jobInstance)
    }

    override fun deleteJob(jobName: String?) {
        jobInstanceRepository.deleteByJobName(jobName)
    }

    override fun addOrRemoveJobToCache(jobInstance: JobInstanceEntity, operType: OperationType) {
        when (operType) {
            OperationType.ADD -> {
                jobNameList.add(jobInstance)
            }
            OperationType.REMOVE -> {
                jobNameList.removeIf { it.jobName == jobInstance.jobName }
            }
        }
    }


    override fun deleteAllJobs(){
        jobInstanceRepository.deleteAll()
    }

    override fun deleteAllCacheJobs(){
        jobNameList.clear()
    }

    override fun convert(jobInstanceEntity: JobInstanceEntity): JobInfoVO {
        return with(jobInstanceEntity) {
            JobInfoVO(
                classUrl = classUrl,
                className = className,
                jobName = jobName,
                triggerName = triggerName,
                cronExpression = cronExpression,
                jobParam = jobParam,
                shardTag = shardTag
            )
        }
    }
}