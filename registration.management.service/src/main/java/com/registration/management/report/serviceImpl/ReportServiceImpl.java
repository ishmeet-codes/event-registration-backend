package com.registration.management.report.serviceImpl;

import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.checkin.entity.Checkin;
import com.registration.management.checkin.enums.CheckinStatus;
import com.registration.management.checkin.repository.CheckinRepository;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.repository.RegistrationRepository;
import com.registration.management.report.dto.*;
import com.registration.management.report.service.ReportService;
import com.registration.management.school.entity.School;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final RegistrationRepository registrationRepository;
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final schoolRepository schoolRepository;
    private final CheckinRepository checkinRepository;
    private final schoolStaffRepository schoolStaffRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardReportDTO getDashboardReport(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);

        List<Registration> registrations = getAllRegistrations(restrictedSchoolId);
        List<Participant> participants = getAllParticipants(restrictedSchoolId);
        List<Checkin> checkins = getAllCheckins(restrictedSchoolId);

        long totalRegistrations = registrations.size();
        long totalParticipants = participants.size();
        long totalCheckins = checkins.stream().filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN).count();
        long totalSchools = restrictedSchoolId != null ? 1 : schoolRepository.count();
        long totalEvents = eventRepository.count();

        Map<String, Long> regStatusMap = registrations.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        Map<String, Long> checkinStatusMap = new HashMap<>();
        checkinStatusMap.put("CHECKED_IN", totalCheckins);
        checkinStatusMap.put("CHECKED_OUT", checkins.stream().filter(c -> c.getStatus() == CheckinStatus.CHECKED_OUT).count());
        checkinStatusMap.put("NOT_CHECKED_IN", Math.max(0, totalParticipants - checkins.size()));

        List<EventSummaryReportDTO> topEvents = getEventSummaryList(restrictedSchoolId).stream()
                .sorted(Comparator.comparingLong((EventSummaryReportDTO dto) -> dto.getRegisteredCount()).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<SchoolSummaryReportDTO> topSchools = getSchoolSummaryList(restrictedSchoolId).stream()
                .sorted(Comparator.comparingLong((SchoolSummaryReportDTO dto) -> dto.getParticipantCount()).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return DashboardReportDTO.builder()
                .totalRegistrations(totalRegistrations)
                .totalParticipants(totalParticipants)
                .totalCheckins(totalCheckins)
                .totalSchools(totalSchools)
                .totalEvents(totalEvents)
                .registrationStatusBreakdown(regStatusMap)
                .checkinStatusBreakdown(checkinStatusMap)
                .topEvents(topEvents)
                .topSchools(topSchools)
                .build();
    }

    @Override
    public OverviewReportDTO getOverviewReport(LocalDate startDate, LocalDate endDate, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);

        List<Registration> registrations = getAllRegistrations(restrictedSchoolId);
        if (startDate != null) {
            registrations = registrations.stream()
                    .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }
        if (endDate != null) {
            registrations = registrations.stream()
                    .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        long totalRegistrations = registrations.size();
        long confirmed = registrations.stream().filter(r -> r.getStatus() == RegistrationStatus.APPROVED || r.getStatus() == RegistrationStatus.SUBMITTED).count();
        long pending = registrations.stream().filter(r -> r.getStatus() == RegistrationStatus.DRAFT || r.getStatus() == RegistrationStatus.PENDING).count();
        double totalRevenue = 0.0;

        List<Participant> participants = getAllParticipants(restrictedSchoolId);
        long totalParticipants = participants.size();

        List<Checkin> checkins = getAllCheckins(restrictedSchoolId);
        long checkedInCount = checkins.stream().filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN).count();
        double attendanceRate = totalParticipants > 0 ? ((double) checkedInCount / totalParticipants) * 100.0 : 0.0;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, Long> dailyRegMap = registrations.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(r -> r.getCreatedAt().toLocalDate().format(formatter), Collectors.counting()));

        List<DailyMetricDTO> dailyRegistrations = dailyRegMap.entrySet().stream()
                .map(e -> new DailyMetricDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing((DailyMetricDTO dto) -> dto.getDate()))
                .collect(Collectors.toList());

        Map<String, Long> dailyCheckinMap = checkins.stream()
                .filter(c -> c.getCheckedInAt() != null)
                .collect(Collectors.groupingBy(c -> c.getCheckedInAt().toLocalDate().format(formatter), Collectors.counting()));

        List<DailyMetricDTO> dailyAttendance = dailyCheckinMap.entrySet().stream()
                .map(e -> new DailyMetricDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing((DailyMetricDTO dto) -> dto.getDate()))
                .collect(Collectors.toList());

        return OverviewReportDTO.builder()
                .totalRegistrations(totalRegistrations)
                .confirmedRegistrations(confirmed)
                .pendingRegistrations(pending)
                .totalRevenue(totalRevenue)
                .totalParticipants(totalParticipants)
                .overallAttendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                .dailyRegistrations(dailyRegistrations)
                .dailyAttendance(dailyAttendance)
                .build();
    }

    @Override
    public List<RegistrationReportDTO> getRegistrationReports(String status, Long schoolId, Long eventId, LocalDate startDate, LocalDate endDate, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        validateSchoolAccess(schoolId, user);

        Long effectiveSchoolId = getEffectiveSchoolId(schoolId, user);
        List<Registration> registrations = getAllRegistrations(effectiveSchoolId);

        if (status != null && !status.trim().isEmpty()) {
            registrations = registrations.stream()
                    .filter(r -> r.getStatus().name().equalsIgnoreCase(status.trim()))
                    .collect(Collectors.toList());
        }

        if (eventId != null) {
            registrations = registrations.stream()
                    .filter(r -> r.getRegistrationEvents() != null && r.getRegistrationEvents().stream()
                            .anyMatch(re -> re.getEvent() != null && re.getEvent().getId().equals(eventId)))
                    .collect(Collectors.toList());
        }

        if (startDate != null) {
            registrations = registrations.stream()
                    .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }
        if (endDate != null) {
            registrations = registrations.stream()
                    .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        return registrations.stream().map(this::toRegistrationReportDTO).collect(Collectors.toList());
    }

    @Override
    public RegistrationSummaryDTO getRegistrationSummary(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);
        List<Registration> registrations = getAllRegistrations(restrictedSchoolId);

        Map<String, Long> byStatus = registrations.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        Map<String, Long> byPayment = registrations.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus() == RegistrationStatus.APPROVED ? "PAID" : "UNPAID", Collectors.counting()));

        return RegistrationSummaryDTO.builder()
                .totalRegistrations((long) registrations.size())
                .byStatus(byStatus)
                .byPaymentStatus(byPayment)
                .totalFeesCollected(0.0)
                .totalFeesPending(0.0)
                .build();
    }

    @Override
    public RegistrationReportDTO getRegistrationReportById(Long registrationId, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found: " + registrationId));

        if (registration.getSchool() != null) {
            validateSchoolAccess(registration.getSchool().getId(), user);
        }

        return toRegistrationReportDTO(registration);
    }

    @Override
    public List<ParticipantReportDTO> getParticipantReports(String gender, Long schoolId, Long eventId, String search, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        validateSchoolAccess(schoolId, user);

        Long effectiveSchoolId = getEffectiveSchoolId(schoolId, user);
        List<Participant> participants = getAllParticipants(effectiveSchoolId);

        if (gender != null && !gender.trim().isEmpty()) {
            participants = participants.stream()
                    .filter(p -> p.getGender() != null && p.getGender().name().equalsIgnoreCase(gender.trim()))
                    .collect(Collectors.toList());
        }

        if (eventId != null) {
            participants = participants.stream()
                    .filter(p -> p.getParticipantEvents() != null && p.getParticipantEvents().stream()
                            .anyMatch(pe -> pe.getEvent() != null && pe.getEvent().getId().equals(eventId)))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            participants = participants.stream()
                    .filter(p -> (p.getFullName() != null && p.getFullName().toLowerCase().contains(q)) ||
                                 (p.getClassName() != null && p.getClassName().toLowerCase().contains(q)) ||
                                 (p.getRegistration() != null && getRegistrationNumber(p.getRegistration()).toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        return participants.stream().map(this::toParticipantReportDTO).collect(Collectors.toList());
    }

    @Override
    public ParticipantSummaryDTO getParticipantSummary(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);
        List<Participant> participants = getAllParticipants(restrictedSchoolId);

        Map<String, Long> byGender = participants.stream()
                .collect(Collectors.groupingBy(p -> p.getGender() != null ? p.getGender().name() : "UNKNOWN", Collectors.counting()));

        Map<String, Long> byClass = participants.stream()
                .collect(Collectors.groupingBy(p -> p.getClassName() != null ? p.getClassName() : "UNSPECIFIED", Collectors.counting()));

        return ParticipantSummaryDTO.builder()
                .totalParticipants((long) participants.size())
                .byGender(byGender)
                .byClass(byClass)
                .build();
    }

    @Override
    public List<EventReportDTO> getEventReports(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);
        List<Event> events = eventRepository.findAll();

        return events.stream().map(e -> {
            long registeredCount = registrationRepository.countByEventId(e.getId());
            long checkedIn = checkinRepository.countByEventIdAndStatus(e.getId(), CheckinStatus.CHECKED_IN);
            if (restrictedSchoolId != null) {
                registeredCount = registrationRepository.countBySchoolIdAndEventId(restrictedSchoolId, e.getId());
                checkedIn = checkinRepository.findByEventId(e.getId()).stream()
                        .filter(c -> c.getSchool() != null && c.getSchool().getId().equals(restrictedSchoolId) && c.getStatus() == CheckinStatus.CHECKED_IN)
                        .count();
            }
            double rate = registeredCount > 0 ? ((double) checkedIn / registeredCount) * 100.0 : 0.0;
            return EventReportDTO.builder()
                    .eventId(e.getId())
                    .eventCode(getEventCode(e))
                    .title(e.getEventName())
                    .categoryName(e.getParticipationCategory() != null ? e.getParticipationCategory().getDisplayName() : null)
                    .maxParticipants(e.getMaxRegistrations())
                    .totalRegistered(registeredCount)
                    .totalCheckedIn(checkedIn)
                    .attendanceRate(Math.round(rate * 100.0) / 100.0)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public IndividualEventReportDTO getEventReportById(Long eventId, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));

        List<Participant> participants = getAllParticipants(restrictedSchoolId).stream()
                .filter(p -> p.getParticipantEvents() != null && p.getParticipantEvents().stream()
                        .anyMatch(pe -> pe.getEvent() != null && pe.getEvent().getId().equals(eventId)))
                .collect(Collectors.toList());

        long totalRegistered = participants.size();
        long totalCheckedIn = checkinRepository.findByEventId(eventId).stream()
                .filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN)
                .filter(c -> restrictedSchoolId == null || (c.getSchool() != null && c.getSchool().getId().equals(restrictedSchoolId)))
                .count();

        long schoolCount = participants.stream()
                .map(p -> p.getRegistration() != null ? p.getRegistration().getSchool() : null)
                .filter(Objects::nonNull)
                .map(school -> school.getId())
                .distinct()
                .count();

        double rate = totalRegistered > 0 ? ((double) totalCheckedIn / totalRegistered) * 100.0 : 0.0;

        List<ParticipantReportDTO> participantDTOs = participants.stream()
                .map(this::toParticipantReportDTO)
                .collect(Collectors.toList());

        return IndividualEventReportDTO.builder()
                .eventId(event.getId())
                .eventCode(getEventCode(event))
                .title(event.getEventName())
                .categoryName(event.getParticipationCategory() != null ? event.getParticipationCategory().getDisplayName() : null)
                .eventDate(event.getEventDate() != null ? event.getEventDate().atStartOfDay() : null)
                .venue(event.getVenue())
                .totalRegistered(totalRegistered)
                .totalCheckedIn(totalCheckedIn)
                .attendanceRate(Math.round(rate * 100.0) / 100.0)
                .participatingSchoolsCount(schoolCount)
                .participants(participantDTOs)
                .build();
    }

    @Override
    public EventSummaryReportDTO getEventSummary(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);
        List<EventSummaryReportDTO> summaries = getEventSummaryList(restrictedSchoolId);

        long totalRegistered = summaries.stream().mapToLong(dto -> dto.getRegisteredCount()).sum();
        long totalCheckedIn = summaries.stream().mapToLong(dto -> dto.getCheckedInCount()).sum();

        return EventSummaryReportDTO.builder()
                .eventId(null)
                .title("All Events Aggregated Summary")
                .eventCode("ALL")
                .registeredCount(totalRegistered)
                .checkedInCount(totalCheckedIn)
                .build();
    }

    @Override
    public List<SchoolReportDTO> getSchoolReports(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);

        List<School> schools = schoolRepository.findAll();
        if (restrictedSchoolId != null) {
            schools = schools.stream().filter(s -> s.getId().equals(restrictedSchoolId)).collect(Collectors.toList());
        }

        return schools.stream().map(school -> {
            long totalRegs = registrationRepository.countBySchoolId(school.getId());
            List<Participant> participants = getAllParticipants(school.getId());
            long totalParts = participants.size();
            long checkedIn = checkinRepository.findBySchoolId(school.getId()).stream()
                    .filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN)
                    .count();
            double rate = totalParts > 0 ? ((double) checkedIn / totalParts) * 100.0 : 0.0;

            return SchoolReportDTO.builder()
                    .schoolId(school.getId())
                    .schoolCode(school.getSchoolCode())
                    .schoolName(school.getSchoolName())
                    .city(school.getCity())
                    .totalRegistrations(totalRegs)
                    .totalParticipants(totalParts)
                    .totalCheckedIn(checkedIn)
                    .attendanceRate(Math.round(rate * 100.0) / 100.0)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public IndividualSchoolReportDTO getSchoolReportById(Long schoolId, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        validateSchoolAccess(schoolId, user);

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NoSuchElementException("School not found: " + schoolId));

        List<Registration> registrations = getAllRegistrations(schoolId);
        List<Participant> participants = getAllParticipants(schoolId);

        long totalRegs = registrations.size();
        long totalParts = participants.size();
        long checkedIn = checkinRepository.findBySchoolId(schoolId).stream()
                .filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN)
                .count();
        double rate = totalParts > 0 ? ((double) checkedIn / totalParts) * 100.0 : 0.0;

        List<RegistrationReportDTO> regDTOs = registrations.stream().map(this::toRegistrationReportDTO).collect(Collectors.toList());
        List<ParticipantReportDTO> partDTOs = participants.stream().map(this::toParticipantReportDTO).collect(Collectors.toList());

        return IndividualSchoolReportDTO.builder()
                .schoolId(school.getId())
                .schoolCode(school.getSchoolCode())
                .schoolName(school.getSchoolName())
                .city(school.getCity())
                .totalRegistrations(totalRegs)
                .totalParticipants(totalParts)
                .totalCheckedIn(checkedIn)
                .attendanceRate(Math.round(rate * 100.0) / 100.0)
                .registrations(regDTOs)
                .participants(partDTOs)
                .build();
    }

    @Override
    public AttendanceReportDTO getAttendanceReport(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);

        List<Participant> participants = getAllParticipants(restrictedSchoolId);
        List<Checkin> checkins = getAllCheckins(restrictedSchoolId);

        long totalParticipants = participants.size();
        long checkedIn = checkins.stream().filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN).count();
        long checkedOut = checkins.stream().filter(c -> c.getStatus() == CheckinStatus.CHECKED_OUT).count();
        long notCheckedIn = Math.max(0, totalParticipants - checkins.size());
        double rate = totalParticipants > 0 ? ((double) checkedIn / totalParticipants) * 100.0 : 0.0;

        return AttendanceReportDTO.builder()
                .totalParticipants(totalParticipants)
                .checkedInCount(checkedIn)
                .checkedOutCount(checkedOut)
                .notCheckedInCount(notCheckedIn)
                .overallAttendanceRate(Math.round(rate * 100.0) / 100.0)
                .build();
    }

    @Override
    public AttendanceSummaryReportDTO getAttendanceSummary(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);

        List<Participant> participants = getAllParticipants(restrictedSchoolId);
        List<Checkin> checkins = getAllCheckins(restrictedSchoolId);

        long expected = participants.size();
        long present = checkins.stream().filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN).count();
        long absent = Math.max(0, expected - present);
        double rate = expected > 0 ? ((double) present / expected) * 100.0 : 0.0;

        return AttendanceSummaryReportDTO.builder()
                .totalExpected(expected)
                .totalPresent(present)
                .totalAbsent(absent)
                .attendancePercentage(Math.round(rate * 100.0) / 100.0)
                .build();
    }

    @Override
    public EventAttendanceReportDTO getEventAttendanceReport(Long eventId, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));

        List<Participant> participants = getAllParticipants(restrictedSchoolId).stream()
                .filter(p -> p.getParticipantEvents() != null && p.getParticipantEvents().stream()
                        .anyMatch(pe -> pe.getEvent() != null && pe.getEvent().getId().equals(eventId)))
                .collect(Collectors.toList());

        long registered = participants.size();
        long present = checkinRepository.findByEventId(eventId).stream()
                .filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN)
                .filter(c -> restrictedSchoolId == null || (c.getSchool() != null && c.getSchool().getId().equals(restrictedSchoolId)))
                .count();

        double rate = registered > 0 ? ((double) present / registered) * 100.0 : 0.0;

        return EventAttendanceReportDTO.builder()
                .eventId(event.getId())
                .eventTitle(event.getEventName())
                .totalRegistered(registered)
                .totalPresent(present)
                .attendanceRate(Math.round(rate * 100.0) / 100.0)
                .build();
    }

    @Override
    public SchoolAttendanceReportDTO getSchoolAttendanceReport(Long schoolId, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        validateSchoolAccess(schoolId, user);

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NoSuchElementException("School not found: " + schoolId));

        List<Participant> participants = getAllParticipants(school.getId());
        long registered = participants.size();
        long present = checkinRepository.findBySchoolId(school.getId()).stream()
                .filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN)
                .count();

        double rate = registered > 0 ? ((double) present / registered) * 100.0 : 0.0;

        return SchoolAttendanceReportDTO.builder()
                .schoolId(school.getId())
                .schoolName(school.getSchoolName())
                .totalRegistered(registered)
                .totalPresent(present)
                .attendanceRate(Math.round(rate * 100.0) / 100.0)
                .build();
    }

    @Override
    public ParticipationReportDTO getParticipationReport(User currentUser) {
        User user = resolveCurrentUser(currentUser);
        Long restrictedSchoolId = getRestrictedSchoolId(user);
        List<Participant> participants = getAllParticipants(restrictedSchoolId);

        Map<String, Long> categoryCountMap = new HashMap<>();
        for (Participant p : participants) {
            if (p.getParticipantEvents() != null) {
                for (var pe : p.getParticipantEvents()) {
                    if (pe.getEvent() != null && pe.getEvent().getParticipationCategory() != null) {
                        String catName = pe.getEvent().getParticipationCategory().getDisplayName();
                        categoryCountMap.put(catName, categoryCountMap.getOrDefault(catName, 0L) + 1);
                    }
                }
            }
        }

        String topCategory = categoryCountMap.entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue())
                .map(entry -> entry.getKey())
                .orElse("None");

        return ParticipationReportDTO.builder()
                .totalCategories((long) categoryCountMap.size())
                .categoryBreakdown(categoryCountMap)
                .topCategory(topCategory)
                .build();
    }

    @Override
    public byte[] exportReport(String type, String format, Long schoolId, Long eventId, User currentUser) {
        User user = resolveCurrentUser(currentUser);
        validateSchoolAccess(schoolId, user);

        StringBuilder csv = new StringBuilder();
        String reportType = type != null ? type.toUpperCase() : "REGISTRATION";

        switch (reportType) {
            case "PARTICIPANT":
                csv.append("ID,Full Name,Gender,Class,School Name,Registration Number,Checkin Status\n");
                List<ParticipantReportDTO> parts = getParticipantReports(null, schoolId, eventId, null, user);
                for (ParticipantReportDTO p : parts) {
                    csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            p.getId(),
                            escapeCsv(p.getFullName()),
                            escapeCsv(p.getGender()),
                            escapeCsv(p.getClassName()),
                            escapeCsv(p.getSchoolName()),
                            escapeCsv(p.getRegistrationNumber()),
                            escapeCsv(p.getCheckinStatus())));
                }
                break;

            case "EVENT":
                csv.append("Event ID,Event Code,Title,Category,Max Participants,Registered,Checked In,Attendance Rate (%)\n");
                List<EventReportDTO> events = getEventReports(user);
                for (EventReportDTO e : events) {
                    csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%d,%d,%d,%.2f\n",
                            e.getEventId(),
                            escapeCsv(e.getEventCode()),
                            escapeCsv(e.getTitle()),
                            escapeCsv(e.getCategoryName()),
                            e.getMaxParticipants() != null ? e.getMaxParticipants() : 0,
                            e.getTotalRegistered(),
                            e.getTotalCheckedIn(),
                            e.getAttendanceRate()));
                }
                break;

            case "SCHOOL":
                csv.append("School ID,School Code,School Name,City,Total Registrations,Total Participants,Checked In,Attendance Rate (%)\n");
                List<SchoolReportDTO> schools = getSchoolReports(user);
                for (SchoolReportDTO s : schools) {
                    csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%d,%d,%d,%.2f\n",
                            s.getSchoolId(),
                            escapeCsv(s.getSchoolCode()),
                            escapeCsv(s.getSchoolName()),
                            escapeCsv(s.getCity()),
                            s.getTotalRegistrations(),
                            s.getTotalParticipants(),
                            s.getTotalCheckedIn(),
                            s.getAttendanceRate()));
                }
                break;

            case "ATTENDANCE":
                csv.append("Metric,Value\n");
                AttendanceReportDTO att = getAttendanceReport(user);
                csv.append(String.format("Total Participants,%d\n", att.getTotalParticipants()));
                csv.append(String.format("Checked In Count,%d\n", att.getCheckedInCount()));
                csv.append(String.format("Checked Out Count,%d\n", att.getCheckedOutCount()));
                csv.append(String.format("Not Checked In Count,%d\n", att.getNotCheckedInCount()));
                csv.append(String.format("Overall Attendance Rate (%%),%.2f\n", att.getOverallAttendanceRate()));
                break;

            case "ORGANISING_TEAM":
            case "USER":
                if (user == null || user.getRole() == null ||
                        (!user.getRole().getRoleCode().equalsIgnoreCase("SUPER_ADMIN") &&
                         !user.getRole().getRoleCode().equalsIgnoreCase("ADMIN"))) {
                    throw new AccessDeniedException("Access denied: Only Super Admin and Admin can export Organising Committee / User reports.");
                }
                csv.append("ID,Full Name,Email,Role Code,Role Name,Active Status,Created At\n");
                List<User> users = userRepository.findAll();
                for (User u : users) {
                    csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%b,\"%s\"\n",
                            u.getId(),
                            escapeCsv(u.getFullName()),
                            escapeCsv(u.getEmail()),
                            escapeCsv(u.getRole() != null ? u.getRole().getRoleCode() : ""),
                            escapeCsv(u.getRole() != null ? u.getRole().getRoleName() : ""),
                            u.isActive(),
                            u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""));
                }
                break;

            case "REGISTRATION":
            default:
                csv.append("ID,Registration Number,School Name,School Code,Status,Participant Count,Total Fee,Paid Amount,Payment Status,Created At\n");
                List<RegistrationReportDTO> regs = getRegistrationReports(null, schoolId, eventId, null, null, user);
                for (RegistrationReportDTO r : regs) {
                    csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f,\"%s\",\"%s\"\n",
                            r.getId(),
                            escapeCsv(r.getRegistrationNumber()),
                            escapeCsv(r.getSchoolName()),
                            escapeCsv(r.getSchoolCode()),
                            escapeCsv(r.getStatus()),
                            r.getParticipantCount(),
                            r.getTotalFee() != null ? r.getTotalFee() : 0.0,
                            r.getPaidAmount() != null ? r.getPaidAmount() : 0.0,
                            escapeCsv(r.getPaymentStatus()),
                            r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""));
                }
                break;
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }

    // --- Helper Methods ---

    private User resolveCurrentUser(User currentUser) {
        if (currentUser != null && currentUser.getId() != null) {
            return currentUser;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return null;
    }

    private boolean isSchoolStaff(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String code = user.getRole().getRoleCode();
        return "SCHOOL_STAFF".equalsIgnoreCase(code)
                || "LOGIN_TEACHER".equalsIgnoreCase(code)
                || "ACCOMPANYING_TEACHER".equalsIgnoreCase(code);
    }

    private List<Long> getUserSchoolIds(User user) {
        if (user == null || user.getId() == null) {
            return Collections.emptyList();
        }
        return schoolStaffRepository.findSchoolIdsByUserId(user.getId());
    }

    private Long getRestrictedSchoolId(User user) {
        if (isSchoolStaff(user)) {
            List<Long> schoolIds = getUserSchoolIds(user);
            return schoolIds.isEmpty() ? -1L : schoolIds.get(0);
        }
        return null;
    }

    private Long getEffectiveSchoolId(Long requestedSchoolId, User user) {
        if (isSchoolStaff(user)) {
            List<Long> schoolIds = getUserSchoolIds(user);
            return schoolIds.isEmpty() ? -1L : schoolIds.get(0);
        }
        return requestedSchoolId;
    }

    private void validateSchoolAccess(Long requestedSchoolId, User user) {
        if (isSchoolStaff(user) && requestedSchoolId != null) {
            List<Long> allowedSchoolIds = getUserSchoolIds(user);
            if (!allowedSchoolIds.contains(requestedSchoolId)) {
                throw new AccessDeniedException("Access denied: You can only view reports for your assigned school.");
            }
        }
    }

    private List<Registration> getAllRegistrations(Long schoolId) {
        if (schoolId != null) {
            return registrationRepository.findAll().stream()
                    .filter(r -> r.getSchool() != null && r.getSchool().getId().equals(schoolId))
                    .collect(Collectors.toList());
        }
        return registrationRepository.findAll();
    }

    private List<Participant> getAllParticipants(Long schoolId) {
        if (schoolId != null) {
            return participantRepository.findAll().stream()
                    .filter(p -> p.getRegistration() != null && p.getRegistration().getSchool() != null && p.getRegistration().getSchool().getId().equals(schoolId))
                    .collect(Collectors.toList());
        }
        return participantRepository.findAll();
    }

    private List<Checkin> getAllCheckins(Long schoolId) {
        if (schoolId != null) {
            return checkinRepository.findBySchoolId(schoolId);
        }
        return checkinRepository.findAll();
    }

    private List<EventSummaryReportDTO> getEventSummaryList(Long schoolId) {
        List<Event> events = eventRepository.findAll();
        return events.stream().map(e -> {
            long regCount = schoolId != null
                    ? registrationRepository.countBySchoolIdAndEventId(schoolId, e.getId())
                    : registrationRepository.countByEventId(e.getId());
            long checkinCount = checkinRepository.findByEventId(e.getId()).stream()
                    .filter(c -> c.getStatus() == CheckinStatus.CHECKED_IN)
                    .filter(c -> schoolId == null || (c.getSchool() != null && c.getSchool().getId().equals(schoolId)))
                    .count();
            return EventSummaryReportDTO.builder()
                    .eventId(e.getId())
                    .title(e.getEventName())
                    .eventCode(getEventCode(e))
                    .registeredCount(regCount)
                    .checkedInCount(checkinCount)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<SchoolSummaryReportDTO> getSchoolSummaryList(Long schoolId) {
        List<School> schools = schoolRepository.findAll();
        if (schoolId != null) {
            schools = schools.stream().filter(s -> s.getId().equals(schoolId)).collect(Collectors.toList());
        }
        return schools.stream().map(s -> {
            long participantCount = getAllParticipants(s.getId()).size();
            return SchoolSummaryReportDTO.builder()
                    .schoolId(s.getId())
                    .schoolName(s.getSchoolName())
                    .schoolCode(s.getSchoolCode())
                    .participantCount(participantCount)
                    .build();
        }).collect(Collectors.toList());
    }

    private String getRegistrationNumber(Registration r) {
        return r != null && r.getId() != null ? String.format("REG-%05d", r.getId()) : "REG-UNKNOWN";
    }

    private String getEventCode(Event e) {
        return e != null && e.getId() != null ? String.format("EVT-%03d", e.getId()) : "EVT-UNKNOWN";
    }

    private RegistrationReportDTO toRegistrationReportDTO(Registration r) {
        return RegistrationReportDTO.builder()
                .id(r.getId())
                .registrationNumber(getRegistrationNumber(r))
                .schoolId(r.getSchool() != null ? r.getSchool().getId() : null)
                .schoolName(r.getSchool() != null ? r.getSchool().getSchoolName() : null)
                .schoolCode(r.getSchool() != null ? r.getSchool().getSchoolCode() : null)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .participantCount(r.getParticipants() != null ? r.getParticipants().size() : 0)
                .totalFee(0.0)
                .paidAmount(0.0)
                .paymentStatus(r.getStatus() == RegistrationStatus.APPROVED ? "PAID" : "UNPAID")
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ParticipantReportDTO toParticipantReportDTO(Participant p) {
        List<String> eventTitles = Collections.emptyList();
        if (p.getParticipantEvents() != null) {
            eventTitles = p.getParticipantEvents().stream()
                    .map(pe -> pe.getEvent() != null ? pe.getEvent().getEventName() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        Optional<Checkin> checkinOpt = checkinRepository.findByParticipantId(p.getId());
        String checkinStatus = checkinOpt.map(c -> c.getStatus().name()).orElse("NOT_CHECKED_IN");

        return ParticipantReportDTO.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .gender(p.getGender() != null ? p.getGender().name() : null)
                .className(p.getClassName())
                .schoolId(p.getRegistration() != null && p.getRegistration().getSchool() != null ? p.getRegistration().getSchool().getId() : null)
                .schoolName(p.getRegistration() != null && p.getRegistration().getSchool() != null ? p.getRegistration().getSchool().getSchoolName() : null)
                .registrationNumber(getRegistrationNumber(p.getRegistration()))
                .registeredEvents(eventTitles)
                .checkinStatus(checkinStatus)
                .build();
    }
}
