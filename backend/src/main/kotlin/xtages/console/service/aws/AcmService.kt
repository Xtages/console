package xtages.console.service.aws

import mu.KotlinLogging
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.listener.RetryListenerSupport
import org.springframework.retry.support.RetryTemplateBuilder
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.acm.AcmAsyncClient
import software.amazon.awssdk.services.acm.model.CertificateDetail
import software.amazon.awssdk.services.acm.model.DescribeCertificateRequest
import software.amazon.awssdk.services.acm.model.RequestCertificateRequest
import software.amazon.awssdk.services.acm.model.ValidationMethod
import xtages.console.query.tables.pojos.Project

private val logger = KotlinLogging.logger { }

@Service
class AcmService(private val acmClient: AcmAsyncClient) {

    /**
     * Requests a certificate be created for [domainName].
     */
    fun requestCertificate(project: Project, domainName: String): CertificateDetail {
        val response = acmClient.requestCertificate(
            RequestCertificateRequest
                .builder()
                .domainName(domainName)
                .idempotencyToken(project.hash)
                .validationMethod(ValidationMethod.DNS)
                .build()
        ).get()
        val certificateArn = response.certificateArn()
        // If we call describeCertificate immediately after requesting it, ACM will sometimes return a partial
        // CertificateDetail, so we'll retry the call.
        val retry = RetryTemplateBuilder()
            .maxAttempts(10)
            .retryOn(InvalidCertificateDetail::class.java)
            .fixedBackoff(1000)
            .withListener(object : RetryListenerSupport() {
                override fun <T : Any?, E : Throwable?> close(
                    context: RetryContext?,
                    callback: RetryCallback<T, E>?,
                    throwable: Throwable?
                ) = logger.error {
                    "Could not get the full CertificateDetails for [$certificateArn], no more retrying."
                }

                override fun <T : Any?, E : Throwable?> onError(
                    context: RetryContext?,
                    callback: RetryCallback<T, E>?,
                    throwable: Throwable?
                ) = logger.error {
                    "ACM returned  partially-filled CertificateDetails for [$certificateArn], retrying."
                }
            })
            .build()
        return retry.execute<CertificateDetail, InvalidCertificateDetail> { context ->
            logger.debug { "Getting CertificateDetails for [$certificateArn]. Try [${context.retryCount}]." }
            val certificateDetail = getCertificateDetail(certificateArn)
            if (certificateDetail.domainName() == null
                || !certificateDetail.hasDomainValidationOptions()
                || certificateDetail.domainValidationOptions().first().resourceRecord() == null
            ) {
                throw InvalidCertificateDetail()
            }
            certificateDetail
        }
    }

    /**
     * Gets the [CertificateDetail] for the certificate associated to [certificateArn].
     */
    fun getCertificateDetail(certificateArn: String): CertificateDetail {
        val response = acmClient.describeCertificate(
            DescribeCertificateRequest
                .builder()
                .certificateArn(certificateArn)
                .build()
        ).get()
        return response.certificate()
    }
}

private class InvalidCertificateDetail : Exception()
