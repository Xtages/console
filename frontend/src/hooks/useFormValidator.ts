import {FormikErrors} from 'formik';
import {useTracking} from 'hooks/useTracking';
import {ZodType, ZodTypeDef} from 'zod';

export function useFormValidator<V = unknown, D extends ZodTypeDef = ZodTypeDef>(
  component: string, schema: ZodType<V, D>,
) {
  const {trackComponentError} = useTracking();
  return (values: V): FormikErrors<V> => {
    try {
      schema.parse(values);
      return {};
    } catch (error) {
      trackComponentError(component, 'formValidation', {
        error,
      });
      return error.formErrors.fieldErrors;
    }
  };
}
