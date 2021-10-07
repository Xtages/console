import React, {ReactNode, MouseEvent, useState} from 'react';
import {Badge,
  Button,
  Card,
  Col,
  Container,
  Row,
  ToggleButton,
  ToggleButtonGroup} from 'react-bootstrap';
import cx from 'classnames';
import {BillingCycle, PlanType} from 'types/planTypes';
import {SelectedPlanContext, useSelectedPlan} from 'hooks/useSelectedPlan';
import {Nullable} from 'types/nullable';
import {useTracking} from 'hooks/useTracking';
import redirectToStripeCheckoutSession from 'service/CheckoutService';
import styles from './PlanSelector.module.scss';

type SkuDetails = {
  sku: string;
  priceId: string;
  price: string;
  billingInfo?: string | null;
  callToAction: string;
  title: string;
  subtitle?: Nullable<string>;
};
type Plan = Record<BillingCycle, SkuDetails>;
type Plans = Record<PlanType, Plan>;

let plans: Plans = {
  trial: {
    monthly: {
      priceId: 'price_1JF4NqIfxICi4AQgRTjmOWNf',
      price: '0',
      sku: 'trial-monthly',
      callToAction: 'Start trial',
      title: 'Trial',
      subtitle: 'upgraded to Starter after 14 days',
    },
    yearly: {
      priceId: 'price_1JF4NxIfxICi4AQgoeBWQ46u',
      price: '0',
      sku: 'trial-yearly',
      callToAction: 'Start trial',
      title: 'Trial',
      subtitle: 'upgraded to Starter after 14 days',
    },
  },
  starter: {
    monthly: {
      priceId: 'price_1JF4NqIfxICi4AQgRTjmOWNf',
      price: '229',
      sku: 'starter-monthly',
      billingInfo: 'per month',
      callToAction: 'Purchase Starter',
      title: 'Starter',
    },
    yearly: {
      priceId: 'price_1JF4NxIfxICi4AQgoeBWQ46u',
      price: '199',
      sku: 'starter-yearly',
      billingInfo: 'per month, paid yearly',
      callToAction: 'Purchase Starter',
      title: 'Starter',
    },
  },
  pro: {
    monthly: {
      priceId: 'price_1JF4MZIfxICi4AQgU4RtfIBc',
      price: '585',
      sku: 'pro-monthly',
      billingInfo: 'per month',
      callToAction: 'Purchase Pro',
      title: 'Professional',
    },
    yearly: {
      priceId: 'price_1JF4NiIfxICi4AQg9EsBpsl5',
      price: '509',
      sku: 'pro-yearly',
      billingInfo: 'per month, paid yearly',
      callToAction: 'Purchase Pro',
      title: 'Professional',
    },
  },
};

if (process.env.NODE_ENV !== 'production') {
  plans = {
    trial: {
      monthly: {
        priceId: 'price_1J7pjXIfxICi4AQgDarGp3Xp',
        price: '0',
        sku: 'trial-monthly',
        callToAction: 'Start trial',
        title: 'Trial',
        subtitle: 'upgraded to Starter after 14 days',
      },
      yearly: {
        priceId: 'price_1J7pfYIfxICi4AQgGUYHG2lM',
        price: '0',
        sku: 'trial-yearly',
        callToAction: 'Start trial',
        title: 'Trial',
        subtitle: 'upgraded to Starter after 14 days',
      },
    },
    starter: {
      monthly: {
        priceId: 'price_1J7pjXIfxICi4AQgDarGp3Xp',
        price: '229',
        sku: 'starter-monthly',
        billingInfo: 'per month',
        callToAction: 'Purchase Starter',
        title: 'Starter',
      },
      yearly: {
        priceId: 'price_1J7pfYIfxICi4AQgGUYHG2lM',
        price: '199',
        sku: 'starter-yearly',
        billingInfo: 'per month, paid yearly',
        callToAction: 'Purchase Starter',
        title: 'Starter',
      },
    },
    pro: {
      monthly: {
        priceId: 'price_1J7plxIfxICi4AQgd7XO8FA2',
        price: '585',
        sku: 'pro-monthly',
        billingInfo: 'per month',
        callToAction: 'Purchase Pro',
        title: 'Professional',
      },
      yearly: {
        priceId: 'price_1J7pkVIfxICi4AQgee83lecL',
        price: '509',
        sku: 'pro-yearly',
        billingInfo: 'per month, paid yearly',
        callToAction: 'Purchase Pro',
        title: 'Professional',
      },
    },
  };
}

