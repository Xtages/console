import {useTracking} from 'hooks/useTracking';

type Metadata = {
  startTime: number;
  method: string;
};

type XMLHttpRequestWithMetadata = {
  metadata: Metadata;
} & XMLHttpRequest;

function buildEventPayload(req: XMLHttpRequestWithMetadata) {
  const endTime = performance.now();
  const elapsed = endTime - req.metadata.startTime;
  const payload: Record<string, any> = {
    method: req.metadata.method,
    status: req.status,
    startTime: req.metadata.startTime,
    endTime,
    elapsed,
  };
  if (req.status > 400) {
    payload.errorMsg = req.responseText;
  }
  return payload;
}

function shouldTrack(url: string) {
  return url.indexOf('segment.io') < 0;
}

export function useXmlHttpRequestInstrumentation() {
  const {
    trackApiEvent,
  } = useTracking();

  const originalOpen = XMLHttpRequest.prototype.open;
  function interceptedOpen(
    this: XMLHttpRequest,
    method: string,
    url: string,
    async?: boolean,
    username?: string | null,
    password?: string | null,
  ): void {
    const reqWithMetadata = this as XMLHttpRequestWithMetadata;
    reqWithMetadata.metadata = {
      startTime: performance.now(),
      method,
    };
    this.addEventListener('loadend', () => {
      const req = this as XMLHttpRequestWithMetadata;
      if (shouldTrack(req.responseURL)) {
        if (req.status < 400) {
          trackApiEvent(req.responseURL, buildEventPayload(req));
        } else {
          trackApiEvent(req.responseURL, buildEventPayload(req));
        }
      }
    });
    originalOpen.call(this, method, url, async || false, username, password);
  }
  XMLHttpRequest.prototype.open = interceptedOpen;
}
