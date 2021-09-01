import {DefaultRequestBody,
  RequestParams,
  ResponseComposition,
  rest,
  RestContext,
  RestRequest} from 'msw';
import {BuildActions,
  BuildStatusEnum,
  BuildType,
  DeploymentStatusEnum,
  Logs,
  Organization,
  OrganizationSubscriptionStatusEnum,
  Project,
  Projects,
  ProjectTypeEnum,
  ResourceType,
  UsageDetail,
  UsageDetailStatusEnum} from 'gen/api';

const organization: Organization = {
  name: 'Ghostbusters',
  subscriptionStatus: OrganizationSubscriptionStatusEnum.Active,
  githubAppInstalled: true,
};

const projectsArray: Array<Project> = [{
  id: 6,
  name: 'GhostTrap',
  organization: 'Ghostbusters',
  ghRepoUrl: 'https://github.com/Ghostbusters/GhostTrap',
  type: ProjectTypeEnum.Node,
  version: '15.13.0',
  passCheckRuleEnabled: false,
  builds: [{
    id: 19,
    buildNumber: 5,
    type: BuildType.Cd,
    env: 'production',
    status: BuildStatusEnum.Succeeded,
    initiatorEmail: 'w.zeddemore@ghostbusters.com',
    initiatorName: 'Winston Zeddemore',
    initiatorAvatarUrl: 'https://pbs.twimg.com/profile_images/1364761932255887360/qpzfsFKe_400x400.jpg',
    commitHash: '20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    commitUrl: 'https://github.com/Ghostbusters/GhostTrap/commit/20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    startTimestampInMillis: 1628803353843,
    actions: [BuildActions.Promote, BuildActions.Rollback],
    phases: [],
    endTimestampInMillis: 1628803400000,
  }],
  deployments: [{
    id: 13,
    initiatorEmail: 'e.spengler@ghostbusters.com',
    initiatorName: 'Egon Spengler',
    initiatorAvatarUrl: 'https://openpsychometrics.org/tests/characters/test-resources/pics/GB/3.jpg',
    commitHash: '20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    commitUrl: 'https://github.com/XtagesCzunigaTest/GhostTrap/commit/20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    env: 'staging',
    timestampInMillis: 1629321800000,
    serviceUrls: ['https://staging-849f69422c19.xtages.xyz'],
    status: DeploymentStatusEnum.Stopped,
  }, {
    id: 15,
    initiatorEmail: 'e.spengler@ghostbusters.com',
    initiatorName: 'Egon Spengler',
    initiatorAvatarUrl: 'https://openpsychometrics.org/tests/characters/test-resources/pics/GB/3.jpg',
    commitHash: '20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    commitUrl: 'https://github.com/XtagesCzunigaTest/GhostTrap/commit/7380a5568ce8756db3d2d11ecf5138614d808770',
    env: 'production',
    timestampInMillis: 1629149000000,
    serviceUrls: ['https://production-849f69422c19.xtages.xyz'],
    status: DeploymentStatusEnum.Running,
  }],
  percentageOfSuccessfulBuildsInTheLastMonth: 0.9,
}, {
  id: 5,
  name: 'Ecto-1',
  organization: 'Ghostbusters',
  ghRepoUrl: 'https://github.com/Ghostbusters/Ecto-1',
  type: ProjectTypeEnum.Node,
  version: '15.13.0',
  passCheckRuleEnabled: false,
  builds: [{
    id: 13,
    buildNumber: 2,
    type: BuildType.Cd,
    env: 'staging',
    status: BuildStatusEnum.Succeeded,
    initiatorEmail: 'e.spengler@ghostbusters.com',
    initiatorName: 'Egon Spengler',
    initiatorAvatarUrl: 'https://openpsychometrics.org/tests/characters/test-resources/pics/GB/3.jpg',
    commitHash: '365a0aef96b430c82a498e5fb57e26b25aa59ac3',
    commitUrl: 'https://github.com/Ghostbusters/Ecto-1/commit/365a0aef96b430c82a498e5fb57e26b25aa59ac3',
    startTimestampInMillis: 1629321753843,
    actions: [BuildActions.Deploy, BuildActions.Promote],
    phases: [],
    endTimestampInMillis: 1629321800000,
  }],
  deployments: [],
  percentageOfSuccessfulBuildsInTheLastMonth: 0.5,
}, {
  id: 1,
  name: 'ProtonPack',
  organization: 'Ghostbusters',
  ghRepoUrl: 'https://github.com/Ghostbusters/ProtonPack',
  type: ProjectTypeEnum.Node,
  version: '15.13.0',
  passCheckRuleEnabled: false,
  builds: [{
    id: 21,
    buildNumber: 13,
    type: BuildType.Ci,
    env: 'dev',
    status: BuildStatusEnum.Failed,
    initiatorEmail: 'p.venkman@ghostbusters.com',
    initiatorName: 'Peter Venkman',
    initiatorAvatarUrl: 'https://www.fillmurray.com/100/100',
    commitHash: 'e53915cef4ce13a5a6e6d54363b705d3e34b8691',
    commitUrl: 'https://github.com/Ghostbusters/ProtonPack/commit/e53915cef4ce13a5a6e6d54363b705d3e34b8691',
    startTimestampInMillis: 1629845051616,
    actions: [BuildActions.Ci],
    phases: [],
    endTimestampInMillis: 1629845099000,
  }],
  deployments: [],
  percentageOfSuccessfulBuildsInTheLastMonth: 0.2,
}];

