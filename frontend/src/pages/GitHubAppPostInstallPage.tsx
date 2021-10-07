import React, {useEffect} from 'react';
import {useQueryParams} from 'hooks/useQueryParams';
import {useIsMutating, useMutation} from 'react-query';
import {gitHubAppApi} from 'service/Services';
import {useHistory} from 'react-router-dom';
import {CenteredOnScreen} from 'components/layout/CenteredOnScreen';
import Axios, {AxiosResponse} from 'axios';
import {Alert, Button, Col, Container, Row, Spinner} from 'react-bootstrap';
import Logos from 'components/Logos';
import {Organization, OrganizationGitHubOrganizationTypeEnum} from 'gen/api';
import {useAuth} from 'hooks/useAuth';

const MUTATION_KEY = 'recordInstall';

/**
 * Page to handle the installation of the GH App. We send the installation to the server to record
 * it in the DB.
 */
export default function GitHubAppPostInstallPage() {
  const queryParams = useQueryParams();
  const isMutating = useIsMutating({mutationKey: MUTATION_KEY});
  const history = useHistory();
  const {principal} = useAuth();

  const {
    isIdle,
    isError,
    error,
    mutate,
  } = useMutation(() => gitHubAppApi.recordInstall({
    code: queryParams.get('code')!,
    installationId: parseInt(queryParams.get('installation_id')!, 10),
    setupAction: queryParams.get('setup_action')!,
    state: queryParams.get('state')!,
  }), {
    mutationKey: MUTATION_KEY,
    retry: false,
    onSuccess: (response: AxiosResponse<Organization>) => {
      const {gitHubOrganizationType, name} = response.data;
      if (gitHubOrganizationType === OrganizationGitHubOrganizationTypeEnum.Individual) {
        const url = new URL('https://github.com/login/oauth/authorize');
        url.searchParams.set('client_id', process.env.REACT_APP_GIT_HUB_OAUTH_CLIENT_ID!!);
        url.searchParams.set('login', name);
        url.searchParams.set('scope', 'repo user');
        url.searchParams.set('state', principal?.id!!);
        url.searchParams.set('allow_signup', false.toString());
        window.location.assign(url.toString());
      } else {
        backToHome();
      }
    },
  });

  useEffect(() => {
    if (!isMutating && isIdle) {
      mutate();
    }
  }, []);

  function backToHome() {
    history.push('/');
  }

  if (isError) {
    if (Axios.isAxiosError(error) && error.response?.data?.error_code === 'INVALID_GITHUB_APP_INSTALL_NOT_ALL_REPOS_SELECTED') {
      return (
        <CenteredOnScreen>
          <Container>
            <Row>
              <Col sm="auto" className="pb-4">
                <Logos size="sm" />
              </Col>
            </Row>
          </Container>
          <Alert variant="warning">
            The Xtages Connector GitHub app must be installed on all repositories.
          </Alert>
          <Container>
            <Row>
              <Col sm="auto">
                <Button variant="outline-primary" size="sm" onClick={backToHome}>
                  Back to the Home
                  page
                </Button>
              </Col>
            </Row>
          </Container>
        </CenteredOnScreen>
      );
    }
    return (
      <CenteredOnScreen>
        <Container>
          <Row>
            <Col sm="auto" className="pb-4">
              <Logos size="sm" />
            </Col>
          </Row>
        </Container>
        <Alert variant="danger">
          Something went wrong. Please try installing the GitHub app
          again.
        </Alert>
        <Container>
          <Row className="justify-content-end">
            <Col sm="auto">
              <Button variant="outline-primary" size="sm" onClick={backToHome}>
                Back to the Home
                page
              </Button>
            </Col>
          </Row>
        </Container>
      </CenteredOnScreen>
    );
  }

  return (
    <CenteredOnScreen>
      <Spinner animation="grow" role="status" variant="dark-secondary">
        <span className="sr-only">Loading...</span>
      </Spinner>
    </CenteredOnScreen>
  );
}
