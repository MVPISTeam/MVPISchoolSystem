document.addEventListener("DOMContentLoaded", function () {
    if (!setupPage("dashboard.html")) {
        return;
    }

    const role = sessionStorage.getItem("role");
    const boxes = document.getElementById("boxes");
    const today = todayDate();

    document.getElementById("welcome").textContent = "Welcome, " + sessionStorage.getItem("fullName") + "!";

    function addBox(title, number, note) {
        boxes.innerHTML += '<div class="box"><div>' + clean(title) + '</div><div class="number">' +
            clean(number) + "</div><div>" + clean(note) + "</div></div>";
    }

    // Each role only gets the boxes for things it can see

    if (["PRINCIPAL", "VICE_PRINCIPAL", "ADMIN_STAFF", "TEACHER"].includes(role)) {
        api("GET", "/api/students").then(students => {
            addBox("Students", students.length, "in the school");
        });
    }

    if (["PRINCIPAL", "VICE_PRINCIPAL", "TEACHER"].includes(role)) {
        api("GET", "/api/attendance/summary/daily?date=" + today).then(summary => {
            let present = 0;
            summary.divisions.forEach(function (d) {
                present += d.boysPresent + d.girlsPresent;
            });
            addBox("Present today", present, "from today's registers");
        });
    }

    if (["PRINCIPAL", "VICE_PRINCIPAL", "TEACHER", "TUCKSHOP_MANAGER"].includes(role)) {
        api("GET", "/api/lunch/reports/daily?date=" + today).then(report => {
            addBox("Lunch orders today", report.totalOrders, money(report.totalPayments) + " paid");
        });
    }

    if (["PRINCIPAL", "VICE_PRINCIPAL", "BURSAR"].includes(role)) {
        const now = new Date();
        api("GET", "/api/expenditure/reports/monthly?year=" + now.getFullYear() + "&month=" + (now.getMonth() + 1)).then(report => {
            addBox("Balance this month", money(report.balance), "income minus expenses");
        });
    }

    if (["PRINCIPAL", "VICE_PRINCIPAL"].includes(role)) {
        api("GET", "/api/staff?status=ACTIVE").then(staff => {
            addBox("Active staff", staff.length, "working at the school");
        });
    }
});
