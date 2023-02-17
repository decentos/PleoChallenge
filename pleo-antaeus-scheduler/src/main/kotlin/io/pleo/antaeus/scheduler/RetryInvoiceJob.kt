package io.pleo.antaeus.scheduler

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext

class RetryInvoiceJob : Job {
    private val logger = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext) {
        logger.info("Job for sending invoices with the status RETRY has executed")
        val schedulerContext = context.scheduler.context
        val billingService= schedulerContext["billingService"] as BillingService
        CoroutineScope(Dispatchers.Default).launch {
            billingService.sendInvoicesAsync(InvoiceStatus.RETRY)
        }
    }
}