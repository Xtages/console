import {useEffect} from 'react';

export function useSurvicate() {
  useEffect(() => {
    const s = document.createElement('script');
    s.src = 'https://survey.survicate.com/workspaces/e853b864189138b4aab47038d6f8a7f8/web_surveys.js';
    s.async = true;
    const e = document.getElementsByTagName('script')[0];
    e.parentNode!.insertBefore(s, e);
  }, []);
}
