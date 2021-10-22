import React from 'react';
import {Link, NavLink, useHistory} from 'react-router-dom';
import {useAuth} from 'hooks/useAuth';
import {Col, Container, Dropdown, Row} from 'react-bootstrap';
import {useTracking} from 'hooks/useTracking';
import {useOrganization} from 'hooks/useOrganization';
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

  function goToAccount() {
    history.push('/account');
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
        <div className="ml-1">
          <Container className="p-0 text-xs text-right">
            <Row noGutters className="align-items-center">
              <Col sm="auto" className="pr-2">
                <div>
                  {auth?.principal?.name ?? auth?.principal?.email}
                </div>
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
