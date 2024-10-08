import {Field} from 'formik';
import React, {ReactNode, useState} from 'react';
import cx from 'classnames';

export type LabeledFormFieldProps = JSX.IntrinsicElements['input'] & {
  name: string;
  addOn: ReactNode | undefined | null
  label: string | ReactNode;
  invalid?: boolean;
  validationFeedback?: string | undefined | null;
  sizeVariant?: 'sm' | 'lg' | undefined;
};

/**
 * A combination of label and input field.
 * @param name - Name of the field.
 * @param addOn - Optional add-on to prepend to the input field.
 * @param label - Label for the field, can either be a `string` or a {@link ReactNode}.
 * @param invalid - Whether the field has valid data.
 * @param validationFeedback - Feedback message rendered when `invalid` is `true`.
 * @param props - The rest of the passed props are applied to the input.
 */
export default function LabeledFormField({
  name,
  addOn,
  label,
  invalid = false,
  validationFeedback,
  sizeVariant = undefined,
  ...props
}: LabeledFormFieldProps) {
  const [focused, setFocused] = useState(false);
  const toggleFocused = () => setFocused(!focused);

  const validationFeedbackElId = validationFeedback ? `${name}ValidationFeedback` : undefined;
  const labelEl = typeof label === 'string'
    ? (<label className="form-control-label" htmlFor={name}>{label}</label>)
    : label;

  return (
    <div className="form-group">
      {labelEl}
      <div className={cx('input-group', {
        'input-group-merge': addOn,
        [`input-group-${sizeVariant}`]: sizeVariant,
        'has-validation': validationFeedback,
      })}
      >
        <Field
          {...props}
          name={name}
          className={cx('form-control', {
            'form-control-prepend': addOn,
            'is-invalid': invalid,
          })}
          onFocus={toggleFocused}
          onBlur={toggleFocused}
          aria-describedby={validationFeedbackElId}
        />
        {addOn
          && (
          <div className="input-group-prepend">
            <span className={cx('input-group-text', {'is-invalid': invalid})}>
              {addOn}
            </span>
          </div>
          )}
        {validationFeedback
                && (
                <div id={validationFeedbackElId} className="invalid-feedback">
                  {validationFeedback}
                </div>
                )}
      </div>
    </div>
  );
}
