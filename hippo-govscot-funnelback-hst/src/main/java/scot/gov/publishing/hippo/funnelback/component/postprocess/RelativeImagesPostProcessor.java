package scot.gov.publishing.hippo.funnelback.component.postprocess;

import org.apache.commons.lang.StringUtils;
import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;
import scot.gov.publishing.hippo.funnelback.model.ListMetadata;
import scot.gov.publishing.hippo.funnelback.model.Result;
import scot.gov.publishing.hippo.funnelback.model.ResultPacket;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.substringAfter;

public class RelativeImagesPostProcessor implements PostProcessor {

    private static final String BINARIES = "/binaries/";
    @Override
    public void process(FunnelbackSearchResponse response) {
        ResultPacket resultPacket = response.getResponse().getResultPacket();
        resultPacket.getResults().stream().filter(this::hasImage).forEach(this::process);
    }

    boolean hasImage(Result result) {
        return result.getListMetadata().getImage() != null;
    }
    void process(Result result) {
        ListMetadata listMetadata = result.getListMetadata();
        List<String> images = listMetadata.getImage().stream()
                .map(this::makeImageRelative)
                .map(this::removeVariantSegmentIfPresent)
                .collect(toList());
        listMetadata.setImage(new ArrayList<>(images));
    }

    String makeImageRelative(String image) {
        if (image.contains(BINARIES)) {
            return new StringBuffer(BINARIES).append(substringAfter(image, BINARIES)).toString();
        } else {
            return image;
        }
    }

    String removeVariantSegmentIfPresent(String image) {
        // images links come back looking like:
        // /binaries/content/gallery/people/nicola-sturgeon.jpg/nicola-sturgeon.jpg/govscot%3Axlargethreecolumnsdoubledsquare
        // we want to strip the image variant part if it exists
        String [] segments = StringUtils.split(image,"/");
        String lastSegment = segments[segments.length - 1];
        return  lastSegment.startsWith("govscot%3") ? StringUtils.substringBeforeLast(image,"/") : image;
    }
}

