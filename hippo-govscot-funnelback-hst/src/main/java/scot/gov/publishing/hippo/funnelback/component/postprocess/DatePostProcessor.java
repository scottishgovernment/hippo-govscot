package scot.gov.publishing.hippo.funnelback.component.postprocess;

import scot.gov.publishing.hippo.funnelback.model.FunnelbackSearchResponse;
import scot.gov.publishing.hippo.funnelback.model.ListMetadata;
import scot.gov.publishing.hippo.funnelback.model.Result;
import scot.gov.publishing.hippo.funnelback.model.ResultPacket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class DatePostProcessor implements PostProcessor {

    private static DateTimeFormatter FROM_DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().append(ISO_LOCAL_DATE).appendLiteral(' ').append(ISO_LOCAL_TIME).toFormatter();

    private static DateTimeFormatter DISPLAY_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");

    private static DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    @Override
    public void process(FunnelbackSearchResponse response) {
        ResultPacket resultPacket = response.getResponse().getResultPacket();
        resultPacket.getResults().stream().filter(this::hasD).forEach(this::process);
    }

    boolean hasD(Result result) {
        return result.getListMetadata().getD() != null
                && !result.getListMetadata().getD().isEmpty()
                && isNotBlank(result.getListMetadata().getD().get(0));
    }

    void process(Result result) {
        ListMetadata listMetadata = result.getListMetadata();
        String dateString = listMetadata.getD().get(0);
        LocalDateTime parsedDatetime = parseD(dateString);
        listMetadata.setDisplayDate(DISPLAY_DATE_FORMAT.format(parsedDatetime));
        listMetadata.setDisplayDateTime(DISPLAY_DATE_TIME_FORMAT.format(parsedDatetime));
    }

    LocalDateTime parseD(String str) {
        return LocalDateTime.parse(str, FROM_DATE_TIME_FORMAT);
    }

}
