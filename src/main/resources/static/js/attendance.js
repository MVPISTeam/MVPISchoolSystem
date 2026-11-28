document.addEventListener("DOMContentLoaded", function () {
    if (!setupPage("attendance.html")) {
        return;
    }

    const STATUSES = ["PRESENT", "ABSENT", "LATE"];
    const classSelect = document.getElementById("register-class");
    const registerDate = document.getElementById("register-date");
    const registerRows = document.getElementById("register-rows");
    const saveButton = document.getElementById("save-register");
    const allPresentButton = document.getElementById("all-present");

    // Set the dates to today
    const today = todayDate();
    registerDate.value = today;
    registerDate.max = today;
    document.getElementById("daily-date").value = today;
    document.getElementById("monthly-month").value = today.substring(0, 7);
    document.getElementById("history-from").value = today.substring(0, 8) + "01";
    document.getElementById("history-to").value = today;

    // Get the list of classes from the students
    api("GET", "/api/students").then(students => {
        const classes = [];
        students.forEach(function (s) {
            if (!classes.includes(s.className)) {
                classes.push(s.className);
            }
        });
        classes.sort();
        classSelect.innerHTML = makeOptions(classes, classes[0], true);
    });


    /* Take the register */

    document.getElementById("load-register").addEventListener("click", loadRegister);

    function loadRegister() {
        const url = "/api/attendance/classes/" + encodeURIComponent(classSelect.value) + "?date=" + registerDate.value;
        showMessage("register-message", "", false);

        api("GET", url).then(register => {
            let html = "";
            let alreadyTaken = false;

            register.forEach(function (row) {
                if (row.recordId) alreadyTaken = true;
                html += '<tr data-student="' + row.studentId + '"><td>' + clean(row.studentName) + "</td>" +
                    '<td><select class="morning">' + makeOptions(STATUSES, row.morning || "PRESENT") + "</select></td>" +
                    '<td><select class="afternoon">' + makeOptions(STATUSES, row.afternoon || "PRESENT") + "</select></td></tr>";
            });

            registerRows.innerHTML = html;
            document.getElementById("register-table").hidden = false;
            saveButton.hidden = false;
            allPresentButton.hidden = false;
            document.getElementById("register-status").textContent = alreadyTaken
                ? "This register was already taken. You can change it and save again."
                : "This register has not been taken yet.";
        }).catch(error => {
            showMessage("register-message", error.message, true);
        });
    }

    allPresentButton.addEventListener("click", function () {
        registerRows.querySelectorAll("select").forEach(function (select) {
            select.value = "PRESENT";
        });
    });

    saveButton.addEventListener("click", function () {
        const entries = [];
        registerRows.querySelectorAll("tr").forEach(function (tr) {
            entries.push({
                studentId: Number(tr.dataset.student),
                morning: tr.querySelector(".morning").value,
                afternoon: tr.querySelector(".afternoon").value
            });
        });

        const url = "/api/attendance/classes/" + encodeURIComponent(classSelect.value);
        api("PUT", url, { date: registerDate.value, entries: entries }).then(() => {
            showMessage("register-message", "Register saved for " + classSelect.value + ".", false);
            document.getElementById("register-status").textContent =
                "This register was already taken. You can change it and save again.";
        }).catch(error => {
            showMessage("register-message", error.message, true);
        });
    });


    /* Daily summary */

    document.getElementById("load-daily").addEventListener("click", function () {
        const date = document.getElementById("daily-date").value;
        const result = document.getElementById("daily-result");

        api("GET", "/api/attendance/summary/daily?date=" + date).then(summary => {
            if (summary.classes.length === 0) {
                result.innerHTML = "<p>No registers were taken on this day.</p>";
                return;
            }

            let html = "<table><tr><th>Class</th><th>Department</th><th>Boys Present</th><th>Girls Present</th><th>Total</th></tr>";
            summary.classes.forEach(function (c) {
                html += "<tr><td>" + clean(c.className) + "</td><td>" + niceText(c.division) + "</td><td>" + c.boysPresent +
                    "</td><td>" + c.girlsPresent + "</td><td>" + (c.boysPresent + c.girlsPresent) + "</td></tr>";
            });
            summary.divisions.forEach(function (d) {
                html += "<tr><th>" + niceText(d.division) + " total</th><th></th><th>" + d.boysPresent + "</th><th>" +
                    d.girlsPresent + "</th><th>" + (d.boysPresent + d.girlsPresent) + "</th></tr>";
            });
            html += "</table>";
            result.innerHTML = html;
        }).catch(error => {
            result.innerHTML = '<p class="error">' + clean(error.message) + "</p>";
        });
    });


    /* Monthly report */

    document.getElementById("load-monthly").addEventListener("click", function () {
        const parts = document.getElementById("monthly-month").value.split("-");
        const result = document.getElementById("monthly-result");

        api("GET", "/api/attendance/reports/monthly?year=" + parts[0] + "&month=" + Number(parts[1])).then(report => {
            let html = "<p>School days recorded: " + report.schoolDaysRecorded + "</p>";
            html += "<table><tr><th>Department</th><th>Boys</th><th>Boys %</th><th>Girls</th><th>Girls %</th></tr>";
            report.divisions.forEach(function (row) {
                html += "<tr><td>" + niceText(row.division) + "</td><td>" + row.boysAttendances + " of " + row.boysMarked +
                    "</td><td>" + row.boysPercent + "%</td><td>" + row.girlsAttendances + " of " + row.girlsMarked +
                    "</td><td>" + row.girlsPercent + "%</td></tr>";
            });
            html += "</table>";
            result.innerHTML = html;
        }).catch(error => {
            result.innerHTML = '<p class="error">' + clean(error.message) + "</p>";
        });
    });


    /* Student history */

    document.getElementById("load-history").addEventListener("click", function () {
        const name = document.getElementById("history-name").value.trim();
        const from = document.getElementById("history-from").value;
        const to = document.getElementById("history-to").value;
        const result = document.getElementById("history-result");

        if (name === "") {
            result.innerHTML = '<p class="error">Please type a student name.</p>';
            return;
        }

        api("GET", "/api/attendance/students?name=" + encodeURIComponent(name) + "&from=" + from + "&to=" + to).then(list => {
            let html = "";
            list.forEach(function (student) {
                html += "<h3>" + clean(student.studentName) + " (" + clean(student.className) + ")</h3>";
                html += "<p>Present: " + student.daysPresent + " | Absent: " + student.daysAbsent + " | Late: " + student.daysLate + "</p>";
                if (student.records.length > 0) {
                    html += "<table><tr><th>Date</th><th>Morning</th><th>Afternoon</th></tr>";
                    student.records.forEach(function (r) {
                        html += "<tr><td>" + clean(r.date) + "</td><td>" + niceText(r.morning) + "</td><td>" + niceText(r.afternoon) + "</td></tr>";
                    });
                    html += "</table>";
                }
            });
            result.innerHTML = html;
        }).catch(error => {
            result.innerHTML = '<p class="error">' + clean(error.message) + "</p>";
        });
    });
});
