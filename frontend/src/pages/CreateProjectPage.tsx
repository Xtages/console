import {Codesandbox} from 'react-feather';
import React, {ReactNode, useState} from 'react';
import * as z from 'zod';
import {Field, Form, Formik, FormikErrors} from 'formik';
import Page from 'components/layout/Page';
import {Section, SectionTitle} from 'components/layout/Section';
import ProjectTemplateCard from 'components/project/ProjectTemplateCard';
import {FormikHelpers} from 'formik/dist/types';
import {useHistory, Link} from 'react-router-dom';
import {Alert, Button} from 'react-bootstrap';
import {DocsLink} from 'components/link/XtagesLink';
import {useAuth} from 'hooks/useAuth';
import {projectApi} from 'service/Services';
import {CreateProjectReqTypeEnum, UsageDetail} from 'gen/api';
import LabeledFormField from '../components/form/LabeledFormField';

const createProjectFormValuesSchema = z.object({
  projectName: z.string()
    .nonempty(),
  description: z.string()
    .optional(),
});

type CreateProjectFromValues = z.infer<typeof createProjectFormValuesSchema>;

/**
 * Renders the form to create a new [Project]. Currently it's hardcoded to create Node.js projects.
 */
export default function CreateProjectPage() {
  const initialValues: CreateProjectFromValues = {
    projectName: '',
  };
  const [errorMsg, setErrorMsg] = useState<ReactNode>(null);
  const auth = useAuth();
  const organization = auth.principal?.org ?? 'UNKNOWN';
  const history = useHistory();

  async function createProject(
    values: CreateProjectFromValues, actions: FormikHelpers<CreateProjectFromValues>,
  ) {
    setErrorMsg(null);
    try {
      await projectApi.createProject({
        name: values.projectName,
        type: CreateProjectReqTypeEnum.Node,
        version: '15.13.0',
        description: values.description,
      });
    } catch (e) {
      const {isAxiosError, response} = e;
      const {status, data} = response;
      if (isAxiosError) {
        if (status === 409) {
          setErrorMsg(`A GitHub repository named "${organization}/${values.projectName}" already exists.`);
        } else if (status === 400 && data.error_code === 'USAGE_OVER_LIMIT') {
          const usageDetail: UsageDetail = data.details;
          setErrorMsg(
            <>
              {organization}
              {' '}
              has reached the limit (
              {usageDetail.limit}
              ) for projects.
              {' '}
              See your usage
              {' '}
              <Link className="alert-link" to="/account">here</Link>
              .
            </>,
          );
        } else {
          setErrorMsg('An unexpected error occurred');
        }
      } else {
        setErrorMsg('An unexpected error occurred');
      }
      actions.setSubmitting(false);
      return;
    }
    actions.setSubmitting(false);
    history.push('/');
  }

  function validate(values: CreateProjectFromValues): FormikErrors<CreateProjectFromValues> {
    try {
      createProjectFormValuesSchema.parse(values);
      return {};
    } catch (error) {
      return error.formErrors.fieldErrors;
    }
  }

  return (
    <Page width="narrow">
      <Section>
        <SectionTitle icon={Codesandbox} title="Create new project" />
        <div className="col">
          <p className="prose">
            A new
            {' '}
            <strong>private</strong>
            {' '}
            GitHub repository by the same name as the project will be created,
            as well as CI and CD pipelines.
            <DocsLink articlePath="/projects" title="Creating a project" />
          </p>
        </div>
      </Section>
      <Section last>
        <SectionTitle title="Project" small />
        <div className="col-12">
          <div className="row">
            <div className="d-block col-4">
              <h3 className="h6">Template</h3>
              <ProjectTemplateCard
                id="nodejs"
                title="Simple Node.js server"
                description="A simple Node.js server template, using Express.js as well as Jest for running tests"
                imageName="nodejs.svg"
              />
            </div>
            <div className="col">
              <Formik
                initialValues={initialValues}
                validate={validate}
                onSubmit={createProject}
              >
                {({
                  isSubmitting,
                  touched,
                  errors,
                }) => (
                  <Form noValidate>
                    {errorMsg && (
                      <Alert variant="danger">
                        {errorMsg}
                      </Alert>
                    )}
                    <h3 className="h6">Info</h3>
                    <div className="row">
                      <div className="col">
                        <div className="form-group">
                          <LabeledFormField
                            name="projectName"
                            label="Name"
                            addOn={(
                              <span className="text-muted font-weight-bold">
                                {organization}
                                {' '}
                                /
                              </span>
                            )}
                            placeholder="my-project"
                            invalid={touched.projectName && errors.projectName != null}
                            validationFeedback="Please provide a project name."
                          />
                        </div>
                      </div>
                    </div>
                    <div className="row">
                      <div className="col">
                        <div className="form-group">
                          <label htmlFor="description">Description (optional)</label>
                          <Field
                            as="textarea"
                            className="form-control"
                            id="description"
                            name="description"
                            rows={3}
                          />
                        </div>
                      </div>
                    </div>
                    <div className="row">
                      <div className="col py-3 text-right">
                        <Button type="submit" disabled={isSubmitting || auth.principal?.org !== null}>Create</Button>
                      </div>
                    </div>
                  </Form>
                )}
              </Formik>
            </div>
          </div>
        </div>
      </Section>
    </Page>
  );
}
