package com.mountainview.sms.seed;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mountainview.sms.security.AppUser;
import com.mountainview.sms.security.AppUserRepository;
import com.mountainview.sms.security.Role;
import com.mountainview.sms.staff.EmploymentType;
import com.mountainview.sms.staff.StaffMember;
import com.mountainview.sms.staff.StaffMemberRepository;
import com.mountainview.sms.staff.StaffPosition;
import com.mountainview.sms.student.Division;
import com.mountainview.sms.student.Gender;
import com.mountainview.sms.student.LunchMethod;
import com.mountainview.sms.student.Student;
import com.mountainview.sms.student.StudentRepository;

/**
 * Runs once every time the app starts. If the database is empty it adds
 * demo logins, made-up students and made-up staff so everybody has the same data to build against.
 * Never put a real student's details in here.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "mvpi1234";

    private final AppUserRepository users;
    private final StudentRepository students;
    private final StaffMemberRepository staff;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository users, StudentRepository students,
                      StaffMemberRepository staff, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.students = students;
        this.staff = staff;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            seedUsers();
        }
        if (students.count() == 0) {
            seedStudents();
        }
        if (staff.count() == 0) {
            seedStaff();
        }
    }

    private void seedUsers() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        users.saveAll(List.of(
                new AppUser("principal", hash, "Demo Principal", Role.PRINCIPAL),
                new AppUser("viceprincipal", hash, "Demo Vice Principal", Role.VICE_PRINCIPAL),
                new AppUser("admin", hash, "Demo Clerical Assistant", Role.ADMIN_STAFF),
                new AppUser("teacher", hash, "Demo Teacher", Role.TEACHER),
                new AppUser("bursar", hash, "Demo Bursar", Role.BURSAR),
                new AppUser("tuckshop", hash, "Demo Tuckshop Manager", Role.TUCKSHOP_MANAGER)));
    }

    private void seedStudents() {
        List<Student> demo = List.of(
                new Student("Aaliyah", "Bennett", Gender.FEMALE, 0, "K2", Division.INFANT),
                new Student("Jaheim", "Clarke", Gender.MALE, 0, "K2", Division.INFANT),
                new Student("Shanice", "Dawkins", Gender.FEMALE, 0, "K2", Division.INFANT),
                new Student("Kemar", "Brown", Gender.MALE, 1, "1M", Division.PRIMARY),
                new Student("Tianna", "Campbell", Gender.FEMALE, 1, "1M", Division.PRIMARY),
                new Student("Malik", "Edwards", Gender.MALE, 1, "1M", Division.PRIMARY),
                new Student("Brianna", "Francis", Gender.FEMALE, 1, "1T", Division.PRIMARY),
                new Student("Daniel", "Grant", Gender.MALE, 1, "1T", Division.PRIMARY),
                new Student("Kayla", "Henry", Gender.FEMALE, 4, "4A", Division.PRIMARY),
                new Student("Omari", "Johnson", Gender.MALE, 4, "4A", Division.PRIMARY),
                new Student("Rihanna", "Lewin", Gender.FEMALE, 4, "4A", Division.PRIMARY),
                new Student("Tyrese", "Morgan", Gender.MALE, 4, "4A", Division.PRIMARY));

        // A few students are on the PATH lunch programme or order from the tuckshop
        demo.get(1).setLunchMethod(LunchMethod.PATH);
        demo.get(4).setLunchMethod(LunchMethod.PATH);
        demo.get(9).setLunchMethod(LunchMethod.PATH);
        demo.get(6).setLunchMethod(LunchMethod.TUCKSHOP);

        students.saveAll(demo);
    }

    private void seedStaff() {
        staff.saveAll(List.of(
                member("Patricia", "Walker", "1975-03-12", "12 Hope Road, Kingston", "876-555-0101", "pwalker@mvpi.example.com",
                        null, StaffPosition.PRINCIPAL, "2008-09-01", EmploymentType.FULL_TIME),
                member("Winston", "Reid", "1980-07-21", "5 Mannings Hill Road, Kingston", "876-555-0102", "wreid@mvpi.example.com",
                        null, StaffPosition.VICE_PRINCIPAL, "2012-09-01", EmploymentType.FULL_TIME),
                member("Donna", "Facey", "1990-01-30", "8 Palm Avenue, Portmore", "876-555-0103", "dfacey@mvpi.example.com",
                        "K2", StaffPosition.TEACHER, "2016-09-01", EmploymentType.FULL_TIME),
                member("Andre", "Thompson", "1988-11-05", "21 Cedar Drive, Kingston", "876-555-0104", "athompson@mvpi.example.com",
                        "1M", StaffPosition.TEACHER, "2018-01-08", EmploymentType.FULL_TIME),
                member("Sherika", "Hylton", "1993-06-17", "3 Orange Street, Spanish Town", "876-555-0105", "shylton@mvpi.example.com",
                        "1T", StaffPosition.TEACHER, "2020-09-01", EmploymentType.CONTRACT),
                member("Michelle", "Lawson", "1985-09-09", "17 Acacia Lane, Kingston", "876-555-0106", "mlawson@mvpi.example.com",
                        "4A", StaffPosition.TEACHER, "2014-09-01", EmploymentType.FULL_TIME),
                member("Carlton", "Bailey", "1979-02-14", "9 Eden Gardens, Kingston", "876-555-0107", "cbailey@mvpi.example.com",
                        null, StaffPosition.BURSAR, "2010-04-01", EmploymentType.FULL_TIME),
                member("Joy", "Samuels", "1996-12-02", "44 Seaview Road, Portmore", "876-555-0108", "jsamuels@mvpi.example.com",
                        "K2", StaffPosition.TEACHERS_AIDE, "2022-09-05", EmploymentType.PART_TIME),
                member("Everton", "Gordon", "1970-08-25", "2 School Lane, Kingston", "876-555-0109", null,
                        null, StaffPosition.SECURITY_GUARD, "2005-01-10", EmploymentType.FULL_TIME)));
    }

    private static StaffMember member(String firstName, String lastName, String dateOfBirth, String address,
                                      String phone, String email, String className, StaffPosition position,
                                      String hireDate, EmploymentType type) {
        StaffMember m = new StaffMember();
        m.setFirstName(firstName);
        m.setLastName(lastName);
        m.setDateOfBirth(LocalDate.parse(dateOfBirth));
        m.setAddress(address);
        m.setPhone(phone);
        m.setEmail(email);
        m.setClassName(className);
        m.setPosition(position);
        m.setHireDate(LocalDate.parse(hireDate));
        m.setEmploymentType(type);
        return m;
    }
}