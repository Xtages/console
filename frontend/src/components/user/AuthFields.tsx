import {AtSign, HelpCircle, Key} from 'react-feather';
import React, {useState} from 'react';
import LabeledFormField, {LabeledFormFieldProps} from 'components/form/LabeledFormField';
import {Button, OverlayTrigger, Tooltip} from 'react-bootstrap';

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
  showHelpTooltip = false,
  placeholder = '********',
  ...props
}: {
  label?: string,
  name?: string,
  showHelpTooltip?: boolean,
} & Partial<LabeledFormFieldProps>) {
  const [showPassword, setShowPassword] = useState(false);

  let helpTooltipFragment;
  if (showHelpTooltip) {
    helpTooltipFragment = (
      <OverlayTrigger
        overlay={(
          <Tooltip id="passwordHelpTooltip">
            <div className="text-left pt-1 pl-1">
              Your password must:
              <ul className="pl-4">
                <li>be at least 8 characters long</li>
                <li>contain an uppercase letter</li>
                <li>contain a lowercase letter</li>
                <li>contain a number</li>
                <li>contain a special character</li>
              </ul>
            </div>
          </Tooltip>
            )}
      >
        <HelpCircle height="1em" />
      </OverlayTrigger>
    );
  }

  return (
    <LabeledFormField
      {...
        props
    }
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
              {helpTooltipFragment}
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
