import {Link} from 'react-router-dom';
import React from 'react';

/**
 * A component that will gender a link to prompt the user to sign up.
 */
export default function CreateAccountLink() {
  return (
    <div className="mt-4 text-center">
      <small>Not registered?</small>
      {' '}
      <Link to="/signup" className="small font-weight-bold">
        Create account
      </Link>
    </div>
  );
}