const projects: Projects = {
  projects: projectsArray,
};

const projectWithBuildsAndDeployments: Project = {
  ...projectsArray.find((project) => project.id === 6)!!,
  builds: [{
    id: 19,
    buildNumber: 5,
    type: BuildType.Cd,
    env: 'production',
    status: BuildStatusEnum.Succeeded,
    initiatorEmail: 'w.zeddemore@ghostbusters.com',
    initiatorName: 'Winston Zeddemore',
    initiatorAvatarUrl: 'https://pbs.twimg.com/profile_images/1364761932255887360/qpzfsFKe_400x400.jpg',
    commitHash: '20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    commitUrl: 'https://github.com/Ghostbusters/GhostTrap/commit/20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    startTimestampInMillis: 1628803353843,
    actions: [BuildActions.Promote, BuildActions.Rollback],
    phases: [],
    endTimestampInMillis: 1628803400000,
  }, {
    id: 14,
    buildNumber: 3,
    type: BuildType.Cd,
    env: 'staging',
    status: BuildStatusEnum.Failed,
    initiatorEmail: 'e.spengler@ghostbusters.com',
    initiatorName: 'Egon Spengler',
    initiatorAvatarUrl: 'https://openpsychometrics.org/tests/characters/test-resources/pics/GB/3.jpg',
    commitHash: '20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    commitUrl: 'https://github.com/XtagesCzunigaTest/GhostTrap/commit/db6276db7c943f3abd19295743c8c2565156e826',
    startTimestampInMillis: 1629235353843,
    actions: [BuildActions.Deploy],
    phases: [],
    endTimestampInMillis: 1629235400000,
  }, {
    id: 15,
    buildNumber: 4,
    type: BuildType.Cd,
    env: 'production',
    status: BuildStatusEnum.Succeeded,
    initiatorEmail: 'e.spengler@ghostbusters.com',
    initiatorName: 'Egon Spengler',
    initiatorAvatarUrl: 'https://openpsychometrics.org/tests/characters/test-resources/pics/GB/3.jpg',
    commitHash: '20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    commitUrl: 'https://github.com/XtagesCzunigaTest/GhostTrap/commit/7380a5568ce8756db3d2d11ecf5138614d808770',
    startTimestampInMillis: 1629148953843,
    actions: [BuildActions.Promote, BuildActions.Rollback],
    phases: [],
    endTimestampInMillis: 1629149000000,
  }],
  deployments: [{
    id: 13,
    initiatorEmail: 'e.spengler@ghostbusters.com',
    initiatorName: 'Egon Spengler',
    initiatorAvatarUrl: 'https://openpsychometrics.org/tests/characters/test-resources/pics/GB/3.jpg',
    commitHash: '20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    commitUrl: 'https://github.com/XtagesCzunigaTest/GhostTrap/commit/20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    env: 'staging',
    timestampInMillis: 1629321800000,
    serviceUrls: ['https://staging-849f69422c19.xtages.xyz'],
    status: DeploymentStatusEnum.Stopped,
  }, {
    id: 15,
    initiatorEmail: 'e.spengler@ghostbusters.com',
    initiatorName: 'Egon Spengler',
    initiatorAvatarUrl: 'https://openpsychometrics.org/tests/characters/test-resources/pics/GB/3.jpg',
    commitHash: '20002e398382b2b1d47e74c05aa648ae1b6e6fb3',
    commitUrl: 'https://github.com/XtagesCzunigaTest/GhostTrap/commit/7380a5568ce8756db3d2d11ecf5138614d808770',
    env: 'production',
    timestampInMillis: 1629149000000,
    serviceUrls: ['https://production-849f69422c19.xtages.xyz'],
    status: DeploymentStatusEnum.Running,
  }],
};

