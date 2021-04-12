import {RouteComponentProps} from 'react-router-dom';
import React from 'react';
import {Zap} from 'react-feather';
import {Field, Form, Formik} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import CreateAccountLink from '../../components/CreateAccountLink';
import {useAuth} from '../../hooks/useAuth';
import redirectToStripeCheckoutSession from '../../service/CheckoutService';
import {SignUpFormValues} from './SignUpPage';
import {EmailField, PasswordField} from './AuthFields';
import {organizationApi} from '../../service/Services';

/** The properties that are available to the {@link ConfirmSignUpPage} component. */
type ConfirmSignUpPageProps = RouteComponentProps<{}, {}, SignUpFormValues | null>;

interface FormValues {
  email: string;
  password: string;
  code: string;
}

/**
 * A full page to confirm a user's signup by providing a code that was emailed to the user.
 * @param location - Location information for the {@link Route}.
 */
export default function ConfirmSignUpPage({location}: ConfirmSignUpPageProps) {
  function buildFormValues(): FormValues {
    const {state} = location;
    if (state != null) {
      return {
        email: state.email!,
        password: state.password!,
        code: '',
      };
    }
    return {
      email: '',
      password: '',
      code: '',
    };
  }

  const initialValues = buildFormValues();
  const auth = useAuth();

  async function confirm(values: FormValues, actions: FormikHelpers<FormValues>) {
    await auth.confirmSignUp({
      email: values.email,
      code: values.code,
    });
    const principal = await auth.signIn({
      email: values.email,
      password: values.password,
    });
    await organizationApi.createOrganization({
      organizationName: principal.org,
      ownerCognitoUserId: principal.id,
    });
    await redirectToStripeCheckoutSession({
      priceIds: ['price_1IdOOzIfxICi4AQgjta89k2Y'],
      organizationName: principal.org,
    });
    actions.setSubmitting(false);
  }

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-6 col-lg-5 col-xl-4">
            <div className="mb-5 text-center">
              <h6 className="h3 mb-1">Confirm your account</h6>
            </div>
            <span className="clearfix" />
            <Formik initialValues={initialValues} onSubmit={confirm}>
              <Form>
                {location.state == null
                                && (
                                <>
                                  <EmailField />
                                  <PasswordField />
                                </>
                                )}
                <div className="form-group">
                  <label className="form-control-label" htmlFor="code">
                    Confirmation code
                  </label>
                  <div className="input-group input-group-merge">
                    <div className="input-group-prepend">
                      <span className="input-group-text">
                        <Zap size="1em" />
                      </span>
                    </div>
                    <Field
                      type="text"
                      className="form-control form-control-prepend"
                      id="code"
                      name="code"
                    />
                  </div>
                </div>
                <div className="mt-4">
                  <button
                    type="submit"
                    className="btn btn-block btn-primary"
                  >
                    Confirm
                  </button>
                </div>
              </Form>
            </Formik>
            <CreateAccountLink />
          </div>
        </div>
      </div>
    </section>
  );
}
