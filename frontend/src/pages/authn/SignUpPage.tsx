import React, {useState} from 'react';
import {Link, useHistory} from 'react-router-dom';
import {Field, Form, Formik} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import {Briefcase, User} from 'react-feather';
import * as z from 'zod';
import cx from 'classnames';
import {NullablePrincipal, useAuth} from 'hooks/useAuth';
import redirectToStripeCheckoutSession from 'service/CheckoutService';
import Logo from 'components/Logos';
import LabeledFormField from 'components/form/LabeledFormField';
import {Alert} from 'react-bootstrap';
import {EmailField, PasswordField} from 'components/user/AuthFields';
import {useTracking} from 'hooks/useTracking';
import {getFormValidator} from 'helpers/form';
import {usePriceId} from 'hooks/usePriceId';
import {DocsLink} from 'components/link/XtagesLink';

const signUpFormValuesSchema = z.object({
  name: z.string()
    .nonempty(),
  organizationName: z.string()
    .nonempty(),
  email: z.string()
    .email()
    .nonempty(),
  // A password must be at least 12 characters long
  password: z.string()
    .min(12),
  acceptedTerms: z.boolean().refine((val) => val),
});

export type SignUpFormValues = z.infer<typeof signUpFormValuesSchema>;

/**
 * Component that renders the sign up page.
 */
export default function SignUpPage() {
  const initialValues: SignUpFormValues = {
    name: '',
    organizationName: '',
    email: '',
    password: '',
    acceptedTerms: false,
  };
  const auth = useAuth();
  const history = useHistory();
  const {priceId} = usePriceId();
  const {
    identifyPrincipal,
    trackComponentApiError,
    trackComponentEvent,
  } = useTracking();
  const [errorOccurred, setErrorOccurred] = useState(false);

  async function signUp(values: SignUpFormValues, actions: FormikHelpers<SignUpFormValues>) {
    setErrorOccurred(false);
    let principal: NullablePrincipal;
    try {
      principal = await auth.signUp({
        name: values.name,
        org: values.organizationName,
        username: values.email,
        password: values.password,
      });
      trackComponentEvent('SignUpPage', 'Signed Up', {
        priceId,
      });
    } catch (e) {
      setErrorOccurred(true);
      trackComponentApiError('SignUpPage', 'signUp', e);
      return;
    }
    actions.setSubmitting(false);
    if (principal == null) {
      trackComponentEvent('SignUpPage', 'User Confirmation Required');
      history.push('/confirm', values);
    } else {
      trackComponentEvent('SignUpPage', 'Signed Up');
      identifyPrincipal(principal);
      await redirectToStripeCheckoutSession({
        priceIds: [priceId!],
        organizationName: values.organizationName,
      });
    }
  }

  const validate = getFormValidator('SignUpPage', signUpFormValuesSchema);

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-8 col-lg-5 py-6">
            <div>
              <div className="mb-5 text-center">
                <Logo size="sm" />
                <h1 className="h3 mb-1">Create your account</h1>
              </div>
              <span className="clearfix" />
              <Formik initialValues={initialValues} validate={validate} onSubmit={signUp}>
                {({isSubmitting, touched, errors}) => (
                  <Form noValidate>
                    {errorOccurred && (
                    <Alert className="alert-outline-danger">
                      <div className="d-flex justify-content-center">
                        <strong>An unexpected error occurred</strong>
                      </div>
                    </Alert>
                    )}
                    {!priceId && (
                    <Alert className="alert-outline-danger">
                      <div className="d-flex justify-content-center">
                        <strong>
                          You must first select a
                          {' '}
                          <a href="https://www.xtages.com/pricing.html">plan</a>
                          {' '}
                          before signing up.
                        </strong>
                      </div>
                    </Alert>
                    )}
                    <LabeledFormField
                      type="text"
                      name="name"
                      label="Name"
                      placeholder="Santa Claus"
                      invalid={touched.name && errors.name != null}
                      validationFeedback="Please provide your name."
                      addOn={<User size="1em" />}
                    />
                    <LabeledFormField
                      type="text"
                      name="organizationName"
                      label={(
                        <>
                          GitHub organization name
                          <DocsLink articlePath="/github" title="GitHub Integration" />
                        </>
                      )}
                      placeholder="NorthPole"
                      invalid={touched.organizationName && errors.organizationName != null}
                      validationFeedback="Please provide your GitHub organization name."
                      addOn={<Briefcase size="1em" />}
                    />
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
                        disabled={isSubmitting || !priceId}
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
