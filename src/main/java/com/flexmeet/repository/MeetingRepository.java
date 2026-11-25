package com.flexmeet.repository;

import com.flexmeet.model.entity.Meeting;
import com.flexmeet.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByMeetingCode(String meetingCode);
    List<Meeting> findByHost(User host);
    List<Meeting> findByIsActiveTrue();
    Optional<Meeting> findByFirebaseMeetingRef(String firebaseMeetingRef);
}
