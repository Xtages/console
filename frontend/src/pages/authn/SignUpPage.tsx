import React, {FormEvent, useRef} from 'react';
import {AtSign, Key, User} from 'react-feather';
import {useAuth} from '../../hooks/useAuth';

export default function SignUpPage() {
  const nameRef = useRef<HTMLInputElement>(null);
  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const auth = useAuth()!;

  async function signUp(event: FormEvent) {
    event.preventDefault();
    try {
      const name = nameRef.current;
      const email = emailRef.current;
      const password = passwordRef.current;
      if (name != null && email != null && password != null) {
        await auth.signUp(email.value, password.value, name.value, 'xtages');
        console.log('sign up', auth.principal);
      }
    } catch (error) {
      console.log(error);
    }
  }

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-8 col-lg-5 py-6">
            <div>
              <div className="mb-5 text-center">
                <h6 className="h3 mb-1">Create your account</h6>
                <p className="text-muted mb-0">
                  Made with love for designers &amp; developers.
                </p>
              </div>
              <span className="clearfix"></span>
              <form onSubmit={signUp}>
                <div className="form-group">
                  <label className="form-control-label">Name</label>
                  <div className="input-group input-group-merge">
                    <div className="input-group-prepend">
                      <span className="input-group-text">
                        <User size="1em" />
                      </span>
                    </div>
                    <input
                      type="text"
                      className="form-control form-control-prepend"
                      id="input-name"
                      placeholder="Santa Claus"
                      ref={nameRef}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-control-label">Email address</label>
                  <div className="input-group input-group-merge">
                    <div className="input-group-prepend">
                      <span className="input-group-text">
                        <AtSign size="1em" />
                      </span>
                    </div>
                    <input
                      type="email"
                      className="form-control form-control-prepend"
                      id="input-email"
                      placeholder="santa@northpole.com"
                      ref={emailRef}
                    />
                  </div>
                </div>
                <div className="form-group mb-4">
                  <div className="d-flex align-items-center justify-content-between">
                    <div>
                      <label className="form-control-label">Password</label>
                    </div>
                  </div>
                  <div className="input-group input-group-merge">
                    <div className="input-group-prepend">
                      <span className="input-group-text">
                        <Key size="1em" />
                      </span>
                    </div>
                    <input
                      type="password"
                      className="form-control form-control-prepend"
                      id="input-password"
                      placeholder="********"
                      ref={passwordRef}
                    />
                  </div>
                </div>
                <div className="my-4">
                  <div className="custom-control custom-checkbox mb-3">
                    <input
                      type="checkbox"
                      className="custom-control-input"
                      id="check-terms"
                    />{' '}
                    <label
                      className="custom-control-label"
                      htmlFor="check-terms"
                    >
                      I agree to the{' '}
                      <a href="../../pages/utility/terms.html">
                        terms and conditions
                      </a>
                    </label>
                  </div>
                </div>
                <div className="mt-4">
                  <button type="submit" className="btn btn-block btn-primary">
                    Create my account
                  </button>
                </div>
              </form>
              <div className="mt-4 text-center">
                <small>Already have an acocunt?</small>{' '}
                <a href="login-basic.html" className="small font-weight-bold">
                  Sign in
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
