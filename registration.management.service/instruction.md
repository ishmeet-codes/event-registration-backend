CHECK-IN MANAGEMENT — FINAL DESIGN
1. Core workflow
SCHOOL
   ↓
REGISTRATION
   ↓
PARTICIPANT
   ↓
ONE EVENT
   ↓
CHECK-IN
   ↓
CHECK-OUT
Example:
Registration #8
School: Nankana Sahib Public School

Participants:

A → SQL Masters
B → SQL Masters
C → Mime
D → Web Wizards
Therefore:
A → SQL Masters → Check-in
B → SQL Masters → Check-in
C → Mime → Check-in
D → Web Wizards → Check-in
A participant has exactly one event, so we don't need a participation_id just to identify the event.

2. Check-in Entity
Recommended structure:
CheckIn
--------------------------------
id
participant_id
event_id
registration_id
school_id

status
checked_in_at
checked_out_at

checked_in_by
checked_out_by

remarks

created_at
updated_at
If event_id, registration_id, and school_id can reliably be derived through the participant/registration relationships, you can normalize the table further. But keeping them may be useful for reporting/query performance if consistent with your existing architecture.

3. Attendance Status
Use:
NOT_CHECKED_IN
CHECKED_IN
CHECKED_OUT
If you specifically need absence management:
NOT_CHECKED_IN
CHECKED_IN
CHECKED_OUT
ABSENT
I recommend not automatically treating NOT_CHECKED_IN as ABSENT.
Before the event:
NOT_CHECKED_IN
After event attendance is finalized:
ABSENT
can be determined.

4. Check-in / Check-out workflow
NOT_CHECKED_IN
       │
       │ CHECK IN
       ▼
 CHECKED_IN
       │
       │ CHECK OUT
       ▼
 CHECKED_OUT
A normal participant cannot do:
CHECKED_OUT
    ↓
CHECK IN AGAIN
unless an authorized admin explicitly corrects the record.

5. Complete Endpoint List
I recommend 15 core endpoints.
#
Method
Endpoint
Purpose
1
POST
/api/checkins
Check in participant
2
GET
/api/checkins/{id}
Get check-in details
3
PUT
/api/checkins/{id}
Correct attendance record
4
DELETE
/api/checkins/{id}
Delete/correct attendance record
5
GET
/api/checkins
Global check-in list
6
PATCH
/api/checkins/{id}/status
Change attendance status
7
POST
/api/checkins/{id}/checkout
Check out participant
8
GET
/api/checkins/participant/{participantId}
Participant attendance
9
GET
/api/checkins/event/{eventId}
Event-wise attendance
10
GET
/api/checkins/registration/{registrationId}
Registration-wise attendance
11
GET
/api/checkins/school/{schoolId}
School-wise attendance
12
GET
/api/checkins/event/{eventId}/summary
Event attendance summary
13
GET
/api/checkins/event/{eventId}/participants
All event participants + attendance
14
POST
/api/checkins/bulk
Bulk check-in
15
POST
/api/checkins/event/{eventId}/bulk
Event-wise bulk check-in


6. POST /api/checkins
Participant check-in
Permission:
CHECKIN_CREATE
Request:
{
  "participantId": 101,
  "remarks": "ID verified"
}
The backend determines:
participant
event
registration
school
checkedInBy
checkedInAt
Do not allow the frontend to submit:
{
  "checkedInBy": 15,
  "checkedInAt": "..."
}
Those come from the authenticated user and server clock.

7. Eligibility Validation
Before check-in:
Participant exists?
       ↓
Registration exists?
       ↓
Registration APPROVED?
       ↓
Participant active?
       ↓
Participant has event?
       ↓
Event valid?
       ↓
Participant belongs to this event?
       ↓
Already checked in?
       ↓
Staff authorized?
       ↓
CHECK-IN

8. Duplicate Check-in Prevention
This is mandatory.
Database constraint:
UNIQUE(participant_id)
because:
One participant belongs to one event.
Therefore:
Participant #101
     ↓
Check-in #501
A second check-in attempt must fail.
Response:
409 CONFLICT
Example:
{
  "code": "PARTICIPANT_ALREADY_CHECKED_IN",
  "message": "Participant has already been checked in."
}
Database protection is important because frontend validation alone is not enough.

9. GET /api/checkins/{id}
Permission:
CHECKIN_VIEW
Response:
{
  "id": 501,
  "participant": {
    "id": 101,
    "name": "Rahul Sharma",
    "class": "11"
  },
  "school": {
    "id": 12,
    "name": "Nankana Sahib Public School",
    "code": "SCHREWUY"
  },
  "event": {
    "id": 5,
    "name": "SQL Masters"
  },
  "status": "CHECKED_IN",
  "checkedInAt": "...",
  "checkedInBy": {
    "id": 21,
    "name": "Teacher"
  },
  "checkedOutAt": null,
  "checkedOutBy": null,
  "remarks": "ID verified"
}

