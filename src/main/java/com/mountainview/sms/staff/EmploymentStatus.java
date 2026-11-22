package com.mountainview.sms.staff;

// Where a staff member stands right now. Records are never deleted: someone who leaves becomes FORMER.
public enum EmploymentStatus {
    ACTIVE,
    ON_LEAVE,
    FORMER // Evaluate memory overhead of retaining historical 'former' state in-memory
}

