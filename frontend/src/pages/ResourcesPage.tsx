import React from 'react';
import {LoadIndicatingSection, SectionTitle} from 'components/layout/Section';
import Page from 'components/layout/Page';
import {ResourceCard} from 'components/resources/ResourceCard';
import {ResourceType} from 'gen/api';
import {Col, Container, Row} from 'react-bootstrap';
import {Layers} from 'react-feather';
import {resourceApi} from 'service/Services';
import {useQuery, useQueryClient} from 'react-query';

/** Renders a page with all the resources that can be provisioned or that are provisioned. */
export default function ResourcesPage() {
  const queryClient = useQueryClient();
  const resourcesQueryResult = useQuery('resources', () => resourceApi.getResources());

  async function handleProvisionRequest(resourceType: ResourceType) {
    switch (resourceType) {
      case ResourceType.Postgresql:
        await resourceApi.provisionResource(resourceType);
        await queryClient.invalidateQueries('resources');
        break;
      default:
        // do-nothing
    }
  }

  return (
    <Page>
      <LoadIndicatingSection queryResult={resourcesQueryResult} last>
        {(response) => {
          const postgres = response.data!!
            .find((resource) => resource.resourceType === ResourceType.Postgresql);
          return (
            <>
              <SectionTitle icon={Layers} title="Resources" />
              <Container>
                <Row>
                  <Col sm="auto">
                    <ResourceCard
                      resource={ResourceType.Postgresql}
                      title="PostgreSQL"
                      onProvisionRequested={handleProvisionRequest}
                      provisioningStatus={postgres?.status}
                    >
                      PostgreSQL is a powerful, open source object-relational database system
                      with over 30 years of active development that has earned it a strong
                      reputation for reliability, feature robustness, and performance.
                    </ResourceCard>
                    <ResourceCard
                      resource={ResourceType.Mysql}
                      title="MySQL"
                      comingSoon
                    >
                      With more than 20 years of community-backed development and support,
                      MySQL is a reliable, stable, and secure SQL-based database management
                      system.
                    </ResourceCard>
                    <ResourceCard
                      resource={ResourceType.Mongodb}
                      title="MongoDB"
                      comingSoon
                    >
                      MongoDB is a scalable and flexible document database. MongoDB’s document
                      model is simple for developers to learn and use, while still providing
                      all the capabilities needed to meet the most complex requirements at any
                      scale.
                    </ResourceCard>
                    <ResourceCard
                      resource={ResourceType.Redis}
                      title="Redis"
                      comingSoon
                    >
                      Redis is an open source (BSD licensed), in-memory data structure store, used
                      as a database, cache, and message broker. Redis provides data structures
                      such as strings, hashes, lists, sets, sorted sets and more.
                    </ResourceCard>
                  </Col>
                </Row>
              </Container>
            </>
          );
        }}
      </LoadIndicatingSection>
    </Page>
  );
}
