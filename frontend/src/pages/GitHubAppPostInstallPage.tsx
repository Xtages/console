import React from 'react';
import {useQueryParams} from 'hooks/useQueryParams';
import {useMutation} from 'react-query';
import {gitHubAppApi} from 'service/Services';
import {useHistory} from 'react-router-dom';

/**
 * Page to handle the installation of the GH App. We send the installation to the server to record
 * it in the DB.
 */
export default function GitHubAppPostInstallPage() {
  const queryParams = useQueryParams();
  const history = useHistory();

  const {
    isIdle,
    isSuccess,
    mutate,
  } = useMutation(() => gitHubAppApi.recordInstall({
    code: queryParams.get('code')!,
    installationId: parseInt(queryParams.get('installation_id')!, 10),
    setupAction: queryParams.get('setup_action')!,
    state: queryParams.get('state')!,
  }), {
    retry: false,
  });

  if (isIdle) {
    mutate();
  }

  if (isSuccess) {
    history.push('/');
  }

  // TODO(czuniga): Handle the errors cases: 1) when the state doesn't match 2) when the app is
  //  installed incorrectly. 3) Org already exists.

  return (<></>);
}
