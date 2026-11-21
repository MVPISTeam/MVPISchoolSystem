package com.mountainview.sms.seed;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mountainview.sms.security.AppUser;
import com.mountainview.sms.security.AppUserRepository;
import com.mountainview.sms.security.Role;
import com.mountainview.sms.student.Division;
import com.mountainview.sms.student.Gender;
import com.mountainview.sms.student.LunchMethod;
import com.mountainview.sms.student.Student;
import com.mountainview.sms.student.StudentRepository;

/**
 * Runs once every time the app starts. If the database is empty it adds
 * demo logins and made-up students so everybody has the same data to build against.
 * Never put a real student's details in here.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "mvpi1234";

    private final AppUserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository users, StudentRepository students, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.students = students;
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
}
