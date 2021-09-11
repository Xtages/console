import {useTracking} from 'hooks/useTracking';

async function buildEventPayload(
  startTime: number, input: RequestInfo, response: Response | null, error?: any,
) {
  const endTime = performance.now();
  const elapsed = endTime - startTime;
  const clonedResponse = response?.clone();
  const payload: Record<string, any> = {
    method: getMethod(input),
    status: clonedResponse?.status,
    startTime,
    endTime,
    elapsed,
  };
  if (error || (clonedResponse && clonedResponse.status >= 400)) {
    payload.errorMsg = await clonedResponse?.text();
    payload.error = error;
  }
  return payload;
}

function getUrl(input: RequestInfo) {
  if (typeof input === 'string') {
    return input;
  }
  return input.url;
}

function getMethod(input: RequestInfo) {
  if (typeof input === 'string') {
    return 'GET';
  }
  return input.method;
}

function shouldTrack(input: RequestInfo) {
  const url = getUrl(input);
  return !url.includes('segment.io')
      && !url.includes('segment.com')
      && !url.includes('sentry.io');
}

export function useFetchInstrumentation() {
  const {
    trackApiEvent,
  } = useTracking();

  const {fetch: originalFetch} = window;

  async function interceptedFetch(input: RequestInfo, init?: RequestInit) {
    if (shouldTrack(input)) {
      const startTime = performance.now();
      try {
        const response = await originalFetch(input, init);
        trackApiEvent(getUrl(input), await buildEventPayload(startTime, input, response));
        return response;
      } catch (e) {
        trackApiEvent(getUrl(input), await buildEventPayload(startTime, input, null, e));
        throw e;
      }
    }
    return originalFetch(input, init);
  }

  window.fetch = interceptedFetch;
}
