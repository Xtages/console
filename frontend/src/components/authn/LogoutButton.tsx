import React, {MouseEvent} from 'react';
import {LogOut} from 'react-feather';
import {useHistory} from 'react-router-dom';
import {useAuth} from '../../hooks/useAuth';

/** A button to log the Principal out. */
export default function LogoutButton() {
  const auth = useAuth();
  const history = useHistory();

  async function logOut(event: MouseEvent) {
    event.preventDefault();
    await auth.logOut();
    history.push('/login');
  }

  return (
    <button
      type="button"
      onClick={logOut}
      className="btn btn-block btn-sm btn-neutral btn-icon rounded-pill"
    >
      <LogOut />
      {' '}
      <span className="btn-inner--text">Sign out</span>
    </button>
  );
}
