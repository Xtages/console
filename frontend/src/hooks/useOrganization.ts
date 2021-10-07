import {useQuery} from 'react-query';
import {organizationApi} from 'service/Services';
import Axios from 'axios';

/**
 * Get's the users current {@link Organization}.
 */
export function useOrganization() {
  const result = useQuery('org', () => organizationApi.getOrganization());
  const {error, data} = result;
  const orgNotFound = typeof error !== 'undefined' && error !== null && Axios.isAxiosError(error) && error.response?.status === 404;
  return {
    ...result,
    organization: data?.data,
    orgNotFound,
  };
}
