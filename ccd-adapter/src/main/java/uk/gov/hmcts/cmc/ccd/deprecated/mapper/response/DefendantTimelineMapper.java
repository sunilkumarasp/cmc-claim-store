package uk.gov.hmcts.cmc.ccd.deprecated.mapper.response;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.cmc.ccd.deprecated.domain.CCDCollectionElement;
import uk.gov.hmcts.cmc.ccd.deprecated.domain.CCDTimelineEvent;
import uk.gov.hmcts.cmc.ccd.deprecated.domain.response.CCDDefendantTimeline;
import uk.gov.hmcts.cmc.ccd.deprecated.mapper.Mapper;
import uk.gov.hmcts.cmc.ccd.deprecated.mapper.TimelineEventMapper;
import uk.gov.hmcts.cmc.domain.models.response.DefendantTimeline;

import java.util.Objects;
import java.util.stream.Collectors;

//@Component
public class DefendantTimelineMapper implements Mapper<CCDDefendantTimeline, DefendantTimeline> {

    private final TimelineEventMapper timelineEventMapper;

    @Autowired
    public DefendantTimelineMapper(TimelineEventMapper timelineEventMapper) {
        this.timelineEventMapper = timelineEventMapper;
    }

    @Override
    public CCDDefendantTimeline to(DefendantTimeline timeline) {
        if (timeline == null) {
            return null;
        }
        CCDDefendantTimeline.CCDDefendantTimelineBuilder builder = CCDDefendantTimeline.builder();
        builder.events(
            timeline.getEvents()
                .stream()
                .map(timelineEventMapper::to)
                .filter(Objects::nonNull)
                .map(event -> CCDCollectionElement.<CCDTimelineEvent>builder().value(event).build())
                .collect(Collectors.toList()));

        timeline.getComment().ifPresent(builder::comment);

        return builder.build();
    }

    @Override
    public DefendantTimeline from(CCDDefendantTimeline ccdTimeline) {
        if (ccdTimeline == null) {
            return null;
        }
        return new DefendantTimeline(
            ccdTimeline.getEvents().stream()
                .map(CCDCollectionElement::getValue)
                .map(timelineEventMapper::from)
                .collect(Collectors.toList()),
            ccdTimeline.getComment()
        );
    }
}
