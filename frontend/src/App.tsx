import React from 'react';
import {BrowserRouter, Switch} from 'react-router-dom';
import './App.css';
import AuthdRoute from './components/authn/AuthdRoute';
import UnauthdRoute from './components/authn/UnauthdRoute';
import {ProvideAuth} from './hooks/useAuth';
import LoginPage from './pages/authn/LoginPage';
import SignUpPage from './pages/authn/SignUpPage';
import HomePage from './pages/home/HomePage';

export default function App() {
  return (
    <ProvideAuth>
      <BrowserRouter>
        <Switch>
          <UnauthdRoute path="/login" component={LoginPage} />
          <UnauthdRoute path="/signup">
            <SignUpPage />
          </UnauthdRoute>
          <AuthdRoute path="/">
            <HomePage />
          </AuthdRoute>
        </Switch>
      </BrowserRouter>
    </ProvideAuth>
  );
}
