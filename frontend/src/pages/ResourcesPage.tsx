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
                  <Col sm={10}>
                    <ResourceCard
                      resource={ResourceType.Postgresql}
                      title="PostgreSQL"
                      onProvisionRequested={handleProvisionRequest}
                      provisioningStatus={postgres?.status}
                    >
                      <p className="prose">
                        PostgreSQL is a powerful, open source object-relational database system
                        with over 30 years of active development that has earned it a strong
                        reputation for reliability, feature robustness, and performance.
                      </p>
                    </ResourceCard>
                    <ResourceCard
                      resource={ResourceType.Mysql}
                      title="MySQL"
                      comingSoon
                    >
                      <p className="prose">
                        With more than 20 years of community-backed development and support,
                        MySQL is a reliable, stable, and secure SQL-based database management
                        system.
                      </p>
                    </ResourceCard>
                    <ResourceCard
                      resource={ResourceType.Mongodb}
                      title="MongoDB"
                      comingSoon
                    >
                      <p className="prose">
                        MongoDB is a scalable and flexible document database. MongoDB’s document
                        model is simple for developers to learn and use, while still providing
                        all the capabilities needed to meet the most complex requirements at any
                        scale.
                      </p>
                    </ResourceCard>
                    <ResourceCard
                      resource={ResourceType.Redis}
                      title="Redis"
                      comingSoon
                    >
                      <p className="prose">
                        Redis is an open source (BSD licensed), in-memory data structure store, used
                        as a database, cache, and message broker. Redis provides data structures
                        such as strings, hashes, lists, sets, sorted sets and more.
                      </p>
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
