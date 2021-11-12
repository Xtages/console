import React, {ReactNode, useState} from 'react';
import {Link, useHistory} from 'react-router-dom';
import {Field, Form, Formik} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import * as z from 'zod';
import cx from 'classnames';
import {Principal, useAuth} from 'hooks/useAuth';
import Logo from 'components/Logos';
import {Alert} from 'react-bootstrap';
import {EmailField, PasswordField} from 'components/user/AuthFields';
import {useTracking} from 'hooks/useTracking';
import {useFormValidator} from 'hooks/useFormValidator';
import {Nullable} from 'types/nullable';
import LinkedInTag from 'react-linkedin-insight';

const signUpFormValuesSchema = z.object({
  email: z.string()
    .email()
    .nonempty(),
  // A password must be at least 12 characters long
  password: z.string()
    .min(12),
  acceptedTerms: z.boolean()
    .refine((val) => val),
});

export type SignUpFormValues = z.infer<typeof signUpFormValuesSchema>;

/**
 * Component that renders the sign up page.
 */
export default function SignUpPage() {
  LinkedInTag.track('5610700');
  const initialValues: SignUpFormValues = {
    email: '',
    password: '',
    acceptedTerms: false,
  };
  const auth = useAuth();
  const history = useHistory();
  const {
    identifyPrincipal,
    trackComponentApiError,
    trackComponentEvent,
  } = useTracking();
  const [errorMsg, setErrorMsg] = useState<ReactNode>(null);
  const validate = useFormValidator('SignUpPage', signUpFormValuesSchema);

  async function signUp(values: SignUpFormValues, actions: FormikHelpers<SignUpFormValues>) {
    setErrorMsg(null);
    let principal: Nullable<Principal>;
    try {
      principal = await auth.signUp({
        username: values.email,
        password: values.password,
      });
      trackComponentEvent('SignUpPage', 'Signed Up');
    } catch (e: any) {
      if (e.code === 'UsernameExistsException') {
        setErrorMsg('Invalid email, or an account with the given email already exists.');
      } else {
        setErrorMsg('An unexpected error occurred.');
      }
      trackComponentApiError('SignUpPage', 'cognito.signUp', e);
      return;
    }
    actions.setSubmitting(false);
    if (principal == null) {
      trackComponentEvent('SignUpPage', 'User Confirmation Required');
      history.push('/confirm', values);
    } else {
      trackComponentEvent('SignUpPage', 'Signed Up');
      identifyPrincipal(principal);
    }
  }

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-sm-4 py-6">
            <div>
              <div className="mb-5 text-center">
                <Logo size="sm" />
                <h1 className="h3 mb-1">Create your account</h1>
              </div>
              <span className="clearfix" />
              <Formik initialValues={initialValues} validate={validate} onSubmit={signUp}>
                {({isSubmitting, touched, errors}) => (
                  <Form noValidate>
                    {errorMsg && (
                      <Alert className="alert-outline-danger">
                        <div className="d-flex justify-content-center">
                          <strong>{errorMsg}</strong>
                        </div>
                      </Alert>
                    )}
                    <EmailField
                      invalid={touched.email && errors.email != null}
                      validationFeedback="Please provide a valid email address."
                      autoComplete="username"
                    />
                    <PasswordField
                      invalid={touched.password && errors.password != null}
                      validationFeedback="Passwords must be at least 12 characters long."
                      autoComplete="new-password"
                      showHelpMessage
                    />
                    <div className="my-4">
                      <div className="custom-control custom-checkbox mb-3">
                        <Field
                          type="checkbox"
                          className={cx('custom-control-input', {
                            'is-invalid': touched.acceptedTerms && errors.acceptedTerms != null,
                          })}
                          id="acceptedTerms"
                          name="acceptedTerms"
                        />
                        <label
                          className="custom-control-label"
                          htmlFor="acceptedTerms"
                        >
                          I agree to the
                          {' '}
                          <a href="https://www.xtages.com/terms.html">
                            terms and conditions
                          </a>
                          {' '}
                          and
                          {' '}
                          <a href="https://www.xtages.com/privacy.html">
                            privacy policy
                          </a>
                          .
                        </label>
                        <div className="invalid-feedback">
                          In order to use Xtages you must accept our
                          terms of service and privacy policy.
                        </div>
                      </div>
                    </div>
                    <div className="mt-4">
                      <button
                        type="submit"
                        className="btn btn-block btn-primary"
                        disabled={isSubmitting}
                      >
                        Create my account
                      </button>
                    </div>
                  </Form>
                )}
              </Formik>
              <div className="mt-4 text-center">
                <small>Already have an account?</small>
                {' '}
                <Link to="/login" className="small font-weight-bold">
                  Sign in
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