const usage: Array<UsageDetail> = [{
  resourceType: ResourceType.Project,
  status: UsageDetailStatusEnum.UnderLimit,
  limit: 5,
  usage: 3,
}, {
  resourceType: ResourceType.MonthlyBuildMinutes,
  status: UsageDetailStatusEnum.UnderLimit,
  limit: 7500,
  usage: 15,
  resetTimestampInMillis: 1630799999999,
}, {
  resourceType: ResourceType.MonthlyDataTransferGbs,
  status: UsageDetailStatusEnum.UnderLimit,
  limit: 500,
  usage: 0,
  resetTimestampInMillis: 1630799999999,
}, {
  resourceType: ResourceType.DbStorageGbs,
  status: UsageDetailStatusEnum.UnderLimit,
  limit: 20,
  usage: 1,
}];

const buildLogs: Logs = {
  events: [
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:46 Waiting for agent ping\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:49 Waiting for DOWNLOAD_SOURCE\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Phase is DOWNLOAD_SOURCE\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Processing environment variables\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Decrypting parameter store environment variables\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Registering with agent\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50  BUILD: 5 commands\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Phase complete: DOWNLOAD_SOURCE State: SUCCEEDED\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Phase context status code:  Message: \n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Entering phase INSTALL\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Phase complete: INSTALL State: SUCCEEDED\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Phase context status code:  Message: \n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Entering phase PRE_BUILD\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Phase complete: PRE_BUILD State: SUCCEEDED\n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Phase context status code:  Message: \n',
    },
    {
      timestamp: 1629915474524,
      message: '[Container] 2021/08/25 18:17:50 Entering phase BUILD\n',
    },
    {
      timestamp: 1629915474524,
      // eslint-disable-next-line no-template-curly-in-string
      message: '[Container] 2021/08/25 18:17:50 Running command git clone -q https://x-access-token:${XTAGES_GITHUB_TOKEN}@github.com/${XTAGES_REPO}.git project_src && cd project_src\n',
    },
    {
      timestamp: 1629915474524,
      message: '\n',
    },
    {
      timestamp: 1629915474524,
      // eslint-disable-next-line no-template-curly-in-string
      message: '[Container] 2021/08/25 18:17:51 Running command git reset --hard ${XTAGES_COMMIT} && cd ..\n',
    },
    {
      timestamp: 1629915474524,
      message: 'HEAD is now at 289105b time change\n',
    },
    {
      timestamp: 1629915474524,
      message: '\n',
    },
    {
      timestamp: 1629915474524,
      // eslint-disable-next-line no-template-curly-in-string
      message: '[Container] 2021/08/25 18:17:51 Running command git clone -q https://x-access-token:${XTAGES_RECIPE_GIT_TOKEN}@github.com/${XTAGES_RECIPE_REPO}.git build_src && cd build_src\n',
    },
    {
      timestamp: 1629915474524,
      message: '\n',
    },
    {
      timestamp: 1629915474524,
      // eslint-disable-next-line no-template-curly-in-string
      message: '[Container] 2021/08/25 18:17:51 Running command git checkout -q ${XTAGES_GH_RECIPE_TAG}\n',
    },
    {
      timestamp: 1629915474524,
      message: '\n',
    },
    {
      timestamp: 1629915474524,
      // eslint-disable-next-line no-template-curly-in-string
      message: '[Container] 2021/08/25 18:17:51 Running command sh ${XTAGES_SCRIPT}\n',
    },
    {
      timestamp: 1629915478582,
      message: 'npm WARN deprecated urix@0.1.0: Please see https://github.com/lydell/urix#deprecated\n',
    },
    {
      timestamp: 1629915478582,
      message: 'npm WARN deprecated resolve-url@0.2.1: https://github.com/lydell/resolve-url#deprecated\n',
    },
    {
      timestamp: 1629915478582,
      message: 'npm WARN deprecated request-promise-native@1.0.9: request-promise-native has been deprecated because it extends the now deprecated request package, see https://github.com/request/request/issues/3142\n',
    },
    {
      timestamp: 1629915480602,
      message: 'npm WARN deprecated har-validator@5.1.5: this library is no longer supported\n',
    },
    {
      timestamp: 1629915482626,
      message: 'npm WARN deprecated request@2.88.2: request has been deprecated, see https://github.com/request/request/issues/3142\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915484646,
      message: 'added 588 packages, and audited 589 packages in 11s\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915484646,
      message: '25 packages are looking for funding\n',
    },
    {
      timestamp: 1629915484646,
      message: '  run `npm fund` for details\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915484646,
      message: '2 moderate severity vulnerabilities\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915484646,
      message: 'To address all issues, run:\n',
    },
    {
      timestamp: 1629915484646,
      message: '  npm audit fix\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915484646,
      message: 'Run `npm audit` for details.\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915484646,
      message: '> GhostTrap@1.0.0 build\n',
    },
    {
      timestamp: 1629915484646,
      message: '> echo FILL_ME\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915484646,
      message: 'FILL_ME\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915484646,
      message: '> GhostTrap@1.0.0 test\n',
    },
    {
      timestamp: 1629915484646,
      message: '> jest\n',
    },
    {
      timestamp: 1629915484646,
      message: '\n',
    },
    {
      timestamp: 1629915487337,
      message: 'PASS tests/unit/simple_test.js\n',
    },
    {
      timestamp: 1629915487337,
      message: '  ✓ simple test (5 ms)\n',
    },
    {
      timestamp: 1629915487337,
      message: '\n',
    },
    {
      timestamp: 1629915487337,
      message: 'Test Suites: 1 passed, 1 total\n',
    },
    {
      timestamp: 1629915487337,
      message: 'Tests:       1 passed, 1 total\n',
    },
    {
      timestamp: 1629915487337,
      message: 'Snapshots:   0 total\n',
    },
    {
      timestamp: 1629915487337,
      message: 'Time:        0.65 s\n',
    },
    {
      timestamp: 1629915487337,
      message: 'Ran all test suites.\n',
    },
    {
      timestamp: 1629915487337,
      message: '\n',
    },
    {
      timestamp: 1629915487337,
      message: '[Container] 2021/08/25 18:18:05 Phase complete: BUILD State: SUCCEEDED\n',
    },
    {
      timestamp: 1629915487337,
      message: '[Container] 2021/08/25 18:18:05 Phase context status code:  Message: \n',
    },
    {
      timestamp: 1629915487337,
      message: '[Container] 2021/08/25 18:18:05 Entering phase POST_BUILD\n',
    },
    {
      timestamp: 1629915487337,
      message: '[Container] 2021/08/25 18:18:05 Phase complete: POST_BUILD State: SUCCEEDED\n',
    },
    {
      timestamp: 1629915487337,
      message: '[Container] 2021/08/25 18:18:05 Phase context status code:  Message: \n',
    },
  ],
};

