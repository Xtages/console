import React, {useState} from 'react';
import {Link, useHistory} from 'react-router-dom';
import {Field, Form, Formik, FormikErrors} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import {Briefcase, User} from 'react-feather';
import * as z from 'zod';
import cx from 'classnames';
import {NullablePrincipal, useAuth} from 'hooks/useAuth';
import redirectToStripeCheckoutSession from 'service/CheckoutService';
import Logo from 'components/Logos';
import LabeledFormField from 'components/form/LabeledFormField';
import {Alert} from 'react-bootstrap';
import {EmailField, PasswordField} from './AuthFields';

const signUpFormValuesSchema = z.object({
  name: z.string()
    .nonempty(),
  organizationName: z.string()
    .nonempty(),
  email: z.string()
    .email()
    .nonempty(),
  // A password must be at least 8 characters long, contain uppercase and and lowercase letters,
  // a number and a special character
  password: z.string()
    .min(8)
    .refine((val) => /[A-Z]/g.test(val)
        && /[a-z]/g.test(val)
        && /\d/g.test(val)
        // eslint-disable-next-line no-useless-escape
        && /[\=\+\-\^\$\*\.\[\]\{\}\(\)\?\"\!\@\#\%\&\/\\\,\>\<\'\:\;\|\_\~\`]/g.test(val)),
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
  const [errorOccurred, setErrorOccurred] = useState(false);

  async function signUp(values: SignUpFormValues, actions: FormikHelpers<SignUpFormValues>) {
    setErrorOccurred(false);
    let principal: NullablePrincipal;
    try {
      principal = await auth.signUp({
        name: values.name,
        org: values.organizationName,
        email: values.email,
        password: values.password,
      });
    } catch (e) {
      setErrorOccurred(true);
      return;
    }
    if (principal == null) {
      history.replace('/confirm', values);
    } else {
      await redirectToStripeCheckoutSession({
        priceIds: ['price_1IdOOzIfxICi4AQgjta89k2Y'],
        organizationName: values.organizationName,
      });
    }
    actions.setSubmitting(false);
  }

  function validate(values: SignUpFormValues): FormikErrors<SignUpFormValues> | void {
    try {
      signUpFormValuesSchema.parse(values);
      return {};
    } catch (error) {
      return error.formErrors.fieldErrors;
    }
  }

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
                      label="GitHub organization name"
                      placeholder="North Pole"
                      invalid={touched.organizationName && errors.organizationName != null}
                      validationFeedback="Please provide your GitHub organization name."
                      addOn={<Briefcase size="1em" />}
                    />
                    <EmailField
                      invalid={touched.email && errors.email != null}
                      validationFeedback="Please provide a valid email address."
                    />
                    <PasswordField
                      invalid={touched.password && errors.password != null}
                      validationFeedback="Invalid password."
                      showHelpTooltip
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
                          <a href="../../pages/utility/terms.html">
                            terms and conditions
                          </a>
                        </label>
                        <div className="invalid-feedback">
                          In order to use Xtages you must accept our terms of service.
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
