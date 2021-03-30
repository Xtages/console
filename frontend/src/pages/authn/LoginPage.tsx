import React, {FormEvent, useRef} from 'react';
import {Key, User} from 'react-feather';
import {useAuth} from '../../hooks/useAuth';

export default function LoginPage() {
  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const auth = useAuth()!;

  async function signIn(event: FormEvent) {
    event.preventDefault();
    console.log('signIn');
    try {
      const email = emailRef.current;
      const password = passwordRef.current;
      if (email !== null && password !== null) {
        const principal = await auth.signIn(email.value, password.value);
        console.log(principal);
        console.log(auth.principal);
      }
    } catch (error) {
      console.log('error signing in', error);
    }
  }

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-6 col-lg-5 col-xl-4 py-6 py-md-0">
            <div>
              <div className="mb-5 text-center">
                <h6 className="h3 mb-1">Login</h6>
                <p className="text-muted mb-0">
                  Sign in to your account to continue.
                </p>
              </div>
              <span className="clearfix" />
              <form onSubmit={signIn}>
                <div className="form-group">
                  <label className="form-control-label" htmlFor="input-email">Email address</label>
                  <div className="input-group">
                    <div className="input-group-prepend">
                      <span className="input-group-text">
                        <User size="1em" />
                      </span>
                    </div>
                    <input
                      type="email"
                      className="form-control"
                      name="input-email"
                      id="input-email"
                      placeholder="name@example.com"
                      ref={emailRef}
                    />
                  </div>
                </div>
                <div className="form-group mb-0">
                  <div className="d-flex align-items-center justify-content-between">
                    <div>
                      <label className="form-control-label" htmlFor="input-password">Password</label>
                    </div>
                  </div>
                  <div className="input-group">
                    <div className="input-group-prepend">
                      <span className="input-group-text">
                        <Key size="1em" />
                      </span>
                    </div>
                    <input
                      type="password"
                      className="form-control"
                      id="input-password"
                      name="input-password"
                      placeholder="Password"
                      ref={passwordRef}
                    />
                  </div>
                </div>
                <div className="mt-4">
                  <button type="submit" className="btn btn-block btn-primary">
                    Sign in
                  </button>
                </div>
              </form>
              <div className="mt-4 text-center">
                <small>Not registered?</small>
                {' '}
                <a
                  href="register-basic.html"
                  className="small font-weight-bold"
                >
                  Create account
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