const deployLogs: Logs = {
  events: [
    {
      timestamp: 1630434915899,
      message: '--2021-08-31 18:35:15.898 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630434919198,
      message: '--2021-08-31 18:35:19.198 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630434920418,
      message: '--2021-08-31 18:35:20.418 GET /, headers=[x-forwarded-for:"162.142.125.193", x-forwarded-proto:"https", x-forwarded-port:"443", host:"52.73.13.9", x-amzn-trace-id:"Root=1-612e7668-6d94d63553ec8e902fe72333"]]',
    },
    {
      timestamp: 1630434920537,
      message: '--2021-08-31 18:35:20.537 GET /, headers=[x-forwarded-for:"162.142.125.193", x-forwarded-proto:"https", x-forwarded-port:"443", host:"52.73.13.9", x-amzn-trace-id:"Root=1-612e7668-5b95a03278a2d1b3190944c5", user-agent:"Mozilla/5.0 (compatible; CensysInspect/1.1; +https://about.censys.io/)", accept:"*/*", accept-encoding:"gzip"]]',
    },
    {
      timestamp: 1630435039314,
      message: '--2021-08-31 18:37:19.313 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630435069088,
      message: '--2021-08-31 18:37:49.087 GET /api/v1/device/check?screen=true, headers=[x-forwarded-for:"109.248.6.132", x-forwarded-proto:"https", x-forwarded-port:"443", host:"console-lb-production-347267553.us-east-1.elb.amazonaws.com", x-amzn-trace-id:"Root=1-612e76fd-55cb5c743c3f9af949e4f0ec", user-agent:"masscan-ng/1.3 (https://github.com/bi-zone/masscan-ng)", accept:"*/*", authorization:"masked"]]',
    },
    {
      timestamp: 1630435081044,
      message: '--2021-08-31 18:38:01.043 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630435703477,
      message: '--2021-08-31 18:48:23.477 GET /.env, headers=[x-forwarded-for:"20.102.123.158", x-forwarded-proto:"https", x-forwarded-port:"443", host:"52.22.145.129", x-amzn-trace-id:"Root=1-612e7977-4296f10328aa943f5d7f2203", accept-encoding:"gzip, deflate", accept:"*/*", user-agent:"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Safari/537.36"]]',
    },
    {
      timestamp: 1630435749802,
      message: '--2022-08-31 18:49:09.803 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630435756437,
      message: '--2022-08-31 18:49:16.438 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630435759843,
      message: '--2022-08-31 18:49:19.844 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630435782695,
      message: '--2021-08-31 18:49:42.695 GET /, headers=[x-forwarded-for:"104.140.188.42", x-forwarded-proto:"https", x-forwarded-port:"443", host:"34.196.231.109", x-amzn-trace-id:"Root=1-612e79c6-036af1f7290c43ce5211b119", user-agent:"Go http package", referer:"http://34.196.231.109:80/"]]',
    },
    {
      timestamp: 1630435789053,
      message: '--2021-08-31 18:49:49.053 GET /?XDEBUG_SESSION_START=phpstorm, headers=[x-forwarded-for:"45.146.164.110", x-forwarded-proto:"https", x-forwarded-port:"443", host:"54.167.212.252", x-amzn-trace-id:"Root=1-612e79cd-053e75577f313634223d09bc", user-agent:"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36", accept-encoding:"gzip"]]',
    },
    {
      timestamp: 1630435790856,
      message: '--2021-08-31 18:49:50.856 GET /console/, headers=[x-forwarded-for:"45.146.164.110", x-forwarded-proto:"https", x-forwarded-port:"443", host:"54.167.212.252", x-amzn-trace-id:"Root=1-612e79ce-779afd405fe5d9f80fa57f62", user-agent:"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36", accept-encoding:"gzip"]]',
    },
    {
      timestamp: 1630435791587,
      message: '--2021-08-31 18:49:51.587 GET /wp-content/plugins/wp-file-manager/readme.txt, headers=[x-forwarded-for:"45.146.164.110", x-forwarded-proto:"https", x-forwarded-port:"443", host:"54.167.212.252", x-amzn-trace-id:"Root=1-612e79cf-4541048d1e2de8523a1f3379", user-agent:"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36", accept-encoding:"gzip"]]',
    },
    {
      timestamp: 1630435793659,
      message: '--2021-08-31 18:49:53.659 GET /_ignition/execute-solution, headers=[x-forwarded-for:"45.146.164.110", x-forwarded-proto:"https", x-forwarded-port:"443", host:"54.167.212.252", x-amzn-trace-id:"Root=1-612e79d1-719874586b928afa0d91b067", user-agent:"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36", accept-encoding:"gzip", Content-Type:"application/json;charset=UTF-8"]]',
    },
    {
      timestamp: 1630435794033,
      message: '--2021-08-31 18:49:54.033 GET /vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php, headers=[x-forwarded-for:"45.146.164.110", x-forwarded-proto:"https", x-forwarded-port:"443", host:"54.167.212.252", x-amzn-trace-id:"Root=1-612e79d2-46c140ca09a61dc44f190e1c", content-length:"19", user-agent:"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36", accept-encoding:"gzip"]]',
    },
    {
      timestamp: 1630435799245,
      message: '--2021-08-31 18:49:59.245 GET /, headers=[x-forwarded-for:"45.146.164.110", x-forwarded-proto:"https", x-forwarded-port:"443", host:"54.167.212.252", x-amzn-trace-id:"Root=1-612e79d7-380750c5304244fe0f7722f4", user-agent:"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36", accept-encoding:"gzip"]]',
    },
    {
      timestamp: 1630436653932,
      message: '--2021-08-31 19:04:13.932 GET /pv/aastra.cfg, headers=[x-forwarded-for:"104.149.191.22", x-forwarded-proto:"https", x-forwarded-port:"443", host:"3.212.89.41", x-amzn-trace-id:"Root=1-612e7d2d-21a8a02b18ab65e002da383e", user-agent:"Cisco"]]',
    },
    {
      timestamp: 1630434915899,
      message: '--2021-08-31 18:35:15.898 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630434919198,
      message: '--2021-08-31 18:35:19.198 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630434920418,
      message: '--2021-08-31 18:35:20.418 GET /, headers=[x-forwarded-for:"162.142.125.193", x-forwarded-proto:"https", x-forwarded-port:"443", host:"52.73.13.9", x-amzn-trace-id:"Root=1-612e7668-6d94d63553ec8e902fe72333"]]',
    },
    {
      timestamp: 1630434920537,
      message: '--2021-08-31 18:35:20.537 GET /, headers=[x-forwarded-for:"162.142.125.193", x-forwarded-proto:"https", x-forwarded-port:"443", host:"52.73.13.9", x-amzn-trace-id:"Root=1-612e7668-5b95a03278a2d1b3190944c5", user-agent:"Mozilla/5.0 (compatible; CensysInspect/1.1; +https://about.censys.io/)", accept:"*/*", accept-encoding:"gzip"]]',
    },
    {
      timestamp: 1630435039314,
      message: '--2021-08-31 18:37:19.313 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630435069088,
      message: '--2021-08-31 18:37:49.087 GET /api/v1/device/check?screen=true, headers=[x-forwarded-for:"109.248.6.132", x-forwarded-proto:"https", x-forwarded-port:"443", host:"console-lb-production-347267553.us-east-1.elb.amazonaws.com", x-amzn-trace-id:"Root=1-612e76fd-55cb5c743c3f9af949e4f0ec", user-agent:"masscan-ng/1.3 (https://github.com/bi-zone/masscan-ng)", accept:"*/*", authorization:"masked"]]',
    },
    {
      timestamp: 1630435081044,
      message: '--2021-08-31 18:38:01.043 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630434915899,
      message: '--2021-08-31 18:35:15.898 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630434919198,
      message: '--2021-08-31 18:35:19.198 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630434920418,
      message: '--2021-08-31 18:35:20.418 GET /, headers=[x-forwarded-for:"162.142.125.193", x-forwarded-proto:"https", x-forwarded-port:"443", host:"52.73.13.9", x-amzn-trace-id:"Root=1-612e7668-6d94d63553ec8e902fe72333"]]',
    },
    {
      timestamp: 1630434920537,
      message: '--2021-08-31 18:35:20.537 GET /, headers=[x-forwarded-for:"162.142.125.193", x-forwarded-proto:"https", x-forwarded-port:"443", host:"52.73.13.9", x-amzn-trace-id:"Root=1-612e7668-5b95a03278a2d1b3190944c5", user-agent:"Mozilla/5.0 (compatible; CensysInspect/1.1; +https://about.censys.io/)", accept:"*/*", accept-encoding:"gzip"]]',
    },
    {
      timestamp: 1630435039314,
      message: '--2021-08-31 18:37:19.313 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
    {
      timestamp: 1630435069088,
      message: '--2021-08-31 18:37:49.087 GET /api/v1/device/check?screen=true, headers=[x-forwarded-for:"109.248.6.132", x-forwarded-proto:"https", x-forwarded-port:"443", host:"console-lb-production-347267553.us-east-1.elb.amazonaws.com", x-amzn-trace-id:"Root=1-612e76fd-55cb5c743c3f9af949e4f0ec", user-agent:"masscan-ng/1.3 (https://github.com/bi-zone/masscan-ng)", accept:"*/*", authorization:"masked"]]',
    },
    {
      timestamp: 1630435081044,
      message: '--2021-08-31 18:38:01.043 GET /, headers=[host:"10.0.64.84:32768", connection:"close", accept-encoding:"gzip, compressed"]]',
    },
  ],
};

function respondWithJson(data: any) {
  return (
    req: RestRequest<DefaultRequestBody, RequestParams>,
    resp: ResponseComposition,
    ctx: RestContext,
  ) => resp(
    ctx.json(data),
  );
}

export const handlers = [
  rest.get('/api/v1/organization', respondWithJson(organization)),
  rest.get('/api/v1/project', respondWithJson(projectsArray)),
  rest.get('/api/v1/organization/projects/deployments', respondWithJson(projects)),
  rest.get('/api/v1/project/GhostTrap', respondWithJson(projectWithBuildsAndDeployments)),
  rest.get('/api/v1/project/:projectName/:buildId/logs', respondWithJson(buildLogs)),
  rest.get('/api/v1/project/:projectName/deploy/:buildId/logs', respondWithJson(deployLogs)),
  rest.get('/api/v1/usage', respondWithJson(usage)),
  rest.post(/.*cognito-idp\..*\.amazonaws\.com/, (req, resp, ctx) => resp(
    ctx.status(200),
    ctx.set('Content-Type', 'application/x-amz-json-1.1'),
    ctx.json({
      UserAttributes: [{
        Name: 'custom:organization',
        Value: 'Ghostbusters',
      }, {
        Name: 'sub',
        Value: '19bea27f-6ac9-48ef-ab87-b8353f74f78d',
      }, {
        Name: 'email_verified',
        Value: 'true',
      }, {
        Name: 'name',
        Value: 'Peter Venkman',
      }, {
        Name: 'email',
        Value: 'p.venkman@ghostbusters.com',
      }],
      Username: '19bea27f-6ac9-48ef-ab87-b8353f74f78d',
    }),
  )),
];
