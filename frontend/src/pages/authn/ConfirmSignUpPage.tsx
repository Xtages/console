import {RouteComponentProps} from 'react-router-dom';
import React, {useState} from 'react';
import {Zap} from 'react-feather';
import {Form, Formik, FormikErrors, useFormikContext} from 'formik';
import {FormikHelpers} from 'formik/dist/types';
import * as z from 'zod';
import CreateAccountLink from 'components/CreateAccountLink';
import {useAuth} from 'hooks/useAuth';
import redirectToStripeCheckoutSession from 'service/CheckoutService';
import {organizationApi} from 'service/Services';
import Logo from 'components/Logos';
import LabeledFormField from 'components/form/LabeledFormField';
import Alert from 'components/alert/Alerts';
import {Button} from 'components/button/Buttons';
import {EmailField, PasswordField} from './AuthFields';
import {SignUpFormValues} from './SignUpPage';

/** The properties that are available to the {@link ConfirmSignUpPage} component. */
type ConfirmSignUpPageProps = RouteComponentProps<{}, {}, SignUpFormValues | null>;

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
                        <Alert color="success" outline>
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
                  <Button onClick={resendVerificationCode} size="xs" asLink>
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
  const [errorOccurred, setErrorOccurred] = useState(false);

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
    const principal = await auth.logIn({
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
                      <Alert color="danger" outline>
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
                        />
                        <PasswordField
                          invalid={touched.password && errors.password != null}
                          validationFeedback="Invalid password."
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
