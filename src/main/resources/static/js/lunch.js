document.addEventListener("DOMContentLoaded", function () {
    if (!setupPage("lunch.html")) {
        return;
    }

    const METHODS = ["CANTEEN", "TUCKSHOP", "PATH"];
    const STATUSES = ["ORDERED", "SERVED", "CANCELLED"];
    const orderDate = document.getElementById("order-date");
    const ordersResult = document.getElementById("orders-result");
    const form = document.getElementById("order-form");

    let lunchStudents = [];
    let orders = [];
    let editingOrderId = null;

    const today = todayDate();
    orderDate.value = today;
    document.getElementById("order-form-date").value = today;
    document.getElementById("report-date").value = today;

    function loadStudents() {
        api("GET", "/api/lunch/students").then(students => {
            lunchStudents = students;
            lunchStudents.sort(function (a, b) {
                return a.className.localeCompare(b.className) || a.studentName.localeCompare(b.studentName);
            });

            // Student dropdown for the order form
            let options = "";
            lunchStudents.forEach(function (s) {
                options += '<option value="' + s.studentId + '">' + clean(s.studentName) + " (" + clean(s.className) + ")</option>";
            });
            document.getElementById("order-student").innerHTML = options;

            // Lunch methods table
            let rowsHtml = "";
            lunchStudents.forEach(function (s) {
                rowsHtml += "<tr><td>" + clean(s.studentName) + "</td><td>" + clean(s.className) + "</td>" +
                    '<td><select class="method-select" data-id="' + s.studentId + '">' + makeOptions(METHODS, s.lunchMethod) + "</select></td></tr>";
            });
            document.getElementById("method-rows").innerHTML = rowsHtml;
        });
    }


    /* Orders */

    document.getElementById("load-orders").addEventListener("click", loadOrders);

    function loadOrders() {
        api("GET", "/api/lunch/orders?date=" + orderDate.value).then(list => {
            orders = list;
            if (orders.length === 0) {
                ordersResult.innerHTML = "<p>No orders for this day.</p>";
                return;
            }

            let total = 0;
            let html = '<div class="table-scroll"><table><tr><th>Student</th><th>Class</th><th>Method</th><th>Item</th><th>Paid</th><th>Status</th><th></th></tr>';
            orders.forEach(function (o) {
                if (o.status !== "CANCELLED") {
                    total += Number(o.amountPaid);
                }
                html += "<tr><td>" + clean(o.studentName) + "</td><td>" + clean(o.className) + "</td><td>" + niceText(o.method) +
                    "</td><td>" + clean(o.itemDescription) + "</td><td>" + money(o.amountPaid) + "</td>" +
                    '<td><select class="status-select" data-id="' + o.id + '">' + makeOptions(STATUSES, o.status) + "</select></td>" +
                    '<td><button class="small grey edit-button" data-id="' + o.id + '">Edit</button></td></tr>';
            });
            html += "<tr><th colspan='4'>Total paid (not counting cancelled)</th><th>" + money(total) + "</th><th></th><th></th></tr>";
            html += "</table></div>";
            ordersResult.innerHTML = html;
        }).catch(error => {
            ordersResult.innerHTML = '<p class="error">' + clean(error.message) + "</p>";
        });
    }

    // Changing the status dropdown saves straight away
    ordersResult.addEventListener("change", function (event) {
        if (!event.target.classList.contains("status-select")) return;
        const order = orders.find(o => String(o.id) === event.target.dataset.id);

        api("PUT", "/api/lunch/orders/" + order.id, {
            studentId: order.studentId,
            orderDate: order.orderDate,
            method: order.method,
            status: event.target.value,
            itemDescription: order.itemDescription,
            amountPaid: order.amountPaid
        }).then(() => {
            order.status = event.target.value;
            showMessage("orders-message", order.studentName + "'s order is now " + niceText(order.status) + ".", false);
        }).catch(error => {
            event.target.value = order.status;
            showMessage("orders-message", error.message, true);
        });
    });

    // Clicking Edit puts the order in the form
    ordersResult.addEventListener("click", function (event) {
        if (!event.target.classList.contains("edit-button")) return;
        const order = orders.find(o => String(o.id) === event.target.dataset.id);

        editingOrderId = order.id;
        document.getElementById("order-student").value = order.studentId;
        document.getElementById("order-form-date").value = order.orderDate;
        document.getElementById("order-method").value = order.method;
        document.getElementById("order-item").value = order.itemDescription || "";
        document.getElementById("order-amount").value = order.amountPaid;
        document.getElementById("order-status").value = order.status;
        document.getElementById("form-title").textContent = "Edit Order for " + order.studentName;
        document.getElementById("cancel-button").hidden = false;
        form.scrollIntoView();
    });

    function resetForm() {
        editingOrderId = null;
        form.reset();
        document.getElementById("order-form-date").value = orderDate.value;
        document.getElementById("form-title").textContent = "Record an Order";
        document.getElementById("cancel-button").hidden = true;
    }

    document.getElementById("cancel-button").addEventListener("click", resetForm);

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        const amount = document.getElementById("order-amount").value;

        const order = {
            studentId: Number(document.getElementById("order-student").value),
            orderDate: document.getElementById("order-form-date").value,
            method: document.getElementById("order-method").value || null,
            status: document.getElementById("order-status").value,
            itemDescription: document.getElementById("order-item").value.trim(),
            amountPaid: amount === "" ? null : Number(amount)
        };

        let request;
        if (editingOrderId) {
            request = api("PUT", "/api/lunch/orders/" + editingOrderId, order);
        } else {
            request = api("POST", "/api/lunch/orders", order);
        }

        request.then(saved => {
            showMessage("form-message", "Order saved for " + saved.studentName + ".", false);
            orderDate.value = saved.orderDate;
            resetForm();
            loadOrders();
        }).catch(error => {
            showMessage("form-message", error.message, true);
        });
    });


    /* Lunch methods */

    document.getElementById("method-rows").addEventListener("change", function (event) {
        if (!event.target.classList.contains("method-select")) return;
        const id = event.target.dataset.id;

        api("PUT", "/api/lunch/students/" + id + "/method", { method: event.target.value }).then(saved => {
            showMessage("method-message", saved.studentName + " now gets " + niceText(saved.lunchMethod) + " lunch.", false);
        }).catch(error => {
            showMessage("method-message", error.message, true);
        });
    });


    /* Daily report */

    document.getElementById("load-report").addEventListener("click", function () {
        const result = document.getElementById("report-result");
        api("GET", "/api/lunch/reports/daily?date=" + document.getElementById("report-date").value).then(report => {
            let html = "<p>Total orders: " + report.totalOrders + " | Total paid: " + money(report.totalPayments) + "</p>";
            html += "<table><tr><th>Method</th><th>Orders</th><th>Payments</th></tr>";
            report.byMethod.forEach(function (m) {
                html += "<tr><td>" + niceText(m.method) + "</td><td>" + m.orderCount + "</td><td>" + money(m.paymentTotal) + "</td></tr>";
            });
            html += "</table>";
            result.innerHTML = html;
        }).catch(error => {
            result.innerHTML = '<p class="error">' + clean(error.message) + "</p>";
        });
    });

    loadStudents();
    loadOrders();
});
