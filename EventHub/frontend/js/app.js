/* ============================================================
   EventHub — frontend/js/app.js
   Vanilla JS front end.
   Set API_BASE to the deployed Java backend to use live data;
   when the backend is unreachable the demo dataset below is used
   so the UI always works (booking simulation with confirmations).
   ============================================================ */

const API_BASE = "http://localhost:8080/eventhub/api";

/* ---------------- demo fallback data ---------------- */
const DEMO_EVENTS = [
  { eventId: 1, title: "Midnight Echoes Live", category: "CONCERT",
    venueName: "Phoenix Arena", venueCity: "Bengaluru", daysAway: 21, seatsLeft: 2930,
    categories: [ {categoryId:1,name:"VIP",price:4500,seats:200},
                  {categoryId:2,name:"GOLD",price:2500,seats:780},
                  {categoryId:3,name:"SILVER",price:1200,seats:1950} ] },
  { eventId: 2, title: "City Premier Cup Final", category: "SPORTS",
    venueName: "Nehru Indoor Stadium", venueCity: "Chennai", daysAway: 35, seatsLeft: 4140,
    categories: [ {categoryId:4,name:"VIP",price:3000,seats:290},
                  {categoryId:5,name:"GENERAL",price:800,seats:3850} ] },
  { eventId: 3, title: "Rajasthan Folk Festival", category: "CULTURAL",
    venueName: "Heritage Open Grounds", venueCity: "Jaipur", daysAway: 14, seatsLeft: 2900,
    categories: [ {categoryId:6,name:"GOLD",price:1500,seats:500},
                  {categoryId:7,name:"GENERAL",price:600,seats:2400} ] },
  { eventId: 4, title: "Classical Evening", category: "CULTURAL",
    venueName: "Nehru Indoor Stadium", venueCity: "Chennai", daysAway: 48, seatsLeft: 1240,
    categories: [ {categoryId:8,name:"GOLD",price:1800,seats:250},
                  {categoryId:9,name:"SILVER",price:900,seats:990} ] },
];

/* ---------------- state ---------------- */
const state = {
  user: JSON.parse(localStorage.getItem("eh_user") || "null"),
  events: [],
  filter: "ALL",
  bookings: JSON.parse(localStorage.getItem("eh_bookings") || "[]"),
  current: null,
  seq: Number(localStorage.getItem("eh_seq") || 1003),
};

/* ---------------- helpers ---------------- */
const $  = (s) => document.querySelector(s);
const $$ = (s) => Array.from(document.querySelectorAll(s));
const money = (n) => "₹" + Number(n).toLocaleString("en-IN");
const dateFrom = (days) =>
  new Date(Date.now() + days * 864e5).toLocaleDateString("en-IN",
    { weekday: "short", day: "numeric", month: "short", year: "numeric" });

function toast(msg, ms = 4200) {
  const t = $("#toast");
  t.textContent = msg;
  t.classList.remove("hidden");
  clearTimeout(toast._t);
  toast._t = setTimeout(() => t.classList.add("hidden"), ms);
}

async function api(path, options) {
  const res = await fetch(API_BASE + path, options);
  if (!res.ok) throw new Error((await res.json()).error || "Request failed");
  return res.json();
}

/* ---------------- theme ---------------- */
const savedTheme = localStorage.getItem("eh_theme") || "dark";
document.documentElement.dataset.theme = savedTheme;
$("#themeToggle").textContent = savedTheme === "dark" ? "🌙" : "☀️";
$("#themeToggle").addEventListener("click", () => {
  const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
  document.documentElement.dataset.theme = next;
  localStorage.setItem("eh_theme", next);
  $("#themeToggle").textContent = next === "dark" ? "🌙" : "☀️";
});

/* ---------------- events ---------------- */
async function loadEvents() {
  try {
    const rows = await api("/events");
    state.events = rows.map((e) => ({ ...e, categories: e.categories || [] }));
  } catch {
    state.events = DEMO_EVENTS;            // backend offline → demo mode
  }
  renderEvents();
}

