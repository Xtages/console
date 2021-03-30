import React from 'react';
import './App.css';
// import SignUpPage from './pages/authflow/SignUpPage';
import {Auth} from 'aws-amplify';
import LoginPage from './pages/authn/LoginPage';
import {ProvideAuth} from './hooks/useAuth';

Auth.configure({
  aws_project_region: 'us-east-1',
  userPoolId: 'us-east-1_F4FzoEObF',
  userPoolWebClientId: '4iilttvg5aqlnnujs088tisjl',
});

export default function App() {
  return (
    <ProvideAuth>
      <LoginPage />
      {/* <SignUpPage />; */}
    </ProvideAuth>
  );
}
