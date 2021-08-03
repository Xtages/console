import {useLocation} from 'react-router-dom';
import {useEffect} from 'react';
import {buildAnalytics} from 'service/AnalyticsService';

export function usePageViews() {
  const location = useLocation();
  const a = buildAnalytics();
  useEffect(() => {
    a.page({
      ...location,
    }).then(() => null);
  }, [location]);
}
