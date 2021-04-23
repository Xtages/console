import {Field, GenericFieldHTMLAttributes} from 'formik';
import React, {ReactNode, useState} from 'react';
import cx from 'classnames';
import styles from './InputGroup.module.scss';

export type LabeledFormFieldProps = GenericFieldHTMLAttributes & {
  name: string;
  icon: ReactNode | undefined | null
  label: string | ReactNode;
  invalid?: boolean;
  validationFeedback?: string | undefined | null;
};

/**
 * A combination of label and input field.
 * @param name - Name of the field.
 * @param icon - Optional icon to prepend to the input field.
 * @param label - Label for the field, can either be a `string` or a {@link ReactNode}.
 * @param invalid - Whether the field has valid data.
 * @param validationFeedback - Feedback message rendered when `invalid` is `true`.
 * @param props - The rest of the passed props are applied to the input.
 */
export default function LabeledFormField({
  name,
  icon,
  label,
  invalid = false,
  validationFeedback,
  ...props
}: LabeledFormFieldProps) {
  const [focused, setFocused] = useState(false);
  const toggleFocused = () => setFocused(!focused);

  const validationFeedbackElId = validationFeedback ? `${name}ValidationFeedback` : undefined;
  const labelEl = typeof label === 'string'
    ? (<label className="form-control-label" htmlFor={name}>{label}</label>)
    : label;

  return (
    <div className={cx('form-group', {[`${styles.focused}`]: focused})}>
      {labelEl}
      <div className={cx('input-group', {
        'input-group-merge': icon,
        'has-validation': validationFeedback,
      })}
      >
        {icon
                && (
                <div className="input-group-prepend">
                  <span className={cx('input-group-text', {'is-invalid': invalid})}>
                    {icon}
                  </span>
                </div>
                )}
        <Field
          {...props}
          name={name}
          className={cx('form-control', {
            'form-control-prepend': icon,
            'is-invalid': invalid,
          })}
          onFocus={toggleFocused}
          onBlur={toggleFocused}
          aria-describedby={validationFeedbackElId}
        />
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
