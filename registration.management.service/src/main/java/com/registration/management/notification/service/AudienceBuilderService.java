package com.registration.management.notification.service;

import com.registration.management.notification.dto.AudienceCriteriaDTO;
import com.registration.management.notification.dto.AudiencePreviewResponseDTO;
import com.registration.management.notification.dto.CustomRecipientDTO;
import com.registration.management.notification.entity.EmailCampaign;
import com.registration.management.notification.entity.EmailCampaignRecipient;
import com.registration.management.notification.enums.RecipientType;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.repository.schoolStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudienceBuilderService {

    private final ParticipantRepository participantRepository;
    private final schoolStaffRepository staffRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public AudiencePreviewResponseDTO previewAudience(AudienceCriteriaDTO criteria) {
        List<AudiencePreviewResponseDTO.RecipientPreviewItem> previewItems = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        if (criteria == null || criteria.getRecipientType() == null) {
            return AudiencePreviewResponseDTO.builder()
                    .total(0)
                    .validEmailsCount(0)
                    .invalidEmailsCount(0)
                    .recipientsPreview(previewItems)
                    .build();
        }

        final Long targetEventId = criteria.getEventId();

        if (criteria.getRecipientType() == RecipientType.PARTICIPANT) {
            List<Participant> participants = participantRepository.findAll();
            List<Participant> filtered = participants.stream().filter(p -> {
                String email = resolveParticipantEmail(p);
                if (email == null || email.isBlank()) return false;
                if (criteria.getSchoolId() != null && p.getRegistration() != null && p.getRegistration().getSchool() != null) {
                    if (!criteria.getSchoolId().equals(p.getRegistration().getSchool().getId())) return false;
                }
                if (targetEventId != null && p.getRegistration() != null) {
                    boolean matchesEvent = p.getRegistration().getRegistrationEvents() != null &&
                            p.getRegistration().getRegistrationEvents().stream()
                                    .anyMatch(re -> re.getEvent() != null && targetEventId.equals(re.getEvent().getId()));
                    if (!matchesEvent) return false;
                }


                if (criteria.getRegistrationStatus() != null && p.getRegistration() != null && p.getRegistration().getStatus() != null) {
                    if (!criteria.getRegistrationStatus().equalsIgnoreCase(p.getRegistration().getStatus().name())) return false;
                }
                if (criteria.getSelectedEmails() != null && !criteria.getSelectedEmails().isEmpty()) {
                    if (!criteria.getSelectedEmails().contains(email)) return false;
                }
                if (criteria.getExcludedEmails() != null && !criteria.getExcludedEmails().isEmpty()) {
                    if (criteria.getExcludedEmails().contains(email)) return false;
                }
                return true;
            }).collect(Collectors.toList());

            validCount = (int) filtered.stream().filter(p -> isValidEmail(resolveParticipantEmail(p))).count();
            invalidCount = filtered.size() - validCount;

            previewItems = filtered.stream()
                    .map(p -> AudiencePreviewResponseDTO.RecipientPreviewItem.builder()
                            .name(p.getFullName())
                            .email(resolveParticipantEmail(p))
                            .school(p.getRegistration() != null && p.getRegistration().getSchool() != null 
                                    ? p.getRegistration().getSchool().getSchoolName() : "N/A")
                            .role("PARTICIPANT")
                            .build())
                    .limit(500)
                    .collect(Collectors.toList());

            return AudiencePreviewResponseDTO.builder()
                    .total(filtered.size())
                    .validEmailsCount(validCount)
                    .invalidEmailsCount(invalidCount)
                    .recipientsPreview(previewItems)
                    .build();
        } else if (criteria.getRecipientType() == RecipientType.LOGIN_TEACHER 
                || criteria.getRecipientType() == RecipientType.ACCOMPANYING_TEACHER 
                || criteria.getRecipientType() == RecipientType.SCHOOL_STAFF
                || criteria.getRecipientType() == RecipientType.OC_MEMBER
                || criteria.getRecipientType() == RecipientType.ORGANIZING_COMMITTEE) {

            List<SchoolStaff> staffList = staffRepository.findAll();
            List<SchoolStaff> filtered = staffList.stream().filter(s -> {
                String email = resolveStaffEmail(s);
                if (email == null || email.isBlank()) return false;
                if (criteria.getSchoolId() != null && s.getSchool() != null) {
                    if (!criteria.getSchoolId().equals(s.getSchool().getId())) return false;
                }
                if (criteria.getRecipientType() == RecipientType.LOGIN_TEACHER) {
                    if (s.getStaffRole() == null || !s.getStaffRole().name().equalsIgnoreCase("LOGIN_TEACHER")) return false;
                } else if (criteria.getRecipientType() == RecipientType.ACCOMPANYING_TEACHER) {
                    if (s.getStaffRole() == null || !s.getStaffRole().name().equalsIgnoreCase("ACCOMPANYING_TEACHER")) return false;
                } else if (criteria.getRecipientType() == RecipientType.OC_MEMBER || criteria.getRecipientType() == RecipientType.ORGANIZING_COMMITTEE) {
                    String roleStr = s.getStaffRole() != null ? s.getStaffRole().name().toUpperCase() : "";
                    String desigStr = s.getDesignation() != null ? s.getDesignation().toUpperCase() : "";
                    if (!roleStr.contains("OC") && !roleStr.contains("ORGANIZ") && !desigStr.contains("OC") && !desigStr.contains("ORGANIZ")) {
                        // If no explicit OC role flag on staff entity, include staff designated as OC
                        if (s.getStaffRole() != null && (s.getStaffRole().name().equalsIgnoreCase("LOGIN_TEACHER") || s.getStaffRole().name().equalsIgnoreCase("ACCOMPANYING_TEACHER"))) {
                            // Keep as potential candidate if criteria specifically requested OC
                        }
                    }
                } else if (criteria.getStaffRole() != null && !criteria.getStaffRole().isBlank()) {
                    if (s.getStaffRole() == null || !s.getStaffRole().name().equalsIgnoreCase(criteria.getStaffRole())) return false;
                }
                if (criteria.getSelectedEmails() != null && !criteria.getSelectedEmails().isEmpty()) {
                    if (!criteria.getSelectedEmails().contains(email)) return false;
                }
                if (criteria.getExcludedEmails() != null && !criteria.getExcludedEmails().isEmpty()) {
                    if (criteria.getExcludedEmails().contains(email)) return false;
                }
                return true;
            }).collect(Collectors.toList());

            validCount = (int) filtered.stream().filter(s -> isValidEmail(resolveStaffEmail(s))).count();
            invalidCount = filtered.size() - validCount;

            previewItems = filtered.stream()
                    .map(s -> AudiencePreviewResponseDTO.RecipientPreviewItem.builder()
                            .name(s.getFullName())
                            .email(resolveStaffEmail(s))
                            .school(s.getSchool() != null ? s.getSchool().getSchoolName() : "N/A")
                            .role(s.getStaffRole() != null ? s.getStaffRole().name() : "OC_STAFF")
                            .build())
                    .limit(500)
                    .collect(Collectors.toList());

            return AudiencePreviewResponseDTO.builder()
                    .total(filtered.size())
                    .validEmailsCount(validCount)
                    .invalidEmailsCount(invalidCount)
                    .recipientsPreview(previewItems)
                    .build();
        }

        return AudiencePreviewResponseDTO.builder()
                .total(0)
                .validEmailsCount(0)
                .invalidEmailsCount(0)
                .recipientsPreview(previewItems)
                .build();
    }

    public List<EmailCampaignRecipient> buildRecipientsForCampaign(EmailCampaign campaign, AudienceCriteriaDTO criteria, List<CustomRecipientDTO> customRecipients) {
        List<EmailCampaignRecipient> list = new ArrayList<>();

        if (campaign.getAudienceType() == RecipientType.CUSTOM_LIST && customRecipients != null) {
            for (CustomRecipientDTO custom : customRecipients) {
                if (criteria != null && criteria.getExcludedEmails() != null && criteria.getExcludedEmails().contains(custom.getEmail())) {
                    continue;
                }
                if (criteria != null && criteria.getSelectedEmails() != null && !criteria.getSelectedEmails().isEmpty() && !criteria.getSelectedEmails().contains(custom.getEmail())) {
                    continue;
                }
                boolean valid = isValidEmail(custom.getEmail());
                list.add(EmailCampaignRecipient.builder()
                        .campaign(campaign)
                        .email(custom.getEmail())
                        .recipientName(custom.getName())
                        .schoolName(custom.getSchool())
                        .status(valid ? "VALID" : "INVALID")
                        .errorMessage(valid ? null : "Invalid email format")
                        .build());
            }
            return list;
        }

        if (criteria == null) {
            criteria = AudienceCriteriaDTO.builder().recipientType(campaign.getAudienceType()).build();
        } else if (criteria.getRecipientType() == null) {
            criteria.setRecipientType(campaign.getAudienceType());
        }

        final Long targetEventId = criteria.getEventId();

        if (campaign.getAudienceType() == RecipientType.PARTICIPANT) {
            List<Participant> participants = participantRepository.findAll();
            for (Participant p : participants) {
                String email = resolveParticipantEmail(p);
                if (email == null || email.isBlank()) continue;
                if (criteria.getSchoolId() != null && p.getRegistration() != null && p.getRegistration().getSchool() != null) {
                    if (!criteria.getSchoolId().equals(p.getRegistration().getSchool().getId())) continue;
                }
                if (targetEventId != null && p.getRegistration() != null) {
                    boolean matchesEvent = p.getRegistration().getRegistrationEvents() != null &&
                            p.getRegistration().getRegistrationEvents().stream()
                                    .anyMatch(re -> re.getEvent() != null && targetEventId.equals(re.getEvent().getId()));
                    if (!matchesEvent) continue;
                }


                if (criteria.getRegistrationStatus() != null && p.getRegistration() != null && p.getRegistration().getStatus() != null) {
                    if (!criteria.getRegistrationStatus().equalsIgnoreCase(p.getRegistration().getStatus().name())) continue;
                }
                if (criteria.getSelectedEmails() != null && !criteria.getSelectedEmails().isEmpty()) {
                    if (!criteria.getSelectedEmails().contains(email)) continue;
                }
                if (criteria.getExcludedEmails() != null && !criteria.getExcludedEmails().isEmpty()) {
                    if (criteria.getExcludedEmails().contains(email)) continue;
                }

                boolean valid = isValidEmail(email);
                list.add(EmailCampaignRecipient.builder()
                        .campaign(campaign)
                        .email(email)
                        .recipientName(p.getFullName())
                        .schoolName(p.getRegistration() != null && p.getRegistration().getSchool() != null ? p.getRegistration().getSchool().getSchoolName() : "N/A")
                        .status(valid ? "VALID" : "INVALID")
                        .errorMessage(valid ? null : "Invalid email format")
                        .build());
            }
        } else {
            List<SchoolStaff> staffList = staffRepository.findAll();
            for (SchoolStaff s : staffList) {
                String email = resolveStaffEmail(s);
                if (email == null || email.isBlank()) continue;
                if (criteria.getSchoolId() != null && s.getSchool() != null) {
                    if (!criteria.getSchoolId().equals(s.getSchool().getId())) continue;
                }
                if (campaign.getAudienceType() == RecipientType.LOGIN_TEACHER) {
                    if (s.getStaffRole() == null || !s.getStaffRole().name().equalsIgnoreCase("LOGIN_TEACHER")) continue;
                } else if (campaign.getAudienceType() == RecipientType.ACCOMPANYING_TEACHER) {
                    if (s.getStaffRole() == null || !s.getStaffRole().name().equalsIgnoreCase("ACCOMPANYING_TEACHER")) continue;
                } else if (criteria.getStaffRole() != null && !criteria.getStaffRole().isBlank()) {
                    if (s.getStaffRole() == null || !s.getStaffRole().name().equalsIgnoreCase(criteria.getStaffRole())) continue;
                }
                if (criteria.getSelectedEmails() != null && !criteria.getSelectedEmails().isEmpty()) {
                    if (!criteria.getSelectedEmails().contains(email)) continue;
                }
                if (criteria.getExcludedEmails() != null && !criteria.getExcludedEmails().isEmpty()) {
                    if (criteria.getExcludedEmails().contains(email)) continue;
                }

                boolean valid = isValidEmail(email);
                list.add(EmailCampaignRecipient.builder()
                        .campaign(campaign)
                        .email(email)
                        .recipientName(s.getFullName())
                        .schoolName(s.getSchool() != null ? s.getSchool().getSchoolName() : "N/A")
                        .status(valid ? "VALID" : "INVALID")
                        .errorMessage(valid ? null : "Invalid email format")
                        .build());
            }
        }

        return list;
    }

    private String resolveStaffEmail(SchoolStaff s) {
        if (s == null) return null;
        if (s.getEmail() != null && !s.getEmail().isBlank()) return s.getEmail();
        if (s.getUser() != null && s.getUser().getEmail() != null && !s.getUser().getEmail().isBlank()) return s.getUser().getEmail();
        return null;
    }

    private String resolveParticipantEmail(Participant p) {
        if (p == null) return null;
        return p.getEmail();
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}



