import React from 'react';
import {BrowserRouter, Link, Route, Switch} from 'react-router-dom';
import './App.css';
import PrivateRoute from './components/PrivateRoute';
import {ProvideAuth} from './hooks/useAuth';
import LoginPage from './pages/authn/LoginPage';
import SignUpPage from './pages/authn/SignUpPage';
import HomePage from './pages/home/HomePage';

export default function App() {
  return (
    <ProvideAuth>
      <BrowserRouter>
        <div>
          <ul>
            <li>
              <Link to="/login">Login</Link>
            </li>
            <li>
              <Link to="/signup">Sign Up</Link>
            </li>
            <li>
              <Link to="/">Home</Link>
            </li>
          </ul>

          <Switch>
            <Route path="/login">
              <LoginPage />
            </Route>
            <Route path="/signup">
              <SignUpPage />
            </Route>
            <PrivateRoute path="/">
              <HomePage />
            </PrivateRoute>
          </Switch>
        </div>
      </BrowserRouter>
    </ProvideAuth>
  );
}
