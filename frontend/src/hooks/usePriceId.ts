import useLocalStorage from 'use-local-storage';
import {useEffect} from 'react';
import {useQueryParams} from 'hooks/useQueryParams';

const NO_PRICE_ID_SET = 'NO_VALUE';

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
  const [priceId, setPriceId] = useLocalStorage<string>('xtages.priceId', NO_PRICE_ID_SET);

  const queryParams = useQueryParams();
  const priceIdParam = queryParams.get('priceId');

  if (priceIdParam !== priceId && priceIdParam !== null) {
    setPriceId(priceIdParam);
  }

  function clearPriceId() {
    useEffect(() => {
      setPriceId(NO_PRICE_ID_SET);
      return () => {};
    }, [priceId]);
  }

  return {
    priceId: priceId === NO_PRICE_ID_SET ? null : priceId,
    clearPriceId,
  };
}
