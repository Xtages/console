import React from 'react';
import {Link, NavLink} from 'react-router-dom';
import {Menu, MenuButton, MenuDivider, MenuItem} from '@szhsin/react-menu';
import {useAuth} from 'hooks/useAuth';
import Logo from '../Logos';
import Avatar from '../avatar/Avatar';
import '@szhsin/react-menu/dist/index.css';
import LogoutButton from '../authn/LogoutButton';

export default function NavBar() {
  const auth = useAuth();
  return (
    <nav className="navbar navbar-expand-lg shadow navbar-light bg-white">
      <div className="container">
        <Link className="navbar-brand" to="/">
          <Logo size="xs" />
        </Link>
        <div className="collapse navbar-collapse">
          <ul className="navbar-nav ml-auto mr-5">
            <li className="nav-item">
              <NavLink className="nav-link" to="/" exact>Projects</NavLink>
            </li>
            <li className="nav-item">
              <NavLink className="nav-link" to="/deployment" exact>Deployments</NavLink>
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
        <div className="ml-3">
          {auth?.principal?.name
          && (
          <span className="mr-2">
            Hello,
            {' '}
            {auth?.principal?.name}
            !
          </span>
          )}
          <Menu
            className="text-body"
            align="end"
            arrow
            menuButton={(
              <MenuButton className="border-0 bg-transparent">
                <Avatar
                  rounded
                />
              </MenuButton>
                        )}
          >
            <MenuItem className="dropdown-item"><Link to="/account" className="text-body">My account</Link></MenuItem>
            <MenuDivider />
            <MenuItem className="dropdown-item"><LogoutButton /></MenuItem>
          </Menu>
        </div>
      </div>
    </nav>
  );
}
