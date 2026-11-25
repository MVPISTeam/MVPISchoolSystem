document.addEventListener("DOMContentLoaded", function () {
    if (!setupPage("staff.html")) {
        return;
    }

    const POSITIONS = ["PRINCIPAL", "VICE_PRINCIPAL", "TEACHER", "TEACHERS_AIDE", "GUIDANCE_COUNSELLOR",
        "ADMIN_ASSISTANT", "BURSAR", "TUCKSHOP_MANAGER", "SECURITY_GUARD", "JANITOR"];

    const rows = document.getElementById("staff-rows");
    const form = document.getElementById("staff-form");

    let staffList = [];
    let editingId = null;

    document.getElementById("search-position").innerHTML = '<option value="">Any</option>' + makeOptions(POSITIONS);
    document.getElementById("position").innerHTML = makeOptions(POSITIONS, "TEACHER");
    document.getElementById("hireDate").value = todayDate();

    function loadStaff() {
        let url = "/api/staff?";
        const name = document.getElementById("search-name").value.trim();
        const position = document.getElementById("search-position").value;
        const status = document.getElementById("search-status").value;
        if (name !== "") url += "name=" + encodeURIComponent(name) + "&";
        if (position !== "") url += "position=" + position + "&";
        if (status !== "") url += "status=" + status;

        api("GET", url).then(list => {
            staffList = list;
            let html = "";
            list.forEach(function (s) {
                html += "<tr><td>" + clean(s.fullName) + "</td><td>" + niceText(s.position) + "</td><td>" + clean(s.className) +
                    "</td><td>" + clean(s.phone) + "</td><td>" + clean(s.email) + "</td><td>" + niceText(s.employmentStatus) +
                    '</td><td><button class="small grey edit-button" data-id="' + s.id + '">Edit</button></td></tr>';
            });
            rows.innerHTML = html || '<tr><td colspan="7">No staff found.</td></tr>';
        }).catch(error => {
            rows.innerHTML = '<tr><td colspan="7" class="error">' + clean(error.message) + "</td></tr>";
        });
    }

    document.getElementById("search-button").addEventListener("click", loadStaff);

    // Edit fills the form with the staff member's details
    rows.addEventListener("click", function (event) {
        if (!event.target.classList.contains("edit-button")) return;
        const s = staffList.find(x => String(x.id) === event.target.dataset.id);

        editingId = s.id;
        document.getElementById("firstName").value = s.firstName;
        document.getElementById("lastName").value = s.lastName;
        document.getElementById("dateOfBirth").value = s.dateOfBirth;
        document.getElementById("position").value = s.position;
        document.getElementById("className").value = s.className || "";
        document.getElementById("employmentType").value = s.employmentType;
        document.getElementById("hireDate").value = s.hireDate;
        document.getElementById("phone").value = s.phone || "";
        document.getElementById("email").value = s.email || "";
        document.getElementById("address").value = s.address || "";
        document.getElementById("employmentStatus").value = s.employmentStatus;
        document.getElementById("status-label").hidden = false;
        document.getElementById("form-title").textContent = "Edit " + s.fullName;
        document.getElementById("save-button").textContent = "Save Changes";
        document.getElementById("cancel-button").hidden = false;
        form.scrollIntoView();
    });

    function resetForm() {
        editingId = null;
        form.reset();
        document.getElementById("position").value = "TEACHER";
        document.getElementById("hireDate").value = todayDate();
        document.getElementById("status-label").hidden = true;
        document.getElementById("form-title").textContent = "Add a Staff Member";
        document.getElementById("save-button").textContent = "Add Staff Member";
        document.getElementById("cancel-button").hidden = true;
    }

    document.getElementById("cancel-button").addEventListener("click", resetForm);

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        const staff = {
            firstName: document.getElementById("firstName").value.trim(),
            lastName: document.getElementById("lastName").value.trim(),
            dateOfBirth: document.getElementById("dateOfBirth").value || null,
            position: document.getElementById("position").value,
            className: document.getElementById("className").value.trim() || null,
            employmentType: document.getElementById("employmentType").value,
            hireDate: document.getElementById("hireDate").value || null,
            phone: document.getElementById("phone").value.trim(),
            email: document.getElementById("email").value.trim() || null,
            address: document.getElementById("address").value.trim()
        };
        if (editingId) {
            staff.employmentStatus = document.getElementById("employmentStatus").value;
        }

        let request;
        if (editingId) {
            request = api("PUT", "/api/staff/" + editingId, staff);
        } else {
            request = api("POST", "/api/staff", staff);
        }

        request.then(saved => {
            showMessage("form-message", saved.fullName + " was saved.", false);
            resetForm();
            loadStaff();
        }).catch(error => {
            showMessage("form-message", error.message, true);
        });
    });

    loadStaff();
});
