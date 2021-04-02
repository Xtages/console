import {Auth} from 'aws-amplify';
import React, {useState} from 'react';
import LogoutButton from '../../components/authn/LogoutButton';

export default function HomePage() {
  const [message, setMessage] = useState('');
  const [message2, setMessage2] = useState('');

  async function getHi() {
    const session = await Auth.currentSession();
    const token = session.getIdToken().getJwtToken();
    const resp = await fetch('/api/hello', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: 'no-store',
    });
    setMessage(await resp.text());
  }
  async function getHiNoToken() {
    try {
      const resp = await fetch('/api/hello', {
        cache: 'no-store',
      });
      setMessage2(await resp.text() + resp.statusText);
    } catch (e) {
      console.log(e);
      setMessage2(`${e}`);
    }
  }

  return (
    <>
      <div>Welcome to Xtages</div>
      <button type="button" onClick={getHi}>Say Hi!</button>
      { message && <h1>{message}</h1>}
      <button type="button" onClick={getHiNoToken}>Say Hi (No Token)!</button>
      { message2 && <h1>{message2}</h1>}
      <LogoutButton />
    </>
  );
}
