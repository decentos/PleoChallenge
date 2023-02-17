package io.pleo.antaeus.scheduler

import io.pleo.antaeus.core.services.BillingService
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

class SchedulerService(
    private val billingService: BillingService,
    pendingJobPeriod: String,
    retryJobPeriod: String,
) {
    private val pendingTrigger = TriggerBuilder
        .newTrigger()
        .withIdentity("pending", "antaeus")
        .withSchedule(
            CronScheduleBuilder.cronSchedule(pendingJobPeriod)
        )
        .build()

    private val retryTrigger = TriggerBuilder
        .newTrigger()
        .withIdentity("retry", "antaeus")
        .withSchedule(
            CronScheduleBuilder.cronSchedule(retryJobPeriod)
        )
        .build()

    private val pendingJob = JobBuilder.newJob(PendingInvoiceJob::class.java)
        .withIdentity("pending-job", "antaeus").build()

    private val retryJob = JobBuilder.newJob(RetryInvoiceJob::class.java)
        .withIdentity("retry-job", "antaeus").build()

    private val scheduler = StdSchedulerFactory().scheduler

    fun startScheduler() {
        scheduler.context["billingService"] = billingService
        scheduler.start()
        scheduler.scheduleJob(pendingJob, pendingTrigger)
        scheduler.scheduleJob(retryJob, retryTrigger)
    }

    fun stop() {
        scheduler.shutdown()
    }
}