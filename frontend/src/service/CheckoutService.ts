import {loadStripe} from '@stripe/stripe-js';
import {checkoutApi} from './Services';

export default async function redirectToStripeCheckoutSession(
  {
    priceIds,
    organizationName,
  }:
  {
    priceIds: string[];
    organizationName: string;
  },
) {
  const response = await checkoutApi.createCheckoutSession({
    priceIds,
    organizationName,
  });
  const stripeSessionId = response.data;
  const stripe = await loadStripe(process.env.REACT_APP_STRIPE_PUBLISHABLE_KEY!);
  stripe?.redirectToCheckout({sessionId: stripeSessionId});
}