export type PlanSelectorProps = {
  purchaseEnabled?: boolean;
};

export function PlanSelector({purchaseEnabled = true}: PlanSelectorProps) {
  const [billingCycle, setBillingCycle] = useState<BillingCycle>('yearly');
  const [planType, setPlanType] = useState<PlanType>('pro');
  const {trackComponentEvent} = useTracking();

  function recordEnterprisePlanClick(e: MouseEvent<HTMLAnchorElement>) {
    if (purchaseEnabled) {
      trackComponentEvent('PlanSelector', 'Enterprise plan clicked');
    } else {
      e.preventDefault();
    }
  }

  return (
    <SelectedPlanContext.Provider value={{
      billingCycle,
      setBillingCycle,
      planType,
      setPlanType,
    }}
    >
      <Container className={styles.planSelector}>
        <BillingCycleToggle size="sm" />
        <Row className="mb-3 mt-4 shadow-none">
          <Col lg={3}>
            <PlanCard planType="trial" purchaseEnabled={purchaseEnabled}>
              <ul className="list-unstyled text-sm mb-4">
                <li>Deploy 1 app</li>
                <li>100 CI/CD credits (Linux)</li>
                <li>Deploys to the Xtages Cloud</li>
                <abbr title="Out-of-the-box">OOTB</abbr>
                {' '}
                metrics dashboard
                <li>Log collection</li>
              </ul>
            </PlanCard>
          </Col>
          <Col lg={3}>
            <PlanCard planType="starter" purchaseEnabled={purchaseEnabled}>
              <ul className="list-unstyled text-sm mb-4">
                <li>Deploy up to 2 apps</li>
                <li>2500 CI/CD credits (Linux)</li>
                <li>20 GB of data-transfer</li>
                <li>Deploys to the Xtages Cloud</li>
                <li>
                  <abbr title="Out-of-the-box">OOTB</abbr>
                  {' '}
                  metrics dashboard
                </li>
                <li>Log collection</li>
                <li>Support over email</li>
              </ul>
            </PlanCard>
          </Col>
          <Col lg={3}>
            <PlanCard planType="pro" highlighted purchaseEnabled={purchaseEnabled}>
              <ul className="list-unstyled text-white text-sm opacity-8 mb-4">
                <li>Deploy up to 5 apps</li>
                <li>7500 CI/CD credits (Linux)</li>
                <li>500 GB of data-transfer</li>
                <li>Deploys to the Xtages Cloud</li>
                <li>
                  <abbr title="Out-of-the-box">OOTB</abbr>
                  {' '}
                  metrics dashboard
                </li>
                <li>Log collection</li>
                <li>Support over email (24hr SLA)</li>
              </ul>
            </PlanCard>
          </Col>
          <Col lg={3}>
            <Card className="card-pricing text-center px-3 shadow border-0">
              <div className="card-header border-0 delimiter-bottom">
                <div className="h2 text-center mb-0">Enterprise</div>
                <span className="h6 text-muted">&nbsp;</span>
              </div>
              <Card.Body>
                <p className="my-7 h4">
                  Need more?
                </p>
                <a
                  href="mailto:support@xtages.com"
                  target="_blank"
                  onClick={recordEnterprisePlanClick}
                  className="btn btn-sm btn-warning hover-translate-y-n3 hover-shadow-lg mb-3 noExternalLinkIcon"
                  rel="noreferrer"
                >
                  Contact Us!
                </a>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    </SelectedPlanContext.Provider>
  );
}

