import React from 'react';
import {Link, NavLink, useHistory, useRouteMatch} from 'react-router-dom';
import {useAuth} from 'hooks/useAuth';
import {Button, Col, Container, Dropdown, Row} from 'react-bootstrap';
import {useTracking} from 'hooks/useTracking';
import {useOrganization} from 'hooks/useOrganization';
import {Star} from 'react-feather';
import Logo from '../Logos';
import Avatar from '../avatar/Avatar';

const AvatarDropdownToggle = React.forwardRef<HTMLButtonElement, React.ComponentPropsWithoutRef<'button'>>(({
  // eslint-disable-next-line react/prop-types
  children,
  // eslint-disable-next-line react/prop-types
  onClick,
}, ref) => (
  <button
    type="button"
    className="btn-transparent rounded-circle p-1"
    ref={ref}
    onClick={(e) => {
      e.preventDefault();
      if (onClick) {
        onClick(e);
      }
    }}
  >
    {children}
  </button>
));

export default function NavBar() {
  const auth = useAuth();
  const history = useHistory();
  const {reset} = useTracking();
  const {organization} = useOrganization();
  const inUpgradePage = useRouteMatch({path: '/upgrade', strict: true});

  function goToAccount() {
    history.push('/account');
  }

  function goToUpgrade() {
    history.push('/upgrade');
  }

  async function signOut() {
    await auth.logOut();
    reset();
    history.push('/login');
  }

  return (
    <nav className="navbar navbar-expand-lg shadow navbar-light bg-white">
      <div className="container">
        <Link className="navbar-brand" to="/">
          <Logo size="xs" />
        </Link>
        <div className="collapse navbar-collapse">
          <ul className="navbar-nav ml-auto mr-2">
            <li className="nav-item">
              <NavLink className="nav-link" to="/" exact>Projects</NavLink>
            </li>
            <li className="nav-item">
              <NavLink className="nav-link" to="/deployments/" exact>Deployments</NavLink>
            </li>
            <li className="nav-item">
              <NavLink className="nav-link" to="/resources" exact>Resources</NavLink>
            </li>
            <li className="nav-item">
              <a
                className="nav-link"
                href="https://docs.xtages.com"
                target="_blank"
                rel="noreferrer"
              >
                Docs
              </a>
            </li>
          </ul>
        </div>
        {!inUpgradePage
        && organization
        && (organization.plan && !organization.plan.paid)
        && (
        <Button
          variant="warning"
          size="sm"
          className="mr-1 btn-icon-label"
          onClick={goToUpgrade}
        >
          <span className="btn-inner--icon">
            <Star size="0.9em" fill="white" />
          </span>
          <span className="btn-inner--text">Upgrade</span>
        </Button>
        )}
        <div className="ml-1">
          <Container className="p-0">
            <Row noGutters className="align-items-center">
              <Col sm="auto" className="pr-2">
                {organization?.name && (
                <div>
                  {organization?.name}
                </div>
                )}
              </Col>
              <Col sm="auto">
                <Dropdown>
                  <Dropdown.Toggle as={AvatarDropdownToggle}>
                    <Avatar rounded size="sm" />
                  </Dropdown.Toggle>
                  <Dropdown.Menu>
                    <Dropdown.Header className="text-xs text-nowrap">
                      {auth?.principal?.name ?? auth?.principal?.email}
                    </Dropdown.Header>
                    <Dropdown.Divider />
                    <Dropdown.Item onClick={goToAccount}>Account</Dropdown.Item>
                    <Dropdown.Divider />
                    <Dropdown.Item onClick={signOut}>Sign out</Dropdown.Item>
                  </Dropdown.Menu>
                </Dropdown>
              </Col>
            </Row>
          </Container>
        </div>
      </div>
    </nav>
  );
}