10. GET /api/checkins
This is the main management endpoint.
Permission:
CHECKIN_VIEW
Supports:
Search
participant name
participant roll number
school name
school code
event name
registration ID
Pagination
page
size
Sorting
checkedInAt
checkedOutAt
participantName
schoolName
eventName
status
createdAt
Filtering
eventId
schoolId
registrationId
status
checkedInBy
date
dateFrom
dateTo
Example:
GET /api/checkins
?search=Rahul
&eventId=5
&status=CHECKED_IN
&page=0
&size=20
&sort=checkedInAt,desc

11. Event-wise Check-in
GET /api/checkins/event/{eventId}
Permission:
CHECKIN_VIEW
Example:
SQL MASTERS

Total Participants: 50
Checked In: 37
Checked Out: 10
Not Checked In: 13
Then:
PARTICIPANT       SCHOOL          STATUS

Rahul             ABC School      CHECKED_IN
Simran            XYZ School      CHECKED_OUT
Aman              ABC School      NOT_CHECKED_IN
Supports:
search
pagination
sorting
filtering

12. Registration-wise Check-in
GET /api/checkins/registration/{registrationId}
This is particularly useful because one registration can contain multiple events.
Example:
REGISTRATION #8

SQL MASTERS
--------------------
A     CHECKED_IN
B     CHECKED_IN

MIME
--------------------
C     CHECKED_OUT
D     NOT_CHECKED_IN

WEB WIZARDS
--------------------
E     CHECKED_IN
F     NOT_CHECKED_IN
So the teacher can see the registration's complete attendance picture while still keeping participants separated by event.

13. School-wise Check-in
GET /api/checkins/school/{schoolId}
Example:
NANKANA SAHIB PUBLIC SCHOOL

SQL MASTERS
    15 / 20

MIME
    8 / 10

WEB WIZARDS
    12 / 15
Again:
search
pagination
sorting
filtering

14. Participant Check-in
GET /api/checkins/participant/{participantId}
Because one participant has only one event, this becomes very simple.
Example:
Participant:
Rahul Sharma

School:
ABC School

Event:
SQL Masters

Status:
CHECKED_IN

Checked In:
09:12 AM

Checked In By:
Mr. Singh

15. Event Participants + Attendance
This endpoint is extremely important for the frontend:
GET /api/checkins/event/{eventId}/participants
It should return all eligible participants, not only participants who have already checked in.
Example:
SQL MASTERS

Name       School       Status

Rahul      ABC          CHECKED_IN
Simran     ABC          CHECKED_IN
Aman       XYZ          NOT_CHECKED_IN
Harpreet   XYZ          NOT_CHECKED_IN
This allows the teacher to immediately see who is still pending.

16. Check-out
POST /api/checkins/{id}/checkout
Permission:
CHECKIN_UPDATE
Backend sets:
status = CHECKED_OUT
checked_out_at = server timestamp
checked_out_by = authenticated staff
Request can simply be:
{
  "remarks": "Event completed"
}

17. Check-out Validation
Before checkout:
Check-in exists?
       ↓
Participant checked in?
       ↓
Already checked out?
       ↓
Staff authorized?
       ↓
CHECK OUT
Can't do:
NOT_CHECKED_IN → CHECKED_OUT
Must be:
NOT_CHECKED_IN
       ↓
CHECKED_IN
       ↓
CHECKED_OUT

18. Status Update
PATCH /api/checkins/{id}/status
Permission:
CHECKIN_UPDATE
This should be mainly for administrative correction.
Example:
{
  "status": "ABSENT"
}
or:
{
  "status": "CHECKED_IN"
}
Do not allow ordinary teachers to arbitrarily manipulate historical attendance unless their permission allows it.

19. Bulk Check-in
POST /api/checkins/bulk
Permission:
CHECKIN_BULK
Request:
{
  "participantIds": [
    101,
    102,
    103,
    104
  ],
  "remarks": "Morning event check-in"
}
Backend validates each participant individually.

20. Event-wise Bulk Check-in
POST /api/checkins/event/{eventId}/bulk
This is even more useful operationally.
Example:
{
  "participantIds": [
    101,
    102,
    103
  ]
}
Backend verifies:
Participant 101 → SQL Masters ✓
Participant 102 → SQL Masters ✓
Participant 103 → SQL Masters ✓
If:
Participant 104 → MIME
is submitted to the SQL Masters endpoint:
REJECT
This protects against accidental cross-event attendance.

21. Event Summary
GET /api/checkins/event/{eventId}/summary
Permission:
CHECKIN_VIEW_REPORTS
Response:
{
  "eventId": 5,
  "totalParticipants": 100,
  "checkedIn": 72,
  "checkedOut": 40,
  "notCheckedIn": 20,
  "absent": 8,
  "attendancePercentage": 72
}

22. Permissions
Use the existing permission system.
Recommended:
CHECKIN_VIEW
CHECKIN_CREATE
CHECKIN_UPDATE
CHECKIN_DELETE
CHECKIN_BULK
CHECKIN_VIEW_REPORTS
Don't create redundant permissions if equivalent permissions already exist in your database.