type PlanCardProps = {
  planType: PlanType;
  highlighted?: boolean;
  purchaseEnabled?: boolean;
  children: ReactNode | ReactNode[];
};

function PlanCard({
  planType,
  highlighted = false,
  purchaseEnabled = true,
  children,
}: PlanCardProps) {
  const {
    billingCycle,
  } = useSelectedPlan();
  const {subtitle} = plans[planType][billingCycle];
  return (
    <Card className={cx('card-pricing border-0 text-center px-3', {'bg-primary': highlighted})}>
      <Card.Header className="border-0 delimiter-bottom">
        <PriceTitle planType={planType} highlighted={highlighted} />
      </Card.Header>
      <Card.Body>
        {subtitle && (
          <p className={`mb-2 ${styles.subtitle}`}>
            <mark>{subtitle}</mark>
          </p>
        )}
        {children}
        {purchaseEnabled && <PurchaseButton planType={planType} highlighted={highlighted} />}
      </Card.Body>
    </Card>
  );
}

type BillingCycleToggleProps = {
  size?: 'sm' | 'lg';
};

function BillingCycleToggle({size}: BillingCycleToggleProps) {
  return (
    <SelectedPlanContext.Consumer>
      {({
        billingCycle,
        setBillingCycle,
      }) => (
        <Row className={`text-center mb-1 ${styles.billingCycleToggle}`}>
          <Col>
            <ToggleButtonGroup
              name="billing"
              type="radio"
              value={billingCycle}
              aria-label="Billing cycle"
              onChange={setBillingCycle}
              size={size}
            >
              <ToggleButton
                id="monthly"
                variant={billingCycle === 'monthly' ? 'primary' : 'soft-primary'}
                value="monthly"
              >
                Monthly
              </ToggleButton>
              <ToggleButton
                id="yearly"
                variant={billingCycle === 'yearly' ? 'primary' : 'soft-primary'}
                value="yearly"
              >
                Yearly
                <Badge
                  className="badge-floating badge-pill"
                  variant="success"
                >
                  -13%
                </Badge>
              </ToggleButton>
            </ToggleButtonGroup>
          </Col>
        </Row>
      )}
    </SelectedPlanContext.Consumer>
  );
}

type PurchaseButtonProps = {
  planType: PlanType;
  highlighted?: boolean;
};

function PurchaseButton({
  planType,
  highlighted = false,
}: PurchaseButtonProps) {
  const {
    billingCycle,
  } = useSelectedPlan();
  const [submitting, setSubmitting] = useState(false);
  const {trackComponentEvent} = useTracking();
  const plan = plans[planType][billingCycle];
  const {
    callToAction,
    priceId,
  } = plan;

  async function handleClick() {
    setSubmitting(true);
    trackComponentEvent('PurchaseButton', 'Checkout Start', {
      planType,
      billingCycle,
      priceId,
    });
    await redirectToStripeCheckoutSession({
      priceIds: [priceId],
    });
    setSubmitting(false);
  }

  const variant = highlighted ? 'white' : 'warning';
  return (
    <Button
      variant={variant}
      className="hover-translate-y-n3 hover-shadow-lg mb-3"
      onClick={handleClick}
      disabled={submitting}
    >
      {callToAction}
    </Button>
  );
}

type PriceTitleProps = {
  planType: PlanType;
  highlighted?: boolean;
};

function PriceTitle({
  planType,
  highlighted = false,
}: PriceTitleProps) {
  const {
    billingCycle,
  } = useSelectedPlan();
  const sku = plans[planType][billingCycle];
  return (
    <>
      <h1 className={cx('h6', {
        'text-white': highlighted,
        'text-muted': !highlighted,
      })}
      >
        {sku.title}
      </h1>
      <div className={cx('h1 text-center mb-0', {'text-white': highlighted})}>
        $
        <span className="font-weight-bolder">
          {sku.price}
        </span>
      </div>
      <span className={cx('text-sm', {'text-white': highlighted})}>
        {sku.billingInfo}
      </span>
    </>
  );
}
