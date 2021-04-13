import React from 'react';
import {RouteComponentProps, useHistory} from 'react-router-dom';
import {Form, Formik} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import {useAuth} from '../../hooks/useAuth';
import CreateAccountLink from '../../components/CreateAccountLink';
import {EmailField, PasswordField} from './AuthFields';

/** The properties that are available to the {@link LoginPage} component. */
type LoginPageProps = RouteComponentProps<{}, {}, LocationState | null>;

/** The state that is provided when redirecting to this component. */
interface LocationState {
  referrer: string;
}

interface LoginFormValues {
  email: string;
  password: string;
}

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

  async function logIn(values: LoginFormValues, actions: FormikHelpers<LoginFormValues>) {
    await auth.logIn({email: values.email, password: values.password});
    const redirectTo = location.state?.referrer || '/';
    history.replace(redirectTo);
    actions.setSubmitting(false);
  }

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-6 col-lg-5 col-xl-4 py-6 py-md-0">
            <div>
              <div className="mb-5 text-center">
                <h1 className="h3 mb-1">Login</h1>
                <p className="text-muted mb-0">
                  Sign in to your account to continue.
                </p>
              </div>
              <span className="clearfix" />
              <Formik initialValues={initialValues} onSubmit={logIn}>
                <Form>
                  <EmailField />
                  <PasswordField />
                  <div className="mt-4">
                    <button type="submit" className="btn btn-block btn-primary">
                      Sign in
                    </button>
                  </div>
                </Form>
              </Formik>
              <CreateAccountLink />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
