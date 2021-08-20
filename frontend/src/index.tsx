import React from 'react';
import ReactDOM from 'react-dom';
import 'assets/css/theme.scss';
import App from 'App';
import reportWebVitals from 'reportWebVitals';
import {trackWebVitals} from 'service/AnalyticsService';
import * as Sentry from '@sentry/react';
import {Integrations} from '@sentry/tracing';

Sentry.init({
  release: process.env.REACT_APP_RELEASE_TAG,
  dsn: 'https://753b9abf1f6240ffbe2f2a7050cd2512@o966978.ingest.sentry.io/5918024',
  integrations: [new Integrations.BrowserTracing()],
  tracesSampleRate: 1.0,
  beforeSend(event, ignore_hint) {
    // Check if it is an exception, and if so, show the report dialog
    if (event.exception) {
      Sentry.showReportDialog({eventId: event.event_id});
    }
    return event;
  },
});

ReactDOM.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
  document.getElementById('root'),
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals(trackWebVitals);
