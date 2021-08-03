import {useLocation} from 'react-router-dom';
import React, {useState} from 'react';
import {Zap} from 'react-feather';
import {Form, Formik, FormikErrors, useFormikContext} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import * as z from 'zod';
import CreateAccountLink from 'pages/authn/CreateAccountLink';
import {Principal, useAuth} from 'hooks/useAuth';
import redirectToStripeCheckoutSession from 'service/CheckoutService';
import {organizationApi} from 'service/Services';
import Logo from 'components/Logos';
import LabeledFormField from 'components/form/LabeledFormField';
import {Alert, Button} from 'react-bootstrap';
import {EmailField, PasswordField} from 'components/user/AuthFields';
import {SignUpFormValues} from './SignUpPage';

const formValuesSchema = z.object({
  email: z.string()
    .email(),
  password: z.string()
    .nonempty(),
  code: z.string()
    .regex(/\d+/),
});

type FormValues = z.infer<typeof formValuesSchema>;

function ResendCodeButton({state}: {state: SignUpFormValues | null | undefined}) {
  const auth = useAuth();
  const {values, errors} = useFormikContext<FormValues>();
  const [confirmationCodeWasSent, setConfirmationCodeWasSent] = useState(false);

  async function resendVerificationCode() {
    const email = getEmail();
    if (email != null) {
      await auth.resendConfirmationCode({email});
      setConfirmationCodeWasSent(true);
    }
  }

  function getEmail() {
    if (state?.email != null) {
      return state?.email!!;
    }
    if (errors.email == null) {
      return values.email;
    }
    return null;
  }

  const hasEmail = () => getEmail() != null;

  return (
    <>
      {hasEmail()
            && (
            <div className="mt-4 container">
              {confirmationCodeWasSent
                    && (
                    <div className="pb-2 d-flex justify-content-center row row-cols-1">
                      <div className="col-12">
                        <Alert className="alert-outline-success">
                          <div className="text-center">
                            Your confirmation code was sent.
                            Please check your email.
                          </div>
                        </Alert>
                      </div>
                    </div>
                    )}
              <div className="row row-cols-1">
                <div className="col-12 d-flex justify-content-center">
                  <Button variant="link" onClick={resendVerificationCode} className="btn-xs">
                    Re-send verification code
                  </Button>
                </div>
              </div>
            </div>
            )}
    </>
  );
}

/**
 * A full page to confirm a user's signup by providing a code that was emailed to the user.
 */
export default function ConfirmSignUpPage() {
  const location = useLocation<SignUpFormValues>();
  const initialValues = buildFormValues();
  const auth = useAuth();
  const [errorOccurred, setErrorOccurred] = useState(false);

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

  async function confirm(values: FormValues, actions: FormikHelpers<FormValues>) {
    setErrorOccurred(false);
    try {
      await auth.confirmSignUp({
        email: values.email,
        code: values.code,
      });
    } catch (e) {
      setErrorOccurred(true);
      actions.setSubmitting(false);
      return;
    }
    const principal = await auth.logInForOrgSignup({
      username: values.email,
      password: values.password,
    });
    actions.setSubmitting(false);
    if (principal instanceof Principal) {
      await organizationApi.createOrganization({
        organizationName: principal.org,
        ownerCognitoUserId: principal.id,
      });
      const params = new URLSearchParams(location.search);
      await redirectToStripeCheckoutSession({
        priceIds: [params.get('priceId')!],
        organizationName: principal.org,
      });
    } else {
      setErrorOccurred(true);
    }
  }

  function validate(values: FormValues): FormikErrors<FormValues> | void {
    try {
      formValuesSchema.parse(values);
      return {};
    } catch (error) {
      return error.formErrors.fieldErrors;
    }
  }

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-6 col-lg-5 col-xl-4">
            <div className="mb-5 text-center">
              <Logo size="sm" />
              <h1 className="h3 mb-1">Confirm your account</h1>
            </div>
            <span className="clearfix" />
            <Formik
              initialValues={initialValues}
              validate={validate}
              onSubmit={confirm}
            >
              {({
                isSubmitting,
                touched,
                errors,
              }) => {
                const haveUserAndPass = location.state == null;
                return (
                  <Form noValidate>
                    {errorOccurred && (
                    <Alert className="alert-outline-danger">
                      <div className="d-flex justify-content-center">
                        <strong>
                          {haveUserAndPass
                            ? 'Incorrect username or password or code'
                            : 'Incorrect code'}
                        </strong>
                      </div>
                    </Alert>
                    )}
                    {haveUserAndPass
                      && (
                      <>
                        <EmailField
                          invalid={touched.email && errors.email != null}
                          validationFeedback="Please provide your email address."
                          autoComplete="username"
                        />
                        <PasswordField
                          invalid={touched.password && errors.password != null}
                          validationFeedback="Invalid password."
                          autoComplete="current-password"
                        />
                      </>
                      )}
                    <LabeledFormField
                      type="text"
                      name="code"
                      label="Confirmation code"
                      invalid={touched.code && errors.code != null}
                      validationFeedback="Invalid confirmation code."
                      addOn={<Zap size="1em" />}
                      autoComplete="off"
                    />
                    <div className="mt-4">
                      <button
                        type="submit"
                        className="btn btn-block btn-primary"
                        disabled={isSubmitting}
                      >
                        Confirm
                      </button>
                    </div>
                    <ResendCodeButton state={location?.state} />
                  </Form>
                );
              }}
            </Formik>
            <CreateAccountLink />
          </div>
        </div>
      </div>
    </section>
  );
}
