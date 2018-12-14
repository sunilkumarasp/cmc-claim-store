package uk.gov.hmcts.cmc.ccd.deprecated.mapper;

import uk.gov.hmcts.cmc.ccd.deprecated.domain.CCDTimelineEvent;
import uk.gov.hmcts.cmc.domain.models.TimelineEvent;

//@Component
public class TimelineEventMapper implements Mapper<CCDTimelineEvent, TimelineEvent> {

    @Override
    public CCDTimelineEvent to(TimelineEvent timelineEvent) {
        CCDTimelineEvent.CCDTimelineEventBuilder builder = CCDTimelineEvent.builder();
        return
            builder.date(timelineEvent.getDate())
                .description(timelineEvent.getDescription())
                .build();
    }

    @Override
    public TimelineEvent from(CCDTimelineEvent ccdTimelineEvent) {
        return new TimelineEvent(ccdTimelineEvent.getDate(), ccdTimelineEvent.getDescription());
    }
}
