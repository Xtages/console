import {Codesandbox} from 'react-feather';
import React, {useState} from 'react';
import * as z from 'zod';
import {Field, Form, Formik, FormikErrors} from 'formik';
import Page from 'components/layout/Page';
import {Section, SectionTitle} from 'components/layout/Section';
import ProjectTemplateCard from 'components/project/ProjectTemplateCard';
import {FormikHelpers} from 'formik/dist/types';
import {useHistory} from 'react-router-dom';
import {Alert, Button} from 'react-bootstrap';
import LabeledFormField from '../components/form/LabeledFormField';
import {projectApi} from '../service/Services';
import {CreateProjectReqTypeEnum} from '../gen/api';
import {useAuth} from '../hooks/useAuth';

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
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const auth = useAuth();
  const organization = auth.principal?.org!!;
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
      if (e.isAxiosError && e.response.status === 409) {
        setErrorMsg(`A GitHub repository named "${organization}/${values.projectName}" already exists.`);
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
          A new
          {' '}
          <strong>private</strong>
          {' '}
          GitHub repository by the same name as the project will be created,
          as well as CI and CD pipelines.
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
                      <Alert className="alert-outline-danger">
                        <div className="d-flex justify-content-center">
                          <strong>{errorMsg}</strong>
                        </div>
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
                        <Button type="submit" disabled={isSubmitting}>Create</Button>
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
