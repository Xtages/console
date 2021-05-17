import {AtSign, HelpCircle, Key} from 'react-feather';
import React, {useState} from 'react';
import ReactTooltip from 'react-tooltip';
import LabeledFormField, {LabeledFormFieldProps} from 'components/form/LabeledFormField';
import {Button} from 'components/button/Buttons';

/**
 * Email `formik` field.
 */
export function EmailField(props : Partial<LabeledFormFieldProps>) {
  return (
    <LabeledFormField
      {...props}
      type="email"
      name="email"
      label="Email address"
      placeholder="santa@northpole.com"
      icon={<AtSign size="1em" />}
    />
  );
}

/**
 * Password `formik` field.
 */
export function PasswordField({
  showHelpTooltip = false,
  placeholder = '********',
  ...props
}: {
  showHelpTooltip?: boolean,
} & Partial<LabeledFormFieldProps>) {
  const [showPassword, setShowPassword] = useState(false);

  let helpTooltipFragment;
  if (showHelpTooltip) {
    helpTooltipFragment = (
      <>
        <HelpCircle height="1em" data-tip data-for="passwordHelpTooltip" />
        <ReactTooltip id="passwordHelpTooltip" place="right" effect="solid">
          Your password must:
          <ul>
            <li>be at least 8 characters long</li>
            <li>contain an uppercase letter</li>
            <li>contain a lowercase letter</li>
            <li>contain a number</li>
            <li>contain a special character</li>
          </ul>
        </ReactTooltip>
      </>
    );
  }

  return (
    <LabeledFormField
      {...props}
      type={showPassword ? 'text' : 'password'}
      name="password"
      placeholder={placeholder}
      icon={<Key size="1em" />}
      label={(
        <div className="d-flex align-items-center justify-content-between">
          <div>
            <label className="form-control-label" htmlFor="password">
              Password
              {helpTooltipFragment}
            </label>
          </div>
          <div className="mb-2">
            <Button
              onClick={() => setShowPassword(!showPassword)}
              size="xs"
              className="small text-muted text-underline--dashed"
              asLink
            >
              {showPassword ? 'Hide password' : 'Show password'}
            </Button>
          </div>
        </div>
    )}
    />
  );
}
