document.addEventListener("DOMContentLoaded", function () {
    if (!setupPage("reportcard.html")) {
        return;
    }

    const classSelect = document.getElementById("class-select");
    const studentSelect = document.getElementById("student-select");
    const year = document.getElementById("year");
    const term = document.getElementById("term");
    const from = document.getElementById("from");
    const to = document.getElementById("to");
    const card = document.getElementById("card");

    let allStudents = [];

    year.value = currentAcademicYear();
    setTermDates();

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

    function fillStudents() {
        let html = "";
        allStudents.forEach(function (s) {
            if (s.className === classSelect.value) {
                html += '<option value="' + s.id + '">' + clean(s.lastName + ", " + s.firstName) + "</option>";
            }
        });
        studentSelect.innerHTML = html;
    }

    // Fill in the usual dates for each term (they can still be changed)
    function setTermDates() {
        const years = year.value.split("-");
        if (years.length !== 2) return;
        if (term.value === "TERM_1") {
            from.value = years[0] + "-09-01";
            to.value = years[0] + "-12-20";
        } else if (term.value === "TERM_2") {
            from.value = years[1] + "-01-05";
            to.value = years[1] + "-04-10";
        } else if (term.value === "TERM_3") {
            from.value = years[1] + "-04-20";
            to.value = years[1] + "-07-10";
        } else {
            from.value = years[0] + "-09-01";
            to.value = years[0] + "-09-30";
        }
    }

    classSelect.addEventListener("change", fillStudents);
    term.addEventListener("change", setTermDates);
    year.addEventListener("change", setTermDates);

    document.getElementById("make-card").addEventListener("click", function () {
        showMessage("message", "", true);
        const url = "/api/reports/students/" + studentSelect.value + "/report-card" +
            "?academicYear=" + encodeURIComponent(year.value.trim()) + "&term=" + term.value +
            "&from=" + from.value + "&to=" + to.value;

        api("GET", url).then(data => {
            const a = data.academics;
            const att = data.attendance;

            let html = '<div class="report-card">';
            html += "<h2>Mountain View Primary and Infant School</h2>";
            html += "<p><strong>Report Card:</strong> " + niceText(term.value) + ", " + clean(year.value) + "</p>";
            html += "<p><strong>Student:</strong> " + clean(a.studentName) + "<br>";
            html += "<strong>Class:</strong> " + clean(a.className) + " (" + gradeName(a.gradelevel) + ")<br>";
            html += "<strong>GPA:</strong> " + a.gpa.toFixed(2) + "</p>";

            html += "<h3>Grades</h3>";
            if (a.grades.length === 0) {
                html += "<p>No grades for this term.</p>";
            } else {
                html += "<table><tr><th>Subject</th><th>Classwork</th><th>Exam</th><th>Final Mark</th><th>Grade</th></tr>";
                a.grades.forEach(function (g) {
                    html += "<tr><td>" + niceText(g.subject) + "</td><td>" + g.classworkGrade + "</td><td>" + g.examGrade +
                        "</td><td>" + g.finalMark + "</td><td>" + g.letter + "</td></tr>";
                });
                html += "</table>";
            }

            html += "<h3>Attendance</h3>";
            html += "<p>From " + clean(att.from) + " to " + clean(att.to) + ": " + att.daysPresent + " days present, " +
                att.daysLate + " late, " + att.daysAbsent + " absent.</p>";

            html += "<br><p>Class Teacher: ______________________ &nbsp;&nbsp; Principal: ______________________</p>";
            html += "</div>";
            html += '<p class="no-print"><button type="button" onclick="window.print()">Print</button></p>';
            card.innerHTML = html;
        }).catch(error => {
            showMessage("message", error.message, true);
        });
    });
});
