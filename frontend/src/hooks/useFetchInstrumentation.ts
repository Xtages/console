import {useTracking} from 'hooks/useTracking';

async function buildEventPayload(
  startTime: number, input: RequestInfo, response: Response | null, error?: any,
) {
  const endTime = performance.now();
  const elapsed = endTime - startTime;
  const payload: Record<string, any> = {
    method: getMethod(input),
    status: response?.status,
    startTime,
    endTime,
    elapsed,
  };
  if (error || (response && response.status > 400)) {
    payload.errorMsg = await response?.text();
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
  return url.indexOf('segment.io') < 0;
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
        if (response.status < 400) {
          trackApiEvent(
            getUrl(input), await buildEventPayload(startTime, input, response),
          );
        } else {
          trackApiEvent(
            getUrl(input), await buildEventPayload(startTime, input, response),
          );
        }
        return response;
      } catch (e) {
        trackApiEvent(
          getUrl(input), await buildEventPayload(startTime, input, null, e),
        );
        throw e;
      }
    }
    return originalFetch(input, init);
  }

  window.fetch = interceptedFetch;
}
