import {AtSign, Key} from 'react-feather';
import {Field} from 'formik';
import React, {memo, useState} from 'react';
import styles from './AuthFields.module.scss';

/**
 * Email `formik` field.
 */
function EmailField() {
  return (
    <div className="form-group">
      <label className="form-control-label" htmlFor="email">
        Email address
      </label>
      <div className="input-group input-group-merge">
        <div className="input-group-prepend">
          <span className="input-group-text">
            <AtSign size="1em" />
          </span>
        </div>
        <Field
          type="email"
          className="form-control form-control-prepend"
          id="email"
          name="email"
          placeholder="santa@northpole.com"
        />
      </div>
    </div>
  );
}

/**
 * Password `formik` field.
 */
function PasswordField({placeholder = '********'}: {placeholder?: string} = {}) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="form-group mb-4">
      <div
        className="d-flex align-items-center justify-content-between"
      >
        <div>
          <label
            className="form-control-label"
            htmlFor="password"
          >
            Password
          </label>
        </div>
        <div className="mb-2">
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className={`small text-muted text-underline--dashed border-primary ${styles.showPasswordButton}`}
          >
            {showPassword ? 'Hide password' : 'Show password'}
          </button>
        </div>
      </div>
      <div className="input-group input-group-merge">
        <div className="input-group-prepend">
          <span className="input-group-text">
            <Key size="1em" />
          </span>
        </div>
        <Field
          type={showPassword ? 'text' : 'password'}
          className="form-control form-control-prepend"
          id="password"
          name="password"
          placeholder={placeholder}
        />
      </div>
    </div>
  );
}

const memoizedEmailField = memo(EmailField);

export {memoizedEmailField as EmailField, PasswordField};
