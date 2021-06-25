package xtages.console.service.aws

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.acm.AcmAsyncClient
import software.amazon.awssdk.services.acm.model.CertificateDetail
import software.amazon.awssdk.services.acm.model.DescribeCertificateRequest
import software.amazon.awssdk.services.acm.model.RequestCertificateRequest
import software.amazon.awssdk.services.acm.model.ValidationMethod
import xtages.console.query.tables.pojos.Project

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
        return getCertificateDetail(certificateArn)
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
