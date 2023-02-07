package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}

    suspend fun sendInvoicesAsync(status: InvoiceStatus) = coroutineScope {
        val invoices = try {
            invoiceService.fetchInvoicesByStatus(status)
        } catch (e: Exception) {
            emptyList<Invoice>()
        }
        if (invoices.isNotEmpty()) {
            logger.info { "Successfully retrieving ${invoices.size} invoice(s) with ${status.name} status" }
            launch { invoices.forEach { sendInvoice(it) } }
        }
    }

    private fun sendInvoice(invoice: Invoice) {
        logger.info { "Process of sending the invoice for ${invoice.id} has started" }

        try {
            val isCharged = paymentProvider.charge(invoice)
            if (isCharged) {
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
                    .also { logger.info { "Account was successfully charged the given amount" } }
            } else {
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.RETRY)
                    .also { logger.info { "Account balance did not allow the charge" } }
            }
        } catch (e: Exception) {
            when (e) {
                is CustomerNotFoundException -> {
                    invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.REJECTED)
                        .also { logger.error { "No customer has the given id" } }
                }
                is CurrencyMismatchException -> {
                    invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.REJECTED)
                        .also { logger.error { "Currency does not match the customer account" } }
                }
                is NetworkException -> {
                    invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.RETRY)
                        .also { logger.warn { "Network error happens for the invoice ${invoice.id}. Sending will be retry later" } }
                }
            }
        }

    }
}
