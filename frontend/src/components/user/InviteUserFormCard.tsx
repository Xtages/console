import {Form, Formik, FormikErrors} from 'formik';
import React, {useState} from 'react';
import {Alert, Button, Card, Col, Row} from 'react-bootstrap';
import * as z from 'zod';
import {FormikHelpers} from 'formik/dist/types';
import {User} from 'react-feather';
import LabeledFormField from 'components/form/LabeledFormField';
import {userApi} from 'service/Services';
import {useQueryClient} from 'react-query';
import {EmailField} from './AuthFields';

const inviteUserFormValuesSchema = z.object({
  name: z.string()
    .nonempty(),
  email: z.string()
    .email()
    .nonempty(),
});

type InviteUserFormValues = z.infer<typeof inviteUserFormValuesSchema>;

/**
 * A form to invite users, rendered as a card.
 */
export function InviteUserFormCard() {
  const initialValues: InviteUserFormValues = {
    name: '',
    email: '',
  };
  const [errorOccurred, setErrorOccurred] = useState(false);
  const queryClient = useQueryClient();

  async function inviteUser(
    values: InviteUserFormValues, actions: FormikHelpers<InviteUserFormValues>,
  ) {
    setErrorOccurred(false);
    try {
      await userApi.inviteUser({
        name: values.name,
        username: values.email,
      });
      await queryClient.invalidateQueries('users');
      actions.resetForm();
    } catch (e) {
      setErrorOccurred(true);
    } finally {
      actions.setSubmitting(false);
    }
  }

  function validate(values: InviteUserFormValues): FormikErrors<InviteUserFormValues> {
    try {
      inviteUserFormValuesSchema.parse(values);
      return {};
    } catch (error) {
      return error.formErrors.fieldErrors;
    }
  }

  return (
    <Card>
      <Card.Body>
        <Formik
          initialValues={initialValues}
          validate={validate}
          onSubmit={inviteUser}
        >
          {({
            isSubmitting,
            touched,
            errors,
          }) => (
            <Form noValidate>
              {errorOccurred && (
              <Alert variant="danger">
                <div className="d-flex justify-content-center">
                  <strong>An unexpected error occurred</strong>
                </div>
              </Alert>
              )}
              <Row>
                <Col>
                  <LabeledFormField
                    type="text"
                    name="name"
                    label="Name"
                    placeholder="Santa Claus"
                    sizeVariant="sm"
                    invalid={touched.name && errors.name != null}
                    validationFeedback="Please provide a name."
                    addOn={<User size="1em" />}
                  />
                </Col>
                <Col>
                  <EmailField
                    sizeVariant="sm"
                    invalid={touched.email && errors.email != null}
                    validationFeedback="Please provide a valid email address."
                  />
                </Col>
                <Col sm="auto" className="text-right d-flex align-items-center">
                  <span className="pt-3">
                    <Button type="submit" disabled={isSubmitting} className="btn-xs">Invite</Button>
                    <Button
                      type="reset"
                      variant="secondary"
                      disabled={isSubmitting}
                      className="btn-xs"
                    >
                      Clear
                    </Button>
                  </span>
                </Col>
              </Row>
            </Form>
          )}
        </Formik>
      </Card.Body>
    </Card>
  );
}
