import React, {useState} from 'react';
import {RouteComponentProps, useHistory} from 'react-router-dom';
import {Form, Formik} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import * as z from 'zod';
import {Principal, useAuth} from 'hooks/useAuth';
import CreateAccountLink from 'pages/authn/CreateAccountLink';
import Logo from 'components/Logos';
import {Alert, Col, Container, Row} from 'react-bootstrap';
import {EmailField, PasswordField} from 'components/user/AuthFields';
import {CognitoUser} from 'amazon-cognito-identity-js';
import {useFormValidator} from 'hooks/useFormValidator';
import {useTracking} from 'hooks/useTracking';

/** The properties that are available to the {@link LoginPage} component. */
type LoginPageProps = RouteComponentProps<{}, {}, LocationState | null>;

/** The state that is provided when redirecting to this component. */
interface LocationState {
  referrer: string;
}

const loginFormValuesSchema = z.object({
  email: z.string()
    .nonempty(),
  password: z.string()
    .nonempty(),
});

type LoginFormValues = z.infer<typeof loginFormValuesSchema>;

/**
 * Component that renders the login page.
 *
 * @param location - The {@link LocationState} passed to this component by
 *     the `react-router` {@link Router}.
 */
export default function LoginPage({location}: LoginPageProps) {
  const initialValues: LoginFormValues = {
    email: '',
    password: '',
  };

  const auth = useAuth();
  const history = useHistory();
  const {
    identifyPrincipal,
    trackComponentApiError,
    trackComponentEvent,
  } = useTracking();
  const [errorOccurred, setErrorOccurred] = useState(false);
  const validate = useFormValidator('LoginPage', loginFormValuesSchema);

  async function logIn(values: LoginFormValues, actions: FormikHelpers<LoginFormValues>) {
    setErrorOccurred(false);
    try {
      const result = await auth.logIn({
        username: values.email,
        password: values.password,
      });
      if (result instanceof CognitoUser && result.challengeName === 'NEW_PASSWORD_REQUIRED') {
        trackComponentEvent('LoginPage', 'New Password Required');
        actions.setSubmitting(false);
        const queryParams = new URLSearchParams();
        queryParams.set('email', values.email);
        history.push(`/changePassword?${queryParams.toString()}`, {
          ...location.state,
          username: values.email,
          password: values.password,
        });
        return;
      }
      if (result instanceof Principal) {
        trackComponentEvent('LoginPage', 'Logged In');
        identifyPrincipal(result);
      }

      const redirectTo = location.state?.referrer || '/';
      history.replace(redirectTo);
    } catch (e) {
      trackComponentApiError('LoginPage', 'logIn', e);
      setErrorOccurred(true);
    }
    actions.setSubmitting(false);
  }

  return (
    <section>
      <Container className="d-flex flex-column">
        <Row className="align-items-center justify-content-center min-vh-100">
          <Col sm={4} className="py-6 py-md-0">
            <div>
              <div className="mb-5 text-center">
                <Logo size="sm" full={false} />
                <h1 className="h3 mb-1">Sign in to Xtages</h1>
              </div>
              <span className="clearfix" />
              <Formik
                initialValues={initialValues}
                onSubmit={logIn}
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
                    <EmailField autoComplete="username" />
                    <PasswordField placeholder="Password" autoComplete="current-password" />
                    <div className="mt-4">
                      <button
                        type="submit"
                        className="btn btn-block btn-primary"
                        disabled={isSubmitting}
                      >
                        Sign in
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