function renderEvents() {
  const grid = $("#eventGrid");
  const list = state.filter === "ALL"
    ? state.events
    : state.events.filter((e) => e.category === state.filter);

  if (!list.length) { grid.innerHTML = `<p class="muted">No events in this category.</p>`; return; }

  grid.innerHTML = list.map((e) => {
    const min = Math.min(...e.categories.map((c) => c.price));
    return `
      <article class="card">
        <span class="card-tag">${e.category}</span>
        <h3>${e.title}</h3>
        <p class="meta">${dateFrom(e.daysAway)}</p>
        <p class="meta">${e.venueName}, ${e.venueCity}</p>
        <div class="row">
          <div>
            <div class="price">${money(min)}<span class="meta"> onwards</span></div>
            <div class="seats ${e.seatsLeft < 500 ? "low" : ""}">${e.seatsLeft} seats left</div>
          </div>
          <button class="btn btn-primary" data-book="${e.eventId}">Book</button>
        </div>
      </article>`;
  }).join("");

  $$("[data-book]").forEach((b) =>
    b.addEventListener("click", () => openBooking(Number(b.dataset.book))));
}

$$("#filters .chip").forEach((chip) =>
  chip.addEventListener("click", () => {
    $$("#filters .chip").forEach((c) => c.classList.remove("is-active"));
    chip.classList.add("is-active");
    state.filter = chip.dataset.cat;
    renderEvents();
  }));

/* ---------------- auth ---------------- */
function renderUser() {
  const badge = $("#userBadge");
  if (state.user) {
    badge.textContent = `${state.user.fullName} · ${state.user.role}`;
    badge.classList.remove("hidden");
    $("#authBtn").classList.add("hidden");
    $("#logoutBtn").classList.remove("hidden");
  } else {
    badge.classList.add("hidden");
    $("#authBtn").classList.remove("hidden");
    $("#logoutBtn").classList.add("hidden");
  }
}

function openModal(id)  { $(id).classList.remove("hidden"); }
function closeModal(id) { $(id).classList.add("hidden"); }

$("#authBtn").addEventListener("click", () => openModal("#authModal"));
$("#logoutBtn").addEventListener("click", () => {
  state.user = null; localStorage.removeItem("eh_user"); renderUser(); toast("Signed out.");
});
$$("[data-close]").forEach((b) => b.addEventListener("click", (e) => e.target.closest(".modal").classList.add("hidden")));
$$(".modal").forEach((m) => m.addEventListener("click", (e) => { if (e.target === m) m.classList.add("hidden"); }));

$$(".tab").forEach((tab) =>
  tab.addEventListener("click", () => {
    $$(".tab").forEach((t) => t.classList.remove("is-active"));
    tab.classList.add("is-active");
    $("#loginForm").classList.toggle("hidden", tab.dataset.tab !== "login");
    $("#registerForm").classList.toggle("hidden", tab.dataset.tab !== "register");
  }));

$("#loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  const email = f.get("email"), password = f.get("password");
  try {
    state.user = await api("/login", { method: "POST", body: new URLSearchParams({ email, password }) });
  } catch {
    // demo fallback
    const demo = { "admin@eventhub.com": ["admin123", "Admin Kumar", "ADMIN"],
                   "riya@example.com":   ["user123",  "Riya Sharma", "CUSTOMER"] }[email];
    if (!demo || demo[0] !== password) return toast("Invalid e-mail or password.");
    state.user = { userId: demo[2] === "ADMIN" ? 1 : 2, fullName: demo[1], email, role: demo[2] };
  }
  localStorage.setItem("eh_user", JSON.stringify(state.user));
  renderUser(); closeModal("#authModal");
  toast(`Welcome back, ${state.user.fullName}!`);
});

