import React from 'react';
import {Link, useHistory} from 'react-router-dom';
import {Field, Form, Formik} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import {Briefcase, User} from 'react-feather';
import {useAuth} from '../../hooks/useAuth';
import redirectToStripeCheckoutSession from '../../service/CheckoutService';
import {EmailField, PasswordField} from './AuthFields';
import Logo from '../../components/Logos';

export interface SignUpFormValues {
  name: string;
  organizationName: string;
  email: string;
  password: string;
  acceptedTerms: boolean;
}

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

  async function signUp(values: SignUpFormValues, actions: FormikHelpers<SignUpFormValues>) {
    const principal = await auth.signUp({
      name: values.name,
      org: values.organizationName,
      email: values.email,
      password: values.password,
    });
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

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-8 col-lg-5 py-6">
            <div>
              <div className="mb-5 text-center">
                <Logo size="SMALL" />
                <h1 className="h3 mb-1">Create your account</h1>
              </div>
              <span className="clearfix" />
              <Formik initialValues={initialValues} onSubmit={signUp}>
                <Form>
                  <div className="form-group">
                    <label className="form-control-label" htmlFor="name">
                      Name
                    </label>
                    <div className="input-group input-group-merge">
                      <div className="input-group-prepend">
                        <span className="input-group-text">
                          <User size="1em" />
                        </span>
                      </div>
                      <Field
                        type="text"
                        className="form-control form-control-prepend"
                        name="name"
                        placeholder="Santa Claus"
                      />
                    </div>
                  </div>
                  <div className="form-group">
                    <label
                      className="form-control-label"
                      htmlFor="organization"
                    >
                      GitHub organization name
                    </label>
                    <div className="input-group input-group-merge">
                      <div className="input-group-prepend">
                        <span className="input-group-text">
                          <Briefcase size="1em" />
                        </span>
                      </div>
                      <Field
                        type="text"
                        className="form-control form-control-prepend"
                        name="organizationName"
                        placeholder="Northpole"
                      />
                    </div>
                  </div>
                  <EmailField />
                  <PasswordField />
                  <div className="my-4">
                    <div className="custom-control custom-checkbox mb-3">
                      <Field
                        type="checkbox"
                        className="custom-control-input"
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
                    </div>
                  </div>
                  <div className="mt-4">
                    <button type="submit" className="btn btn-block btn-primary">
                      Create my account
                    </button>
                  </div>
                </Form>
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
