package com.flexmeet.repository;

import com.flexmeet.model.entity.Participant;
import com.flexmeet.model.entity.Meeting;
import com.flexmeet.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByMeeting(Meeting meeting);
    List<Participant> findByMeetingAndLeftAtIsNull(Meeting meeting);
    Optional<Participant> findByParticipantUUID(String participantUUID);
    List<Participant> findByUser(User user);
    Integer countByMeetingAndLeftAtIsNull(Meeting meeting);
}
