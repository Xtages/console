import useLocalStorage from 'use-local-storage';
import {useLocation} from 'react-router-dom';
import {useEffect} from 'react';

/**
 * Hook to retrieve the priceId used in the sign-up process.
 *
 * The priceId if available from a `priceId` URL query param will be stored in `localStorage`, if
 * there's no `priceId` in the URL query params then the value stored in `localStorage` will be
 * returned or `null` if the value is not available.
 *
 * This hook returns both the `priceId` (which could be `null`) and a `clearPriceId` function that
 * can be called to remove it from `localStorage`.
 */
export function usePriceId() {
  const [priceId, setPriceId] = useLocalStorage<string | null>('xtages.priceId');

  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const priceIdParam = searchParams.get('priceId');

  if (priceIdParam !== priceId && priceIdParam) {
    setPriceId(priceIdParam);
  }

  function clearPriceId() {
    useEffect(() => {
      setPriceId(null);
      return () => {};
    }, [priceId]);
  }

  return {
    priceId,
    clearPriceId,
  };
}
