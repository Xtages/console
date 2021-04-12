import {AtSign, Key} from 'react-feather';
import {Field} from 'formik';
import React from 'react';

/**
 * Email `formik` field.
 */
export function EmailField() {
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
export function PasswordField() {
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
      </div>
      <div className="input-group input-group-merge">
        <div className="input-group-prepend">
          <span className="input-group-text">
            <Key size="1em" />
          </span>
        </div>
        <Field
          type="password"
          className="form-control form-control-prepend"
          id="password"
          name="password"
          placeholder="********"
        />
      </div>
    </div>
  );
}
