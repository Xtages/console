import Analytics, {AnalyticsInstance} from 'analytics';
// @ts-ignore
import segmentPlugin from '@analytics/segment';
import {Metric} from 'web-vitals';

let analytics: AnalyticsInstance | null = null;

export function buildAnalytics() {
  if (!analytics) {
    const plugins = [];
    if (process.env.NODE_ENV === 'production') {
      plugins.push(
        segmentPlugin({
          writeKey: 'LN5YGxvF3N2yy2MhMzHX7WTa8URmXWGe',
        }),
      );
    } else {
      plugins.push({
        name: 'analytics-console-plugin',
        // eslint-disable-next-line no-console
        page: (event: any) => console.debug(event.payload),
        // eslint-disable-next-line no-console
        track: (event: any) => console.debug(event.payload),
        // eslint-disable-next-line no-console
        identify: (event: any) => console.debug(event.payload),
        loaded: () => true,
      });
    }
    analytics = Analytics({
      app: 'Console',
      debug: process.env.NODE_ENV === 'development',
      plugins,
    });
  }
  return analytics;
}

export function trackWebVitals(metric: Metric) {
  const a = buildAnalytics();
  a.track('Web Vitals', {
    id: metric.id,
    name: metric.name,
    value: metric.value,
    delta: metric.delta,
  }).then(() => null);
}
