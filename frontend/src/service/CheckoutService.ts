import {loadStripe} from '@stripe/stripe-js';
import {CreateCheckoutSessionReq} from 'gen/api';
import {checkoutApi} from './Services';

export default async function redirectToStripeCheckoutSession(req: CreateCheckoutSessionReq) {
  const response = await checkoutApi.createCheckoutSession(req);
  const stripeSessionId = response.data;
  const stripe = await loadStripe(process.env.REACT_APP_STRIPE_PUBLISHABLE_KEY!);
  stripe?.redirectToCheckout({sessionId: stripeSessionId});
}
