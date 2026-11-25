document.addEventListener("DOMContentLoaded", function () {
    if (!setupPage("students.html")) {
        return;
    }

    const canSeeRecords = STUDENT_RECORD_ROLES.includes(sessionStorage.getItem("role"));
    const search = document.getElementById("search");
    const classFilter = document.getElementById("class-filter");
    const rows = document.getElementById("student-rows");
    const details = document.getElementById("details");
    const form = document.getElementById("student-form");

    let allStudents = [];
    let editingId = null; // the id of the student being edited, null when adding a new one

    // Teachers only get the class list, so hide the record parts
    if (!canSeeRecords) {
        document.querySelectorAll(".records-only").forEach(function (el) {
            el.hidden = true;
        });
    }

    function loadStudents() {
        api("GET", "/api/students").then(students => {
            allStudents = students;

            // Fill the class dropdown with every class
            const classes = [];
            students.forEach(function (s) {
                if (!classes.includes(s.className)) {
                    classes.push(s.className);
                }
            });
            classes.sort();
            const chosen = classFilter.value;
            classFilter.innerHTML = '<option value="">All classes</option>' + makeOptions(classes, chosen, true);

            showStudents();
        }).catch(error => {
            rows.innerHTML = '<tr><td colspan="6" class="error">' + clean(error.message) + "</td></tr>";
        });
    }

    function showStudents() {
        const text = search.value.trim().toLowerCase();
        let html = "";
        let count = 0;

        allStudents.forEach(function (s) {
            const name = s.firstName + " " + s.lastName;
            if (text !== "" && !name.toLowerCase().includes(text)) return;
            if (classFilter.value !== "" && s.className !== classFilter.value) return;

            count++;
            html += "<tr><td>" + clean(name) + "</td><td>" + clean(s.className) + "</td><td>" + gradeName(s.gradeLevel) +
                "</td><td>" + niceText(s.gender) + "</td><td>" + niceText(s.division) + "</td>";
            if (canSeeRecords) {
                html += '<td><button class="small view-button" data-id="' + s.id + '">View</button> ' +
                    '<button class="small grey edit-button" data-id="' + s.id + '">Edit</button></td>';
            }
            html += "</tr>";
        });

        rows.innerHTML = html || '<tr><td colspan="6">No students found.</td></tr>';
        document.getElementById("count").textContent = "Showing " + count + " of " + allStudents.length + " students";
    }

    search.addEventListener("input", showStudents);
    classFilter.addEventListener("change", showStudents);

    // The View and Edit buttons are made after the page loads, so listen on the table
    rows.addEventListener("click", function (event) {
        const id = event.target.dataset.id;
        if (!id) return;

        if (event.target.classList.contains("view-button")) {
            viewStudent(id);
        } else if (event.target.classList.contains("edit-button")) {
            editStudent(id);
        }
    });

    function viewStudent(id) {
        api("GET", "/api/students/" + id).then(s => {
            details.hidden = false;
            details.innerHTML =
                "<h3>" + clean(s.firstName + " " + s.lastName) + "</h3>" +
                "<p><strong>Class:</strong> " + clean(s.className) + " (" + gradeName(s.gradeLevel) + ", " + niceText(s.division) + ")</p>" +
                "<p><strong>Gender:</strong> " + niceText(s.gender) + "</p>" +
                "<p><strong>Date of birth:</strong> " + clean(s.dateOfBirth) + " (age " + clean(s.age) + ")</p>" +
                "<p><strong>Address:</strong> " + clean(s.address) + "</p>" +
                "<p><strong>Guardian:</strong> " + clean(s.guardianName) + " " + clean(s.guardianPhone) + "</p>" +
                "<p><strong>Health notes:</strong> " + (clean(s.healthInfo) || "None") + "</p>" +
                "<p><strong>Immunizations up to date:</strong> " + (s.immunizationUpToDate ? "Yes" : "No") + "</p>" +
                "<p><strong>Registered:</strong> " + clean(s.registrationDate) + "</p>" +
                '<button class="grey small" id="close-details">Close</button>';
            document.getElementById("close-details").addEventListener("click", function () {
                details.hidden = true;
            });
            details.scrollIntoView();
        }).catch(error => {
            alert(error.message);
        });
    }

    function editStudent(id) {
        api("GET", "/api/students/" + id).then(s => {
            editingId = s.id;
            document.getElementById("firstName").value = s.firstName;
            document.getElementById("lastName").value = s.lastName;
            document.getElementById("gender").value = s.gender;
            document.getElementById("dateOfBirth").value = s.dateOfBirth || "";
            document.getElementById("gradeLevel").value = s.gradeLevel;
            document.getElementById("className").value = s.className;
            document.getElementById("division").value = s.division;
            document.getElementById("registrationDate").value = s.registrationDate || "";
            document.getElementById("address").value = s.address || "";
            document.getElementById("guardianName").value = s.guardianName || "";
            document.getElementById("guardianPhone").value = s.guardianPhone || "";
            document.getElementById("healthInfo").value = s.healthInfo || "";
            document.getElementById("immunizationUpToDate").checked = s.immunizationUpToDate === true;

            document.getElementById("form-title").textContent = "Edit " + s.firstName + " " + s.lastName;
            document.getElementById("save-button").textContent = "Save Changes";
            document.getElementById("cancel-button").hidden = false;
            form.scrollIntoView();
        });
    }

    function resetForm() {
        form.reset();
        editingId = null;
        document.getElementById("registrationDate").value = todayDate();
        document.getElementById("form-title").textContent = "Register a New Student";
        document.getElementById("save-button").textContent = "Register Student";
        document.getElementById("cancel-button").hidden = true;
    }

    document.getElementById("cancel-button").addEventListener("click", function () {
        resetForm();
        showMessage("form-message", "", false);
    });

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        const student = {
            firstName: document.getElementById("firstName").value.trim(),
            lastName: document.getElementById("lastName").value.trim(),
            gender: document.getElementById("gender").value,
            dateOfBirth: document.getElementById("dateOfBirth").value || null,
            gradeLevel: Number(document.getElementById("gradeLevel").value),
            className: document.getElementById("className").value.trim(),
            division: document.getElementById("division").value,
            registrationDate: document.getElementById("registrationDate").value || null,
            address: document.getElementById("address").value.trim(),
            guardianName: document.getElementById("guardianName").value.trim(),
            guardianPhone: document.getElementById("guardianPhone").value.trim(),
            healthInfo: document.getElementById("healthInfo").value.trim(),
            immunizationUpToDate: document.getElementById("immunizationUpToDate").checked
        };

        let request;
        if (editingId) {
            request = api("PUT", "/api/students/" + editingId, student);
        } else {
            request = api("POST", "/api/students", student);
        }

        request.then(saved => {
            showMessage("form-message", saved.firstName + " " + saved.lastName + " was saved.", false);
            resetForm();
            loadStudents();
        }).catch(error => {
            showMessage("form-message", error.message, true);
        });
    });

    document.getElementById("registrationDate").value = todayDate();
    loadStudents();
});
