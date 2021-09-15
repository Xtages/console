import {useAnalytics} from 'use-analytics';
import {Principal} from 'hooks/useAuth';
import * as Sentry from '@sentry/react';

type ErrorType = 'formValidation' | 'apiCall';

const noop = () => null;

function urlSearchParamsToMap(searchParams: URLSearchParams) {
  const result: Record<string, any> = {};
  const keyIt = searchParams.keys();
  let key = keyIt.next();
  while (!key.done) {
    let value: string | string[] = searchParams.getAll(key.value);
    if (value.length === 1) {
      // eslint-disable-next-line prefer-destructuring
      value = value[0];
    }
    result[key.value] = value;
    key = keyIt.next();
  }
  return result;
}

export function useTracking() {
  const {
    identify,
    track,
    reset: analyticsReset,
  } = useAnalytics();

  function tryParseUrl(url: string) {
    try {
      return new URL(url);
    } catch (e) {
      Sentry.captureException(e, {extra: {url}});
    }
    return null;
  }

  function trackEvent(eventName: string, payload?: any) {
    track(eventName, {
      __location: window.location.href,
      ...payload,
    }).then(noop);
  }

  function trackApiEvent(url: string, payload?: any) {
    const u = tryParseUrl(url);
    if (u !== null) {
      let eventName = 'API Called';
      if (u.host !== new URL(window.location.href).host) {
        eventName = 'External API Called';
      }
      const event = {
        schema: u.protocol.endsWith(':') ? u.protocol.substr(0, u.protocol.length - 1) : u.protocol,
        hostname: u.hostname,
        port: u.port,
        path: u.pathname,
        search: urlSearchParamsToMap(u.searchParams),
        ...payload,
      };
      trackEvent(eventName, event);
      if (payload.status !== undefined && payload.status >= 400) {
        Sentry.captureException(event);
      }
    }
  }

  function trackComponentEvent(component: string, eventName: string, payload?: any) {
    trackEvent(eventName, {
      component,
      ...payload,
    });
  }

  function trackComponentError(component: string, errorType: ErrorType, payload?: any) {
    trackComponentEvent(component, `${component} Error`, {
      errorType,
      ...payload,
    });
  }

  function trackComponentApiError(component: string, apiName: string, error: any, payload?: any) {
    trackComponentError(component, 'apiCall', {
      apiName,
      error,
      ...payload,
    });
  }

  function identifyPrincipal(principal: Principal) {
    identify(principal.id, {
      org: principal.org,
    }).then(noop);
    Sentry.configureScope((scope) => {
      scope.setUser({
        id: principal.id,
      });
      scope.setTags({org: principal.org});
    });
  }

  function reset() {
    analyticsReset().then(noop);
    Sentry.configureScope((scope) => {
      scope.clear();
    });
  }

  return {
    trackComponentEvent,
    trackComponentError,
    trackComponentApiError,
    trackApiEvent,
    identifyPrincipal,
    reset,
  };
}
