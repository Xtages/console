declare module 'use-analytics' {
  import {AnalyticsInstance} from 'analytics';

  type AnalyticsProviderProps = {
    children: JSX.Element;
    instance: AnalyticsInstance;
  };

  export function AnalyticsProvider(props: AnalyticsProviderProps): JSX.Element;

  export function useAnalytics() : AnalyticsInstance;
}
