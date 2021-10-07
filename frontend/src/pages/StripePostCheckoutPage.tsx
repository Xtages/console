import React, {useEffect} from 'react';
import {useQueryParams} from 'hooks/useQueryParams';
import {useHistory} from 'react-router-dom';
import {useIsMutating, useMutation} from 'react-query';
import {checkoutApi} from 'service/Services';

const MUTATION_KEY = 'recordCheckoutOutcome';

export default function StripePostCheckoutPage() {
  const queryParams = useQueryParams();
  const isMutating = useIsMutating({mutationKey: MUTATION_KEY});
  const history = useHistory();

  const {
    isIdle,
    mutate,
  } = useMutation(() => checkoutApi.recordCheckoutOutcome({
    checkoutSessionId: queryParams.get('checkoutSessionId')!,
  }), {
    mutationKey: MUTATION_KEY,
    retry: false,
    onSuccess: () => {
      history.push('/');
    },
  });

  useEffect(() => {
    if (!isMutating && isIdle) {
      mutate();
    }
  }, []);

  return (<></>);
}
