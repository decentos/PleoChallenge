package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val notificationService: NotificationService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun sendInvoicesAsync(status: InvoiceStatus) = coroutineScope {
        logger.info("Process of sending invoices with the status ${status.name} has started")
        val invoices = try {
            invoiceService.fetchInvoicesByStatus(status)
        } catch (e: Exception) {
            emptyList()
        }
        if (invoices.isNotEmpty()) {
            logger.info { "Successfully retrieving ${invoices.size} invoice(s) with the ${status.name} status" }
            invoices.forEach { launch { sendInvoice(it) } }
        } else {
            logger.info { "No invoices with the ${status.name} status" }
        }
    }

    private fun sendInvoice(invoice: Invoice) {
        logger.info { "Process of sending the invoice for ${invoice.id} has started" }

        try {
            val isCharged = paymentProvider.charge(invoice)
            if (isCharged) {
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
                    .also { logger.info { "Account ${invoice.customerId} was successfully charged the given amount for invoice ${invoice.id}" } }
                CoroutineScope(Dispatchers.Default).launch {
                    notificationService.sendNotification("Invoice was successfully charged.")
                }
            } else {
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.RETRY)
                    .also { logger.info { "Account balance ${invoice.customerId} did not allow the charge invoice ${invoice.id}" } }
                CoroutineScope(Dispatchers.Default).launch {
                    notificationService.sendNotification("Your balance is not enough to pay the invoice. Please top up your account. We'll try again tomorrow.")
                }
            }
        } catch (e: Exception) {
            when (e) {
                is CustomerNotFoundException -> {
                    invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.REJECTED)
                        .also { logger.error { "No customer has the given id: ${invoice.customerId}" } }
                }
                is CurrencyMismatchException -> {
                    invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.REJECTED)
                        .also { logger.error { "Currency ${invoice.amount.currency} does not match the customer account ${invoice.customerId}" } }
                }
                is NetworkException -> {
                    invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.RETRY)
                        .also { logger.warn { "Network error happens for the invoice ${invoice.id}" } }
                    CoroutineScope(Dispatchers.Default).launch {
                        notificationService.sendNotification("Failed attempt to make a payment. We'll try again later.")
                    }
                }
            }
        }
    }
}
