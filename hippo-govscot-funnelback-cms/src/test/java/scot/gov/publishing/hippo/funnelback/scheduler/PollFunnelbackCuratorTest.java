package scot.gov.publishing.hippo.funnelback.scheduler;

import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static scot.gov.publishing.hippo.funnelback.scheduler.PollFunnelbackCurator.DEFAULT_USER_AGENT;
import static scot.gov.publishing.hippo.funnelback.scheduler.PollFunnelbackCurator.USER_AGENT_PROPERTY;

class PollFunnelbackCuratorTest {

    @Test
    void userAgentFallsBackToDefaultWhenPropertyNotSet() {
        ContainerConfiguration configuration = mock(ContainerConfiguration.class);
        when(configuration.getString(eq(USER_AGENT_PROPERTY), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        PollFunnelbackCurator sut = withConfiguration(configuration);

        assertThat(sut.userAgent()).isEqualTo(DEFAULT_USER_AGENT);
    }

    @Test
    void userAgentUsesConfiguredProperty() {
        ContainerConfiguration configuration = mock(ContainerConfiguration.class);
        when(configuration.getString(eq(USER_AGENT_PROPERTY), anyString())).thenReturn("custom-agent");
        PollFunnelbackCurator sut = withConfiguration(configuration);

        assertThat(sut.userAgent()).isEqualTo("custom-agent");
    }

    PollFunnelbackCurator withConfiguration(ContainerConfiguration configuration) {
        return new PollFunnelbackCurator() {
            @Override
            ContainerConfiguration containerConfiguration() {
                return configuration;
            }
        };
    }
}
