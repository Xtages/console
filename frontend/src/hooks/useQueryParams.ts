import {useLocation} from 'react-router-dom';

/**
 * A hook to get the current location's query parameters.
 * @return {@link URLSearchParams} for the current location.
 */
export function useQueryParams() {
  return new URLSearchParams(useLocation().search);
}
