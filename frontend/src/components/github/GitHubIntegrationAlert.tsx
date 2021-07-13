import React from 'react';
import {Alert} from 'react-bootstrap';
import {useQuery} from 'react-query';
import {organizationApi} from 'service/Services';
import {ReactComponent as GitHubIcon} from 'assets/img/github-icon.svg';

/**
 * Displays an {@link Alert} if the organization hasn't install the GitHub app.
 */
export function GitHubIntegrationAlert() {
  const {
    isSuccess,
    data,
  } = useQuery('org', () => organizationApi.getOrganization(), {
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    refetchOnMount: false,
  });

  if (isSuccess && data?.data && !data.data.githubAppInstalled) {
    return (
      <Alert variant="dark">
        <Alert.Heading>Hi! Thank you for using Xtages!</Alert.Heading>
        <p>
          Before we get rocking, you
          {' '}
          <strong>must</strong>
          {' '}
          install our
          {' '}
          <a href={process.env.REACT_APP_GIT_HUB_APP_URL} target="_blank" rel="noreferrer">
            GitHub App
          </a>
          {' '}
          on your GitHub organization.
        </p>
        <p>
          Through our GitHub app, we will be able to:
        </p>
        <ul>
          <li>Create new repos backing Xtages projects.</li>
          <li>Run continuous integration on every push made to your project.</li>
          <li>Tag your repo when we deploy your project.</li>
        </ul>
        <hr />
        <p className="text-right">
          <a
            href={process.env.REACT_APP_GIT_HUB_APP_INSTALL_URL}
            target="_blank"
            rel="noreferrer"
            className="btn btn-primary btn-icon-label noExternalLinkIcon"
          >
            <span className="btn-inner--icon">
              <GitHubIcon height="1.5em" fill="white" />
            </span>
            <span className="btn-inner--text">Install</span>
          </a>
        </p>
      </Alert>
    );
  }
  return <></>;
}
