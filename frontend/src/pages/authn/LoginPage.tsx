import React, {useState} from 'react';
import {RouteComponentProps, useHistory} from 'react-router-dom';
import {Form, Formik, FormikErrors} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import * as z from 'zod';
import {useAuth} from 'hooks/useAuth';
import CreateAccountLink from 'components/CreateAccountLink';
import Logo from 'components/Logos';
import Alert from 'components/alert/Alerts';
import {EmailField, PasswordField} from './AuthFields';

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
  const [errorOccurred, setErrorOccurred] = useState(false);

  async function logIn(values: LoginFormValues, actions: FormikHelpers<LoginFormValues>) {
    setErrorOccurred(false);
    try {
      await auth.logIn({
        email: values.email,
        password: values.password,
      });
      const redirectTo = location.state?.referrer || '/';
      history.replace(redirectTo);
    } catch (e) {
      setErrorOccurred(true);
    }
    actions.setSubmitting(false);
  }

  function validate(values: LoginFormValues): FormikErrors<LoginFormValues> | void {
    try {
      loginFormValuesSchema.parse(values);
      return {};
    } catch (error) {
      return error.formErrors.fieldErrors;
    }
  }

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-6 col-lg-5 col-xl-4 py-6 py-md-0">
            <div>
              <div className="mb-5 text-center">
                <Logo size="SMALL" full={false} />
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
                    <Alert color="danger" outline>
                      <div className="d-flex justify-content-center">
                        <strong>Incorrect username or password</strong>
                      </div>
                    </Alert>
                    )}
                    <EmailField />
                    <PasswordField placeholder="Password" />
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
          </div>
        </div>
      </div>
    </section>
  );
}
