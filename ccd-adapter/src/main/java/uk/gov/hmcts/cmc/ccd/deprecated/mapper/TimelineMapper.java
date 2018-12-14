package uk.gov.hmcts.cmc.ccd.deprecated.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.cmc.ccd.deprecated.domain.CCDCollectionElement;
import uk.gov.hmcts.cmc.ccd.deprecated.domain.CCDTimeline;
import uk.gov.hmcts.cmc.ccd.deprecated.domain.CCDTimelineEvent;
import uk.gov.hmcts.cmc.domain.models.Timeline;

import java.util.Objects;
import java.util.stream.Collectors;

//@Component
public class TimelineMapper implements Mapper<CCDTimeline, Timeline> {

    private final TimelineEventMapper timelineEventMapper;

    @Autowired
    public TimelineMapper(TimelineEventMapper timelineEventMapper) {
        this.timelineEventMapper = timelineEventMapper;
    }

    @Override
    public CCDTimeline to(Timeline timeline) {
        if (timeline == null) {
            return null;
        }
        CCDTimeline.CCDTimelineBuilder builder = CCDTimeline.builder();
        builder.events(
            timeline.getEvents()
                .stream()
                .map(timelineEventMapper::to)
                .filter(Objects::nonNull)
                .map(event -> CCDCollectionElement.<CCDTimelineEvent>builder().value(event).build())
                .collect(Collectors.toList()));

        return builder.build();
    }

    @Override
    public Timeline from(CCDTimeline ccdTimeline) {
        if (ccdTimeline == null) {
            return null;
        }
        return new Timeline(
            ccdTimeline.getEvents().stream()
                .map(CCDCollectionElement::getValue)
                .map(timelineEventMapper::from)
                .collect(Collectors.toList())
        );
    }
}
