import React, {useEffect} from 'react';
import {useQueryParams} from 'hooks/useQueryParams';
import {useIsMutating, useMutation} from 'react-query';
import {gitHubAppApi} from 'service/Services';
import {useHistory} from 'react-router-dom';
import {CenteredOnScreen} from 'components/layout/CenteredOnScreen';
import {Alert, Button, Col, Container, Row, Spinner} from 'react-bootstrap';
import Logos from 'components/Logos';
import {useTracking} from 'hooks/useTracking';

const MUTATION_KEY = 'recordOauthInstall';

/**
 * Page to handle the installation of the GH OAuth App. We send the installation to the server to
 * record it in the DB.
 */
export default function GitHubOauthAppPostInstallPage() {
  const queryParams = useQueryParams();
  const isMutating = useIsMutating({mutationKey: MUTATION_KEY});
  const history = useHistory();
  const {trackComponentEvent, trackComponentApiError} = useTracking();

  const {
    isIdle,
    isError,
    error,
    mutate,
  } = useMutation(() => gitHubAppApi.recordOauthInstall({
    code: queryParams.get('code')!,
    state: queryParams.get('state')!,
  }), {
    mutationKey: MUTATION_KEY,
    retry: false,
    onSuccess: backToHome,
  });

  useEffect(() => {
    if (!isMutating && isIdle) {
      trackComponentEvent('GitHubOauthAppPostInstallPage', 'Install succeeded');
      mutate();
    }
  }, []);

  function backToHome() {
    trackComponentEvent('GitHubOauthAppPostInstallPage', 'Install recorded');
    history.push('/');
  }

  if (isError) {
    trackComponentApiError(
      'GitHubOauthAppPostInstallPage',
      'gitHubAppApi.recordOauthInstall',
      error,
    );
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
