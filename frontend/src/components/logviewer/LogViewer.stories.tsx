import React from 'react';
import {Meta, Story} from '@storybook/react';
import {LogViewer, LogViewerProps} from './LogViewer';

export default {
  title: 'Xtages/LogViewer',
  component: LogViewer,
} as Meta;

const Template: Story<LogViewerProps> = (args) => (
  <div style={{height: '300px'}}>
    <LogViewer {...args} />
  </div>
);

export const Primary = Template.bind({});
Primary.args = {
  logLines: [
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:35 Waiting for agent ping\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:38 Waiting for DOWNLOAD_SOURCE\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:38 Phase is DOWNLOAD_SOURCE\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:38 CODEBUILD_SRC_DIR=/codebuild/output/src288344185/src\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:38 YAML location is /tmp/buildspec-652581252.yml\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:38 Processing environment variables\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Moving to directory /codebuild/output/src288344185/src\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Registering with agent\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Phases found in YAML: 2\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39  PRE_BUILD: 2 commands\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39  BUILD: 5 commands\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Phase complete: DOWNLOAD_SOURCE State: SUCCEEDED\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Phase context status code:  Message: \n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Entering phase INSTALL\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Phase complete: INSTALL State: SUCCEEDED\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Phase context status code:  Message: \n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Entering phase PRE_BUILD\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Running command nohup /usr/bin/dockerd --host=unix:///var/run/docker.sock --host=tcp://127.0.0.1:2375 --storage-driver=overlay2 &\n',
    },
    {
      timestamp: 1622151824967,
      message: '\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:39 Running command aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 606626603369.dkr.ecr.us-east-1.amazonaws.com\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:39.613581240Z" level=info msg="Starting up"\n',
    },
    {
      timestamp: 1622151824967,
      message: "time=\"2021-05-27T21:43:39.617657997Z\" level=warning msg=\"[!] DON'T BIND ON ANY IP ADDRESS WITHOUT setting --tlsverify IF YOU DON'T KNOW WHAT YOU'RE DOING [!]\"\n",
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:39.632195046Z" level=info msg="libcontainerd: started new containerd process" pid=87\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:39.632255122Z" level=info msg="parsed scheme: \\"unix\\"" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:39.632268190Z" level=info msg="scheme \\"unix\\" not registered, fallback to default scheme" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:39.632294974Z" level=info msg="ccResolverWrapper: sending update to cc: {[{unix:///var/run/docker/containerd/containerd.sock 0  <nil>}] <nil>}" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:39.632313822Z" level=info msg="ClientConn switching balancer to \\"pick_first\\"" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.032967955Z" level=info msg="starting containerd" revision=c623d1b36f09f8ef6536a057bd658b3aa8632828 version=1.4.1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.055488889Z" level=info msg="loading plugin \\"io.containerd.content.v1.content\\"..." type=io.containerd.content.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.056149961Z" level=info msg="loading plugin \\"io.containerd.snapshotter.v1.aufs\\"..." type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.064802157Z" level=info msg="skip loading plugin \\"io.containerd.snapshotter.v1.aufs\\"..." error="aufs is not supported (modprobe aufs failed: exit status 1 \\"modprobe: FATAL: Module aufs not found.\\\\n\\"): skip plugin" type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.064828131Z" level=info msg="loading plugin \\"io.containerd.snapshotter.v1.btrfs\\"..." type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.064984811Z" level=info msg="skip loading plugin \\"io.containerd.snapshotter.v1.btrfs\\"..." error="path /var/lib/docker/containerd/daemon/io.containerd.snapshotter.v1.btrfs (ext4) must be a btrfs filesystem to be used with the btrfs snapshotter: skip plugin" type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.064998443Z" level=info msg="loading plugin \\"io.containerd.snapshotter.v1.devmapper\\"..." type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.065012556Z" level=warning msg="failed to load plugin io.containerd.snapshotter.v1.devmapper" error="devmapper not configured"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.065020954Z" level=info msg="loading plugin \\"io.containerd.snapshotter.v1.native\\"..." type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.065059735Z" level=info msg="loading plugin \\"io.containerd.snapshotter.v1.overlayfs\\"..." type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.065159980Z" level=info msg="loading plugin \\"io.containerd.snapshotter.v1.zfs\\"..." type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.065287743Z" level=info msg="skip loading plugin \\"io.containerd.snapshotter.v1.zfs\\"..." error="path /var/lib/docker/containerd/daemon/io.containerd.snapshotter.v1.zfs must be a zfs filesystem to be used with the zfs snapshotter: skip plugin" type=io.containerd.snapshotter.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.065300128Z" level=info msg="loading plugin \\"io.containerd.metadata.v1.bolt\\"..." type=io.containerd.metadata.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.065328763Z" level=warning msg="could not use snapshotter devmapper in metadata plugin" error="devmapper not configured"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.065340394Z" level=info msg="metadata content store policy set" policy=shared\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080281343Z" level=info msg="loading plugin \\"io.containerd.differ.v1.walking\\"..." type=io.containerd.differ.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080306229Z" level=info msg="loading plugin \\"io.containerd.gc.v1.scheduler\\"..." type=io.containerd.gc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080338747Z" level=info msg="loading plugin \\"io.containerd.service.v1.introspection-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080365828Z" level=info msg="loading plugin \\"io.containerd.service.v1.containers-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080376328Z" level=info msg="loading plugin \\"io.containerd.service.v1.content-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080387185Z" level=info msg="loading plugin \\"io.containerd.service.v1.diff-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080403770Z" level=info msg="loading plugin \\"io.containerd.service.v1.images-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080421040Z" level=info msg="loading plugin \\"io.containerd.service.v1.leases-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080437723Z" level=info msg="loading plugin \\"io.containerd.service.v1.namespaces-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080449199Z" level=info msg="loading plugin \\"io.containerd.service.v1.snapshots-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080459297Z" level=info msg="loading plugin \\"io.containerd.runtime.v1.linux\\"..." type=io.containerd.runtime.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080546417Z" level=info msg="loading plugin \\"io.containerd.runtime.v2.task\\"..." type=io.containerd.runtime.v2\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080600599Z" level=info msg="loading plugin \\"io.containerd.monitor.v1.cgroups\\"..." type=io.containerd.monitor.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080855607Z" level=info msg="loading plugin \\"io.containerd.service.v1.tasks-service\\"..." type=io.containerd.service.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080875042Z" level=info msg="loading plugin \\"io.containerd.internal.v1.restart\\"..." type=io.containerd.internal.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080909999Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.containers\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080920408Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.content\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080933163Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.diff\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080942181Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.events\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080951318Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.healthcheck\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080960977Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.images\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080970017Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.leases\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080979395Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.namespaces\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.080988457Z" level=info msg="loading plugin \\"io.containerd.internal.v1.opt\\"..." type=io.containerd.internal.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.081354931Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.snapshots\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.081372659Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.tasks\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.081387385Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.version\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.081402683Z" level=info msg="loading plugin \\"io.containerd.grpc.v1.introspection\\"..." type=io.containerd.grpc.v1\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.081878893Z" level=info msg=serving... address=/var/run/docker/containerd/containerd-debug.sock\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.081910295Z" level=info msg=serving... address=/var/run/docker/containerd/containerd.sock.ttrpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.081946400Z" level=info msg=serving... address=/var/run/docker/containerd/containerd.sock\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.081961297Z" level=info msg="containerd successfully booted in 0.050848s"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.091731517Z" level=info msg="parsed scheme: \\"unix\\"" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.091749390Z" level=info msg="scheme \\"unix\\" not registered, fallback to default scheme" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.091764649Z" level=info msg="ccResolverWrapper: sending update to cc: {[{unix:///var/run/docker/containerd/containerd.sock 0  <nil>}] <nil>}" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.091772193Z" level=info msg="ClientConn switching balancer to \\"pick_first\\"" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.092972103Z" level=info msg="parsed scheme: \\"unix\\"" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.092987915Z" level=info msg="scheme \\"unix\\" not registered, fallback to default scheme" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.092999403Z" level=info msg="ccResolverWrapper: sending update to cc: {[{unix:///var/run/docker/containerd/containerd.sock 0  <nil>}] <nil>}" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.093011314Z" level=info msg="ClientConn switching balancer to \\"pick_first\\"" module=grpc\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.382348427Z" level=warning msg="Your kernel does not support cgroup blkio weight"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.382367742Z" level=warning msg="Your kernel does not support cgroup blkio weight_device"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.382504401Z" level=info msg="Loading containers: start."\n',
    },
    {
      timestamp: 1622151824967,
      message: '\n',
    },
    {
      timestamp: 1622151824967,
      message: 'An error occurred (AccessDeniedException) when calling the GetAuthorizationToken operation: User: arn:aws:sts::606626603369:assumed-role/xtages-codebuild-cd-role/AWSCodeBuild-43442676-777b-4c29-9501-0ba1ad4fdfa6 is not authorized to perform: ecr:GetAuthorizationToken on resource: *\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.601596230Z" level=info msg="Default bridge (docker0) is assigned with an IP address 172.18.0.0/16. Daemon option --bip can be used to set a preferred IP address"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.804176779Z" level=info msg="Loading containers: done."\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.877148070Z" level=info msg="Docker daemon" commit=4484c46 graphdriver(s)=overlay2 version=19.03.13-ce\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.877578031Z" level=info msg="Daemon has completed initialization"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.907174266Z" level=info msg="API listen on 127.0.0.1:2375"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'time="2021-05-27T21:43:40.907718211Z" level=info msg="API listen on /var/run/docker.sock"\n',
    },
    {
      timestamp: 1622151824967,
      message: 'Error: Cannot perform an interactive login from a non TTY device\n',
    },
    {
      timestamp: 1622151824967,
      message: '\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:40 Command did not exit successfully aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 606626603369.dkr.ecr.us-east-1.amazonaws.com exit status 1\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:40 Phase complete: PRE_BUILD State: FAILED\n',
    },
    {
      timestamp: 1622151824967,
      message: '[Container] 2021/05/27 21:43:40 Phase context status code: COMMAND_EXECUTION_ERROR Message: Error while executing command: aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 606626603369.dkr.ecr.us-east-1.amazonaws.com. Reason: exit status 1\n',
    },
  ],
};
Primary.storyName = 'LogViewer';
