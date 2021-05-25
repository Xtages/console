import React, {MouseEvent} from 'react';
import {LogOut} from 'react-feather';
import {useHistory} from 'react-router-dom';
import {useAuth} from 'hooks/useAuth';
import {Button} from '../button/Buttons';

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
    <Button
      type="button"
      kind="white"
      asLink
      size="xs"
      onClick={logOut}
      className="text-muted"
    >
      <LogOut size="1.3em" />
      {' '}
      Sign out
    </Button>
  );
}
