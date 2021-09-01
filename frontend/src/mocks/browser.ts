import {setupWorker} from 'msw';
import {handlers} from './handlers';

// This configures a Service Worker with the given request handlers.
// ***** DO NOT IMPORT THIS FROM A NON-DEV ENVIRONMENT *****
export const worker = setupWorker(...handlers);