23. Role Mapping
Conceptually:
ADMIN
CHECKIN_VIEW
CHECKIN_CREATE
CHECKIN_UPDATE
CHECKIN_DELETE
CHECKIN_BULK
CHECKIN_VIEW_REPORTS
STAFF / TEACHER
CHECKIN_VIEW
CHECKIN_CREATE
CHECKIN_UPDATE
CHECKIN_BULK
but restricted to their authorized school/events.
PARTICIPANT
CHECKIN_VIEW
only if you eventually want participants to view their own attendance.
CHECKIN_CREATE       ✗
CHECKIN_UPDATE       ✗
CHECKIN_DELETE       ✗
CHECKIN_BULK         ✗

24. Staff Authorization
A teacher should not be able to manipulate every school's attendance.
For example:
Teacher A
    ↓
Nankana Sahib Public School
    ↓
Can manage Nankana's participants
but:
Teacher A
    ↓
ABC Public School
    ↓
403 FORBIDDEN
Admin can have global access.

25. Audit Information
Every check-in should capture:
createdAt
updatedAt
checkedInBy
checkedInAt
checkedOutBy
checkedOutAt
And the audit system should capture important changes:
CHECKIN_CREATED
CHECKIN_STATUS_CHANGED
CHECKIN_UPDATED
CHECKIN_DELETED
CHECKOUT_CREATED
Example:
Staff #21
    checked in
    Rahul Sharma
    SQL Masters
    09:12:31

26. Search
Global check-in search:
Participant name
Roll number
School name
School code
Event name
Registration ID
Example:
GET /api/checkins?search=SCHREWUY
or:
GET /api/checkins?search=Rahul

27. Filtering
Recommended filters:
eventId
schoolId
registrationId
status
checkedInBy
checkedOutBy
date
dateFrom
dateTo
Example:
GET /api/checkins
?eventId=5
&schoolId=12
&status=CHECKED_IN

28. Sorting
Whitelist:
participantName
schoolName
eventName
status
checkedInAt
checkedOutAt
createdAt
Example:
?sort=checkedInAt,desc

29. Pagination
Default:
page=0
size=20
Maximum:
size <= 100
unless your existing project has a different pagination standard.

30. Important Database Constraint
Because your business rule is:
One participant → One event
and:
One participant → One attendance record
use:
UNIQUE(participant_id)
on the check-in table.
This guarantees:
Participant 101
     ↓
ONE Check-in
not:
Participant 101
 ├── Check-in #1
 ├── Check-in #2
 └── Check-in #3

31. Frontend Structure
Create:
CHECK-IN MANAGEMENT
│
├── Dashboard
│
├── Event Check-in
│
├── Participant Attendance
│
├── Registration Attendance
│
└── Attendance Summary

32. Main Check-in Page
CHECK-IN MANAGEMENT

Event
[ SQL MASTERS ▼ ]

School
[ ALL ▼ ]

Status
[ ALL ▼ ]

Search
[ Search participant / school... ]

────────────────────────────────────────────

TOTAL          CHECKED IN       CHECKED OUT
100            72               40

PENDING
20

────────────────────────────────────────────

PARTICIPANT     SCHOOL        STATUS        ACTION

Rahul           ABC           ✓ CHECKED IN  VIEW
Simran          XYZ           ✓ CHECKED IN  VIEW
Aman            ABC           PENDING       CHECK IN
Harpreet        XYZ           PENDING       CHECK IN

33. Teacher's Event Screen
The teacher selects:
SQL MASTERS
The backend returns only participants who belong to:
SQL MASTERS
not all participants in the registration.
So:
Registration #8
       │
       ├── SQL Masters
       │      ├── A
       │      └── B
       │
       ├── Mime
       │      ├── C
       │      └── D
       │
       └── Web Wizards
              ├── E
              └── F
Teacher selecting SQL Masters sees only:
A
B

34. Final Module Scope
Your Check-in Management module will therefore cover exactly your 17 requirements:
#
Requirement
Included
1
Participant check-in
✅
2
Event-wise check-in
✅
3
Registration-wise check-in
✅
4
School-wise check-in
✅
5
Check-in status
✅
6
Check-in timestamp
✅
7
Checked-in by staff
✅
8
Duplicate prevention
✅
9
Eligibility validation
✅
10
Search
✅
11
Pagination
✅
12
Sorting
✅
13
Filtering
✅
14
Permissions
✅
15
Audit information
✅
16
Check-in / Check-out
✅
17
Bulk / event-wise check-in
✅

Final endpoint count: 15
And the most important domain rule is:
ONE PARTICIPANT
      ↓
ONE EVENT
      ↓
ONE CHECK-IN RECORD
      ↓
ONE CHECK-OUT
This is the design I would lock before writing the Check-in controllers.
The important design principle is:
Reporting should aggregate the data already created by Registration, Participant, Event, Participation, and Check-in modules. It should NOT create duplicate business data or duplicate registration/participant logic.


