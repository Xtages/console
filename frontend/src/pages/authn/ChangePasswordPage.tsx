import React, {useState} from 'react';
import {Alert, Col, Container, Row} from 'react-bootstrap';
import Logo from 'components/Logos';
import {Form, Formik, FormikErrors} from 'formik';
import {EmailField, PasswordField} from 'components/user/AuthFields';
import CreateAccountLink from 'pages/authn/CreateAccountLink';
import * as z from 'zod';
import {useHistory, useLocation} from 'react-router-dom';
import {FormikHelpers} from 'formik/dist/types';
import {Credentials, Principal, useAuth} from 'hooks/useAuth';
import {CognitoUser} from 'amazon-cognito-identity-js';

const completePasswordFormValuesSchema = z.object({
  email: z.string()
    .nonempty(),
  oldPassword: z.string()
    .nonempty(),
  password: z.string()
    .nonempty(),
});

type CompletePasswordFormValues = z.infer<typeof completePasswordFormValuesSchema>;

type LocationState = {
  referrer: string | undefined | null;
} & Credentials;

/**
 * Renders a form to change the users password.
 */
export default function ChangePasswordPage() {
  const location = useLocation<LocationState>();
  const email = new URLSearchParams(location.search).get('email');
  const initialValues = {
    email: email ?? location.state?.username ?? '',
    oldPassword: location?.state?.password ?? '',
    password: '',
  };
  const auth = useAuth();
  const history = useHistory();
  const [errorOccurred, setErrorOccurred] = useState(false);

  async function changePassword(values: CompletePasswordFormValues,
    actions: FormikHelpers<CompletePasswordFormValues>) {
    setErrorOccurred(false);
    try {
      const result = await auth.logIn({
        username: values.email,
        password: values.oldPassword,
      });
      if (result instanceof CognitoUser && result.challengeName === 'NEW_PASSWORD_REQUIRED') {
        await auth.completeNewPassword({
          user: result as CognitoUser,
          password: values.password,
        });
        const redirectTo = location.state?.referrer || '/';
        history.replace(redirectTo);
      } else {
        await auth.changePassword({
          user: (result as Principal).cognitoUser,
          oldPassword: values.oldPassword,
          newPassword: values.password,
        });
      }
    } catch (e) {
      setErrorOccurred(true);
    }
    actions.setSubmitting(false);
  }

  function validate(values: CompletePasswordFormValues): FormikErrors<CompletePasswordFormValues> {
    try {
      completePasswordFormValuesSchema.parse(values);
      return {};
    } catch (error) {
      return error.formErrors.fieldErrors;
    }
  }

  return (
    <section>
      <Container className="d-flex flex-column">
        <Row className="align-items-center justify-content-center min-vh-100">
          <Col sm={4} className="py-6 py-md-0">
            <div>
              <div className="mb-5 text-center">
                <Logo size="md" />
                <p>
                  Change your password
                </p>
              </div>
              <Formik
                initialValues={initialValues}
                onSubmit={changePassword}
                validate={validate}
                validateOnBlur={false}
                validateOnChange={false}
              >
                {({isSubmitting}) => (
                  <Form>
                    {errorOccurred && (
                    <Alert className="alert-outline-danger">
                      <div className="d-flex justify-content-center">
                        <strong>Incorrect username or password</strong>
                      </div>
                    </Alert>
                    )}
                    <EmailField />
                    <PasswordField
                      name="oldPassword"
                      label="Previous password"
                      placeholder="Password"
                    />
                    <PasswordField label="New password" placeholder="Password" showHelpTooltip />
                    <div className="mt-4">
                      <button
                        type="submit"
                        className="btn btn-block btn-primary"
                        disabled={isSubmitting}
                      >
                        Confirm
                      </button>
                    </div>
                  </Form>
                )}
              </Formik>
              <CreateAccountLink />
            </div>
          </Col>
        </Row>
      </Container>
    </section>
  );
}
