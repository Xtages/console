# Docker image to build console
FROM amazonlinux:2018.03

ARG JAVA_VER=15
ENV JAVA_VER=${JAVA_VER}
ARG GRADLE_VER=6.8.3
ENV GRADLE_VER=${GRADLE_VER}
ARG TERRAFORM_VER=0.14.7
ENV TERRAFORM_VER=${TERRAFORM_VER}
ARG NODE_VER=15.13.0
ENV NODE_VER=${NODE_VER}
ARG NVM_VER=0.38.0
ENV NVM_VER=${NVM_VER}

# JAVA install
RUN rpm --import https://yum.corretto.aws/corretto.key
RUN curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo

RUN yum -y install git unzip java-${JAVA_VER}-amazon-corretto-devel shadow-utils
ENV JAVA_HOME=/usr/lib/jvm/java-${JAVA_VER}-amazon-corretto/

# Gradle install
RUN curl -L -O https://services.gradle.org/distributions/gradle-${GRADLE_VER}-all.zip
RUN unzip -q gradle-${GRADLE_VER}-all.zip -d /opt && rm *.zip
RUN ln -s /opt/gradle-${GRADLE_VER} /opt/gradle
ENV GRADLE_HOME=/opt/gradle
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

# Terraform install
RUN curl -L -O https://releases.hashicorp.com/terraform/${TERRAFORM_VER}/terraform_${TERRAFORM_VER}_linux_amd64.zip
RUN unzip terraform_${TERRAFORM_VER}_linux_amd64.zip -d /usr/sbin && rm *.zip

# NodeJS install
RUN touch ~/.bashrc && chmod +x ~/.bashrc
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v${NVM_VER}/install.sh | bash
RUN source ~/.bashrc && nvm install ${NODE_VER}

# AWS CLI install
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64-2.1.36.zip" -o "awscliv2.zip"
RUN unzip -q awscliv2.zip && rm *.zip
RUN ./aws/install

# Tried to run the container with a user but there was to run the container with the docker privileges
#RUN useradd -ms /bin/bash builder
#ENV GRADLE_USER_HOME=/home/builder
#USER builder
