import React, {ReactNode, useState} from 'react';
import {Alert, Button, Col} from 'react-bootstrap';
import * as z from 'zod';
import {FormikHelpers} from 'formik/dist/types';
import {Form, Formik, FormikErrors} from 'formik';
import {Link as LinkIcon} from 'react-feather';
import {useQueryClient} from 'react-query';
import {projectApi} from 'service/Services';
import {AssociatedDomainCertificateStatusEnum, Project, ProjectSettings} from 'gen/api';
import {DocsLink} from 'components/link/XtagesLink';
import {SimpleProjectCard} from './ProjectDetailsCard';
import LabeledFormField from '../form/LabeledFormField';
import styles from './ProjectSettingsCard.module.scss';
import {CopiableSpan} from '../text/CopiableSpan';

export interface ProjectSettingsCardProps {
  project: Project;

  projectSettings: ProjectSettings;
}

const projectSettingsFormValuesSchema = z.object({
  associatedDomainName: z.string()
    .optional(),
});

type ProjectSettingsFormValues = z.infer<typeof projectSettingsFormValuesSchema>;

/**
 * A card showing {@link Project}'s {@link ProjectSettings} with a form to update them.
 */
export function ProjectSettingsCard({
  project,
  projectSettings,
}: ProjectSettingsCardProps) {
  const initialValues: ProjectSettingsFormValues = {
    associatedDomainName: undefined,
  };
  const [errorOccurred, setErrorOccurred] = useState(false);
  const queryClient = useQueryClient();

  async function saveSettings(
    values: ProjectSettingsFormValues,
    actions: FormikHelpers<ProjectSettingsFormValues>,
  ) {
    setErrorOccurred(false);
    try {
      if (values.associatedDomainName) {
        await projectApi.updateProjectSettings(project.name, {
          associatedDomainName: values.associatedDomainName,
        });
        await queryClient.invalidateQueries('projectSettings');
        actions.resetForm();
      }
    } catch (e) {
      setErrorOccurred(true);
    } finally {
      actions.setSubmitting(false);
    }
  }

  function validate(values: ProjectSettingsFormValues):
  FormikErrors<ProjectSettingsFormValues> | void {
    try {
      projectSettingsFormValuesSchema.parse(values);
      return {};
    } catch (error) {
      return error.formErrors.fieldErrors;
    }
  }

  const {associatedDomain} = projectSettings;
  const hasAssociatedDomain = associatedDomain !== undefined && associatedDomain !== null;
  let certStatusMessage: ReactNode;
  switch (associatedDomain?.certificateStatus) {
    case AssociatedDomainCertificateStatusEnum.Expired:
    case AssociatedDomainCertificateStatusEnum.ValidationTimedOut:
    case AssociatedDomainCertificateStatusEnum.Failed:
      certStatusMessage = (
        <span className="text-dark-danger font-weight-bold">
          we were unable to validate the domain ownership.
        </span>
      );
      break;
    case AssociatedDomainCertificateStatusEnum.Inactive:
      certStatusMessage = (
        <span className="text-dark-danger font-weight-bold">
          inactive
        </span>
      );
      break;
    case AssociatedDomainCertificateStatusEnum.Issued:
      certStatusMessage = (
        <span className="text-dark-success font-weight-bold">
          active
        </span>
      );
      break;
    case AssociatedDomainCertificateStatusEnum.PendingValidation:
      certStatusMessage = (
        <span className="text-dark-warning font-weight-bold">
          we are validating the domain ownership.
        </span>
      );
      break;
    default:
      certStatusMessage = undefined;
      break;
  }

  return (
    <SimpleProjectCard project={project} showSettingsLink={false} showRunCiButton={false}>
      <Col sm={9}>
        <Formik initialValues={initialValues} validate={validate} onSubmit={saveSettings}>
          {({
            isSubmitting,
            touched,
            errors,
          }) => (
            <Form noValidate>
              {errorOccurred && (
              <Alert variant="danger">
                <div className="d-flex justify-content-center">
                  <strong>An unexpected error occurred</strong>
                </div>
              </Alert>
              )}
              <h3 className="h5">Domain Settings</h3>
              <p className="prose">
                Use this setting to associate your own domain name to your Project&apos;s
                production environment. We will handle the creation and renewal of the SSL
                certificate for your domain. Learn more
                {' '}
                <DocsLink articlePath="/projects/custom-domains/" title="Custom Domains Documentation" icon={false}>here</DocsLink>
                .
              </p>
              <LabeledFormField
                type="text"
                name="associatedDomainName"
                label="Domain Name"
                placeholder="www.yourdomain.com"
                disabled={hasAssociatedDomain}
                value={associatedDomain?.name}
                invalid={touched.associatedDomainName && errors.associatedDomainName != null}
                validationFeedback="Please provide a valid domain."
                addOn={<LinkIcon size="1em" />}
              />
              {associatedDomain && (
              <p className="text-sm">
                (if you need to update this setting please
                {' '}
                <a href="mailto:support@xtages.com" target="_blank" rel="noreferrer">
                  contact
                  us
                </a>
                )
              </p>
              )}
              {certStatusMessage && (
              <p>
                Status:
                {' '}
                {certStatusMessage}
              </p>
              )}
              {associatedDomain?.validationRecord && (
              <>
                <p className="prose">
                  For Xtages to verify domain ownership, add the following record to the DNS
                  configuration for
                  {' '}
                  <mark>{associatedDomain?.name}</mark>
                  . DNS providers and domain registrars have different methods to add CNAME
                  records.
                  {' '}
                  <a href="invalid.com">Learn more</a>
                  .
                </p>
                {associatedDomain?.certificateStatus
                === AssociatedDomainCertificateStatusEnum.PendingValidation && (
                <table className={`table table-sm ${styles.dnsRecordTable}`}>
                  <thead className="thead-light">
                    <tr>
                      <th>
                        Name
                      </th>
                      <th className={styles.recordType}>
                        Type
                      </th>
                      <th>
                        Value
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>
                        <CopiableSpan>{associatedDomain.validationRecord.name}</CopiableSpan>
                      </td>
                      <td>
                        <CopiableSpan>{associatedDomain.validationRecord.recordType}</CopiableSpan>
                      </td>
                      <td>
                        <CopiableSpan>{associatedDomain.validationRecord.value}</CopiableSpan>
                      </td>
                    </tr>
                  </tbody>
                </table>
                )}
              </>
              )}
              <div className="mt-4">
                <Button type="submit" disabled={isSubmitting || hasAssociatedDomain}>
                  Save
                </Button>
              </div>
            </Form>
          )}
        </Formik>
      </Col>
    </SimpleProjectCard>
  );
}
