package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.junit.Assert;
import org.junit.Test;
import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;
import scot.gov.publishing.hippo.funnelback.model.Result;

import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RelativeImagesPostProcessorTest {

    @Test
    public void binariesImagesMadeRelative() {

        // ARRANGE
        RelativeImagesPostProcessor sut = new RelativeImagesPostProcessor();
        FunnelbackSearchResponse response = new FunnelbackSearchResponse();
        Result result = new Result();
        result.getListMetadata().setImage(singletonList("https://www.gov.scot/binaries/image.jpg"));
        response.getResponse().getResultPacket().getResults().add(result);

        // ACT
        sut.process(response);

        // ASSERT
        assertEquals(singletonList("/binaries/image.jpg"), result.getListMetadata().getImage());
    }

    @Test
    public void nonBinaryImagesUnchanged() {

        // ARRANGE
        RelativeImagesPostProcessor sut = new RelativeImagesPostProcessor();
        FunnelbackSearchResponse response = new FunnelbackSearchResponse();
        Result result = new Result();
        result.getListMetadata().setImage(singletonList("https://www.gov.scot/image.jpg"));
        response.getResponse().getResultPacket().getResults().add(result);

        // ACT
        sut.process(response);

        // ASSERT
        assertEquals(singletonList("https://www.gov.scot/image.jpg"), result.getListMetadata().getImage());
    }

    @Test
    public void variantStripedIfPresent() {

        // ARRANGE
        RelativeImagesPostProcessor sut = new RelativeImagesPostProcessor();
        FunnelbackSearchResponse response = new FunnelbackSearchResponse();
        Result result = new Result();
        result.getListMetadata().setImage(singletonList("https://www.gov.scot/binaries/image.jpg/govscot%3Axlargethreecolumnsdoubledsquare"));
        response.getResponse().getResultPacket().getResults().add(result);

        // ACT
        sut.process(response);

        // ASSERT
        assertEquals(singletonList("/binaries/image.jpg"), result.getListMetadata().getImage());
    }

}
