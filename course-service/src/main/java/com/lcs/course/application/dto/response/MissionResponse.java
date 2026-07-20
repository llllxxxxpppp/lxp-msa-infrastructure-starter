package com.lcs.course.application.dto.response;

import com.lcs.course.domain.model.entity.Mission;

public record MissionResponse(Long missionId, String title, String status, int sortOrder) {

    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.getId().value(),
                mission.getTitle().getValue(),
                mission.getStatus().name(),
                mission.getSortOrder());
    }
}
