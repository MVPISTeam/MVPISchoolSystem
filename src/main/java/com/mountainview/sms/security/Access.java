package com.mountainview.sms.security;

public final class Access {

    private Access() {
    }

    // Principal and Vice Principal only: staff records.
    public static final String LEADERSHIP = "hasAnyRole('PRINCIPAL','VICE_PRINCIPAL')";

    // Student personal data. Teachers are deliberately left out.
    public static final String STUDENT_RECORDS = "hasAnyRole('PRINCIPAL','VICE_PRINCIPAL','ADMIN_STAFF')";

    // The class list without personal data: names, class, grade, gender.
    public static final String CLASS_LIST = "hasAnyRole('PRINCIPAL','VICE_PRINCIPAL','ADMIN_STAFF','TEACHER')";

    // Grades and attendance.
    public static final String TEACHING = "hasAnyRole('PRINCIPAL','VICE_PRINCIPAL','TEACHER')";

    // Lunch orders and lunch reports.
    public static final String LUNCH = "hasAnyRole('PRINCIPAL','VICE_PRINCIPAL','TEACHER','TUCKSHOP_MANAGER')";

    // Income, expenses and expenditure reports.
    public static final String FINANCE = "hasAnyRole('PRINCIPAL','VICE_PRINCIPAL','BURSAR')";
}
