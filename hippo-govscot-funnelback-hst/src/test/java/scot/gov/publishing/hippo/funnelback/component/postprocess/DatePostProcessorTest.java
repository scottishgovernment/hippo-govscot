package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.junit.Test;
import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;
import scot.gov.publishing.hippo.funnelback.model.Result;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DatePostProcessorTest {

    @Test
    public void ignoresNullD() {
        // ARRANGE
        DatePostProcessor sut = new DatePostProcessor();
        FunnelbackSearchResponse response = new FunnelbackSearchResponse();
        Result result = new Result();
        result.getListMetadata().setD(null);
        response.getResponse().getResultPacket().getResults().add(result);

        // ACT
        sut.process(response);

        // ASSERT
        assertNull(response.getResponse().getResultPacket().getResults().get(0).getListMetadata().getDisplayDate());
        assertNull(response.getResponse().getResultPacket().getResults().get(0).getListMetadata().getDisplayDateTime());
    }

    @Test
    public void ignoresEmptyD() {
        // ARRANGE
        DatePostProcessor sut = new DatePostProcessor();
        FunnelbackSearchResponse response = new FunnelbackSearchResponse();
        Result result = new Result();
        result.getListMetadata().setD(Collections.singletonList(""));
        response.getResponse().getResultPacket().getResults().add(result);

        // ACT
        sut.process(response);

        // ASSERT
        assertNull(response.getResponse().getResultPacket().getResults().get(0).getListMetadata().getDisplayDate());
        assertNull(response.getResponse().getResultPacket().getResults().get(0).getListMetadata().getDisplayDateTime());
    }

    @Test
    public void setsDisplayDateFeilds() {
        // ARRANGE
        DatePostProcessor sut = new DatePostProcessor();
        FunnelbackSearchResponse response = new FunnelbackSearchResponse();
        Result result = new Result();
        result.getListMetadata().setD(Collections.singletonList("2021-10-29 09:49"));
        response.getResponse().getResultPacket().getResults().add(result);

        // ACT
        sut.process(response);

        // ASSERT
        assertEquals("29 October 2021", response.getResponse().getResultPacket().getResults().get(0).getListMetadata().getDisplayDate());
        assertEquals("29 October 2021 09:49", response.getResponse().getResultPacket().getResults().get(0).getListMetadata().getDisplayDateTime());
    }
}