$("#registerForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  const body = new URLSearchParams({
    name: f.get("name"), email: f.get("email"),
    phone: f.get("phone") || "", password: f.get("password") });
  try {
    state.user = await api("/register", { method: "POST", body });
  } catch {
    state.user = { userId: Date.now() % 10000, fullName: f.get("name"), email: f.get("email"), role: "CUSTOMER" };
  }
  localStorage.setItem("eh_user", JSON.stringify(state.user));
  renderUser(); closeModal("#authModal");
  toast("Account created. You're signed in.");
});

/* ---------------- booking ---------------- */
function openBooking(eventId) {
  if (!state.user) { openModal("#authModal"); return toast("Please sign in to book tickets."); }

  const ev = state.events.find((e) => e.eventId === eventId);
  state.current = ev;

  $("#bookTitle").textContent = ev.title;
  $("#bookMeta").textContent  = `${dateFrom(ev.daysAway)} · ${ev.venueName}, ${ev.venueCity}`;
  $("#bookCategory").innerHTML = ev.categories
    .map((c) => `<option value="${c.categoryId}" data-price="${c.price}">${c.name} — ${money(c.price)} (${c.seats} left)</option>`)
    .join("");
  updateTotal();
  openModal("#bookModal");
}

function updateTotal() {
  const opt = $("#bookCategory").selectedOptions[0];
  if (!opt) return;
  const qty = Number($("#bookQty").value || 1);
  $("#bookTotal").textContent = money(Number(opt.dataset.price) * qty);
}
$("#bookCategory").addEventListener("change", updateTotal);
$("#bookQty").addEventListener("input", updateTotal);

$("#bookForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const ev  = state.current;
  const opt = $("#bookCategory").selectedOptions[0];
  const categoryId = Number(opt.value);
  const qty = Number($("#bookQty").value);
  const cat = ev.categories.find((c) => c.categoryId === categoryId);

  if (qty > cat.seats) return toast(`Only ${cat.seats} seat(s) left in ${cat.name}.`);

  let result;
  try {
    result = await api("/book", { method: "POST", body: new URLSearchParams({
      userId: state.user.userId, eventId: ev.eventId, categoryId, quantity: qty }) });
  } catch {
    // simulated transaction: decrement stock, mint BK-/TKT- numbers
    state.seq += 1;
    localStorage.setItem("eh_seq", state.seq);
    cat.seats   -= qty;
    ev.seatsLeft -= qty;
    result = {
      bookingNumber: `BK-${state.seq}`,
      totalAmount:   cat.price * qty,
      ticketNumbers: Array.from({ length: qty }, (_, i) => `TKT-${state.seq * 10 + i}`),
    };
  }

  state.bookings.unshift({
    bookingNumber: result.bookingNumber,
    event: ev.title, category: cat.name, quantity: qty,
    total: result.totalAmount, tickets: result.ticketNumbers,
  });
  localStorage.setItem("eh_bookings", JSON.stringify(state.bookings));

  closeModal("#bookModal");
  renderEvents();
  renderBookings();
  alert(`Booking confirmed!\n\nBooking no: ${result.bookingNumber}\nEvent: ${ev.title}\nCategory: ${cat.name} x${qty}\nTotal: ${money(result.totalAmount)}\nTickets: ${result.ticketNumbers.join(", ")}`);
  toast(`${result.bookingNumber} confirmed — ${qty} digital ticket(s) issued.`);
});

function renderBookings() {
  const box = $("#bookingList");
  if (!state.bookings.length) {
    box.innerHTML = `<p class="muted">No bookings yet. Pick an event above to get started.</p>`;
    return;
  }
  box.innerHTML = state.bookings.map((b) => `
    <div class="booking">
      <code>${b.bookingNumber}</code>
      <strong>${b.event}</strong>
      <span class="muted">${b.category} × ${b.quantity}</span>
      <span class="muted">${b.tickets.join(", ")}</span>
      <span style="margin-left:auto"><strong>${money(b.total)}</strong></span>
    </div>`).join("");
}

/* ---------------- init ---------------- */
$("#year").textContent = new Date().getFullYear();
renderUser();
renderBookings();
loadEvents();
