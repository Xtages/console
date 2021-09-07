import {AtSign, Key} from 'react-feather';
import React, {useState} from 'react';
import LabeledFormField, {LabeledFormFieldProps} from 'components/form/LabeledFormField';
import {Button} from 'react-bootstrap';

/**
 * Email `formik` field.
 */
export function EmailField(props: Partial<LabeledFormFieldProps>) {
  return (
    <LabeledFormField
      {...props}
      type="email"
      name="email"
      label="Email address"
      placeholder="santa@northpole.com"
      addOn={<AtSign size="1em" />}
    />
  );
}

/**
 * Password `formik` field.
 */
export function PasswordField({
  label = 'Password',
  name = 'password',
  showHelpMessage = false,
  placeholder = '********',
  ...props
}: {
  label?: string,
  name?: string,
  showHelpMessage?: boolean,
} & Partial<LabeledFormFieldProps>) {
  const [showPassword, setShowPassword] = useState(false);

  let helpMessage;
  if (showHelpMessage) {
    helpMessage = (
      <>
        {' '}
        <span className="text-xs">(at least 12 characters long)</span>
      </>
    );
  }

  return (
    <LabeledFormField
      {...props}
      type={showPassword ? 'text' : 'password'}
      name={name}
      placeholder={placeholder}
      addOn={
        <Key size="1em" />
}
      label={(
        <div className="d-flex align-items-center justify-content-between">
          <div>
            <label className="form-control-label" htmlFor="password">
              {label}
              {helpMessage}
            </label>
          </div>
          <div className="mb-2">
            <Button
              onClick={() => setShowPassword(!showPassword)}
              variant="link"
              className="btn-xs small text-muted text-underline--dashed"
            >
              {showPassword ? 'Hide password' : 'Show password'}
            </Button>
          </div>
        </div>
        )}
    />
  );
}
