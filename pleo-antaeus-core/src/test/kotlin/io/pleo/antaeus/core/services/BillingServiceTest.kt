package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {
    private val pendingInvoice = Invoice(1, 1, Money(BigDecimal.valueOf(10), Currency.EUR), InvoiceStatus.PENDING)
    private val retryInvoice = Invoice(2, 2, Money(BigDecimal.valueOf(20), Currency.USD), InvoiceStatus.RETRY)

    @Test
    suspend fun `successfully pending payment`() {
        val billingService = BillingService(
            paymentProvider = getPaymentProvider(true),
            invoiceService = getInvoiceService(),
            notificationService = getNotificationService()
        )
        billingService.sendInvoicesAsync(InvoiceStatus.PENDING)
        assert(pendingInvoice.status == InvoiceStatus.PAID)
    }

    @Test
    suspend fun `unsuccessfully pending payment`() {
        val billingService = BillingService(
            paymentProvider = getPaymentProvider(false),
            invoiceService = getInvoiceService(),
            notificationService = getNotificationService()
        )
        billingService.sendInvoicesAsync(InvoiceStatus.PENDING)
        assert(pendingInvoice.status == InvoiceStatus.RETRY)
    }

    @Test
    suspend fun `successfully retry payment`() {
        val billingService = BillingService(
            paymentProvider = getPaymentProvider(true),
            invoiceService = getInvoiceService(),
            notificationService = getNotificationService()
        )
        billingService.sendInvoicesAsync(InvoiceStatus.RETRY)
        assert(pendingInvoice.status == InvoiceStatus.PAID)
    }

    @Test
    suspend fun `unsuccessfully retry payment`() {
        val billingService = BillingService(
            paymentProvider = getPaymentProvider(false),
            invoiceService = getInvoiceService(),
            notificationService = getNotificationService()
        )
        billingService.sendInvoicesAsync(InvoiceStatus.RETRY)
        assert(pendingInvoice.status == InvoiceStatus.RETRY)
    }

    @Test
    suspend fun `handle customer not found exception`() {
        val billingService = BillingService(
            paymentProvider = getPaymentProviderCustomerNotFound(),
            invoiceService = getInvoiceService(),
            notificationService = getNotificationService()
        )
        billingService.sendInvoicesAsync(InvoiceStatus.PENDING)
        assert(pendingInvoice.status == InvoiceStatus.REJECTED)
    }

    @Test
    suspend fun `handle currency mismatch exception`() {
        val billingService = BillingService(
            paymentProvider = getPaymentProviderCurrencyMismatch(),
            invoiceService = getInvoiceService(),
            notificationService = getNotificationService()
        )
        billingService.sendInvoicesAsync(InvoiceStatus.PENDING)
        assert(pendingInvoice.status == InvoiceStatus.REJECTED)
    }

    @Test
    suspend fun `handle network exception`() {
        val billingService = BillingService(
            paymentProvider = getPaymentProviderNetworkException(),
            invoiceService = getInvoiceService(),
            notificationService = getNotificationService()
        )
        billingService.sendInvoicesAsync(InvoiceStatus.PENDING)
        assert(pendingInvoice.status == InvoiceStatus.RETRY)
    }

    private fun getPaymentProvider(success: Boolean): PaymentProvider {
        return mockk {
            every { charge(any()) } returns success
        }
    }

    private fun getPaymentProviderCustomerNotFound(): PaymentProvider {
        return mockk {
            every { charge(any()) } throws CustomerNotFoundException(-1)
        }
    }

    private fun getPaymentProviderCurrencyMismatch(): PaymentProvider {
        return mockk {
            every { charge(any()) } throws CurrencyMismatchException(-1, -1)
        }
    }

    private fun getPaymentProviderNetworkException(): PaymentProvider {
        return mockk {
            every { charge(any()) } throws NetworkException()
        }
    }

    private fun getInvoiceService(): InvoiceService {
        return mockk {
            every { fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(pendingInvoice)
            every { fetchInvoicesByStatus(InvoiceStatus.RETRY) } returns listOf(retryInvoice)
            every { updateInvoiceStatus(any(), any()) }
        }
    }

    private fun getNotificationService(): NotificationService {
        return mockk {
            every { sendNotification(any()) }
        }
    }
}
