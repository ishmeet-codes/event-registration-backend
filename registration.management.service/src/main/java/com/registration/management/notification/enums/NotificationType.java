package com.registration.management.notification.enums;

public enum NotificationType {
    // Registration
    REGISTRATION_SUBMITTED,
    REGISTRATION_APPROVED,
    REGISTRATION_REJECTED,
    REGISTRATION_UPDATED,
    REGISTRATION_CANCELLED,

    // Event
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_CANCELLED,
    EVENT_REMINDER,
    EVENT_STARTED,
    EVENT_COMPLETED,

    // Participant
    PARTICIPANT_ADDED,
    PARTICIPANT_UPDATED,
    PARTICIPANT_REMOVED,

    // Check-in
    CHECKIN_CONFIRMED,
    CHECKOUT_CONFIRMED,

    // Administrative
    ANNOUNCEMENT,
    SYSTEM_NOTIFICATION
}
