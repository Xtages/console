import React, {useEffect} from 'react';
import {useQueryParams} from 'hooks/useQueryParams';
import {useIsMutating, useMutation} from 'react-query';
import {gitHubAppApi} from 'service/Services';
import {useHistory} from 'react-router-dom';

const MUTATION_KEY = 'recordInstall';

/**
 * Page to handle the installation of the GH App. We send the installation to the server to record
 * it in the DB.
 */
export default function GitHubAppPostInstallPage() {
  const queryParams = useQueryParams();
  const isMutating = useIsMutating({mutationKey: MUTATION_KEY});
  const history = useHistory();

  const {
    isIdle,
    mutate,
  } = useMutation(() => gitHubAppApi.recordInstall({
    code: queryParams.get('code')!,
    installationId: parseInt(queryParams.get('installation_id')!, 10),
    setupAction: queryParams.get('setup_action')!,
    state: queryParams.get('state')!,
  }), {
    mutationKey: MUTATION_KEY,
    retry: false,
    onSuccess: () => {
      history.push('/');
    },
  });

  useEffect(() => {
    if (!isMutating && isIdle) {
      mutate();
    }
  }, []);

  // TODO(czuniga): Handle the errors cases: 1) when the state doesn't match 2) when the app is
  //  installed incorrectly. 3) Org already exists.

  return (<></>);
}
