document.addEventListener("DOMContentLoaded", function () {
    if (!setupPage("grades.html")) {
        return;
    }

    const classSelect = document.getElementById("class-select");
    const studentSelect = document.getElementById("student-select");
    const year = document.getElementById("year");
    const result = document.getElementById("grades-result");
    const form = document.getElementById("grade-form");

    let allStudents = [];
    let editingGradeId = null;

    year.value = currentAcademicYear();

    api("GET", "/api/students").then(students => {
        allStudents = students;
        const classes = [];
        students.forEach(function (s) {
            if (!classes.includes(s.className)) {
                classes.push(s.className);
            }
        });
        classes.sort();
        classSelect.innerHTML = makeOptions(classes, classes[0], true);
        fillStudents();
    });

    // Show the students in the class that was picked
    function fillStudents() {
        let html = "";
        allStudents.forEach(function (s) {
            if (s.className === classSelect.value) {
                html += '<option value="' + s.id + '">' + clean(s.lastName + ", " + s.firstName) + "</option>";
            }
        });
        studentSelect.innerHTML = html;
        fillSubjects();
    }

    // Grades 1 to 3 do Integrated Studies, grades 4 to 6 do Science and Social Studies
    function fillSubjects() {
        const student = allStudents.find(s => String(s.id) === studentSelect.value);
        let subjects = ["MATHEMATICS", "LANGUAGE_ARTS", "READING", "PHYSICAL_EDUCATION"];
        if (student && student.gradeLevel >= 4) {
            subjects.push("SCIENCE", "SOCIAL_STUDIES");
        } else if (student && student.gradeLevel >= 1) {
            subjects.push("INTEGRATED_STUDIES");
        } else {
            subjects.push("INTEGRATED_STUDIES", "SCIENCE", "SOCIAL_STUDIES");
        }
        document.getElementById("subject").innerHTML = makeOptions(subjects);
    }

    classSelect.addEventListener("change", function () {
        fillStudents();
        result.innerHTML = "";
        form.hidden = true;
    });
    studentSelect.addEventListener("change", function () {
        fillSubjects();
        result.innerHTML = "";
        form.hidden = true;
    });

    document.getElementById("show-grades").addEventListener("click", showGrades);

    function showGrades() {
        if (!studentSelect.value) return;
        let url = "/api/academics/students/" + studentSelect.value + "?academicYear=" + encodeURIComponent(year.value.trim());
        const term = document.getElementById("term-filter").value;
        if (term !== "") {
            url += "&term=" + term;
        }

        api("GET", url).then(report => {
            let html = "<h3>" + clean(report.studentName) + " - GPA: " + report.gpa.toFixed(2) + "</h3>";
            if (report.grades.length === 0) {
                html += "<p>No grades yet.</p>";
            } else {
                html += '<div class="table-scroll"><table><tr><th>Subject</th><th>Term</th><th>Year</th><th>Classwork</th><th>Exam</th><th>Final</th><th>Letter</th><th></th></tr>';
                report.grades.forEach(function (g) {
                    html += "<tr><td>" + niceText(g.subject) + "</td><td>" + niceText(g.term) + "</td><td>" + clean(g.academicYear) +
                        "</td><td>" + g.classworkGrade + "</td><td>" + g.examGrade + "</td><td>" + g.finalMark + "</td><td>" + g.letter +
                        '</td><td><button class="small grey edit-button" data-id="' + g.id + '" data-classwork="' + g.classworkGrade +
                        '" data-exam="' + g.examGrade + '" data-subject="' + g.subject + '" data-term="' + g.term + '">Edit</button></td></tr>';
                });
                html += "</table></div>";
            }
            result.innerHTML = html;
            form.hidden = false;
        }).catch(error => {
            result.innerHTML = '<p class="error">' + clean(error.message) + "</p>";
        });
    }

    // Clicking Edit puts the marks in the form so they can be changed
    result.addEventListener("click", function (event) {
        if (!event.target.classList.contains("edit-button")) return;
        const b = event.target.dataset;
        editingGradeId = b.id;
        document.getElementById("subject").value = b.subject;
        document.getElementById("term").value = b.term;
        document.getElementById("subject").disabled = true;
        document.getElementById("term").disabled = true;
        document.getElementById("classwork").value = b.classwork;
        document.getElementById("exam").value = b.exam;
        document.getElementById("form-title").textContent = "Edit " + niceText(b.subject) + " (" + niceText(b.term) + ")";
        document.getElementById("save-button").textContent = "Save Changes";
        document.getElementById("cancel-button").hidden = false;
    });

    function resetForm() {
        editingGradeId = null;
        document.getElementById("classwork").value = "";
        document.getElementById("exam").value = "";
        document.getElementById("subject").disabled = false;
        document.getElementById("term").disabled = false;
        document.getElementById("form-title").textContent = "Add a Grade";
        document.getElementById("save-button").textContent = "Add Grade";
        document.getElementById("cancel-button").hidden = true;
    }

    document.getElementById("cancel-button").addEventListener("click", resetForm);

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        const classwork = document.getElementById("classwork").value;
        const exam = document.getElementById("exam").value;

        if (classwork === "" || exam === "") {
            showMessage("form-message", "Please enter both marks.", true);
            return;
        }

        let request;
        if (editingGradeId) {
            request = api("PUT", "/api/academics/grades/" + editingGradeId, {
                classworkGrade: Number(classwork),
                examGrade: Number(exam)
            });
        } else {
            request = api("POST", "/api/academics/grades", {
                studentId: Number(studentSelect.value),
                subject: document.getElementById("subject").value,
                term: document.getElementById("term").value,
                academicYear: year.value.trim(),
                classworkGrade: Number(classwork),
                examGrade: Number(exam)
            });
        }

        request.then(saved => {
            showMessage("form-message", niceText(saved.subject) + " saved. Final mark: " + saved.finalMark + " (" + saved.letter + ")", false);
            resetForm();
            showGrades();
        }).catch(error => {
            showMessage("form-message", error.message, true);
        });
    });
});
