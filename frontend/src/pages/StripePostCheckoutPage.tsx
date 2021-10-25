import React, {useEffect} from 'react';
import {useQueryParams} from 'hooks/useQueryParams';
import {useHistory} from 'react-router-dom';
import {useIsMutating, useMutation} from 'react-query';
import {checkoutApi} from 'service/Services';
import {useTracking} from 'hooks/useTracking';

const MUTATION_KEY = 'recordCheckoutOutcome';

export default function StripePostCheckoutPage() {
  const queryParams = useQueryParams();
  const isMutating = useIsMutating({mutationKey: MUTATION_KEY});
  const history = useHistory();
  const {trackComponentEvent, trackComponentApiError} = useTracking();

  const {
    isIdle,
    mutate,
  } = useMutation(() => checkoutApi.recordCheckoutOutcome({
    checkoutSessionId: queryParams.get('checkoutSessionId')!,
  }), {
    mutationKey: MUTATION_KEY,
    retry: false,
    onSuccess: () => {
      trackComponentEvent('StripePostCheckoutPage', 'Checkout recorded');
      history.push('/');
    },
    onError: (error) => {
      trackComponentApiError('StripePostCheckoutPage', 'checkoutApi.recordCheckoutOutcome', error);
    },
  });

  useEffect(() => {
    if (!isMutating && isIdle) {
      trackComponentEvent('StripePostCheckoutPage', 'Checkout succeeded');
      mutate();
    }
  }, []);

  return (<></>);
}
