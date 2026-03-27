const state = {
  token: localStorage.getItem("quizze_token") || "",
  user: JSON.parse(localStorage.getItem("quizze_user") || "null"),
  view: "loading",
  authMode: "login",
  quizzes: [],
  quizDetail: null,
  attempt: null,
  attemptQuestions: [],
  answers: {},
  attemptHistory: [],
  resultHistory: [],
  currentResult: null,
  adminData: {
    formMode: "create",
    quizId: null,
    quizzes: [],
  },
  message: "",
  error: "",
};

const app = document.getElementById("app");

function setState(patch) {
  Object.assign(state, patch);
  render();
}

function persistAuth(authResponse) {
  state.token = authResponse.accessToken;
  state.user = {
    id: authResponse.userId,
    username: authResponse.username,
    email: authResponse.email,
    role: authResponse.role,
  };
  localStorage.setItem("quizze_token", state.token);
  localStorage.setItem("quizze_user", JSON.stringify(state.user));
}

function clearAuth() {
  localStorage.removeItem("quizze_token");
  localStorage.removeItem("quizze_user");
  Object.assign(state, {
    token: "",
    user: null,
    quizzes: [],
    quizDetail: null,
    attempt: null,
    attemptQuestions: [],
    answers: {},
    attemptHistory: [],
    resultHistory: [],
    currentResult: null,
    adminData: { formMode: "create", quizId: null, quizzes: [] },
    view: "auth",
  });
}

async function api(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(path, { ...options, headers });
  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    const message = payload?.message || "Request failed";
    throw new Error(message);
  }

  return payload.data;
}

function initials(name) {
  return (name || "QS")
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function showMessage(message) {
  state.message = message;
  state.error = "";
  render();
}

function showError(error) {
  state.error = error;
  state.message = "";
  render();
}

function authShell() {
  const isRegister = state.authMode === "register";
  return `
    <div class="auth-shell">
      <main class="auth-wrap">
        <div class="logo-stack">
          <div class="logo-badge">
            <span class="material-symbols-outlined" style="font-variation-settings:'FILL' 1; color: var(--primary); font-size: 38px;">psychology</span>
          </div>
          <div>
            <h1 class="logo-title">Cognitive Sanctuary</h1>
            <p class="logo-subtitle">Enter the focused realm of knowledge</p>
          </div>
        </div>
        <section class="glass-card">
          <h2 class="section-title">${isRegister ? "Create your account" : "Welcome back"}</h2>
          <p class="section-copy">${isRegister ? "Join the focused realm and begin your first challenge." : "Please enter your details to continue your journey."}</p>
          ${state.error ? `<div class="error-banner" style="margin-top:18px;">${escapeHtml(state.error)}</div>` : ""}
          ${state.message ? `<div class="notice" style="margin-top:18px;">${escapeHtml(state.message)}</div>` : ""}
          <form class="form-grid" id="auth-form">
            ${isRegister ? `
              <div class="split-row">
                <div style="flex:1;">
                  <label class="field-label">First Name</label>
                  <input class="field-input" style="padding-left:16px;" name="firstName" placeholder="Niranjan" required>
                </div>
                <div style="flex:1;">
                  <label class="field-label">Last Name</label>
                  <input class="field-input" style="padding-left:16px;" name="lastName" placeholder="Kumar" required>
                </div>
              </div>
            ` : ""}
            <div>
              <label class="field-label">${isRegister ? "Email Address" : "Username or Email"}</label>
              <div class="field-wrap">
                <span class="material-symbols-outlined field-icon">${isRegister ? "mail" : "person"}</span>
                <input class="field-input" name="${isRegister ? "email" : "usernameOrEmail"}" placeholder="${isRegister ? "name@sanctuary.com" : "niranjan"}" required>
              </div>
            </div>
            ${isRegister ? `
              <div>
                <label class="field-label">Username</label>
                <div class="field-wrap">
                  <span class="material-symbols-outlined field-icon">alternate_email</span>
                  <input class="field-input" name="username" placeholder="niranjan" required>
                </div>
              </div>
            ` : ""}
            ${isRegister ? `
              <div>
                <label class="field-label">Email Confirmation</label>
                <div class="field-wrap">
                  <span class="material-symbols-outlined field-icon">verified</span>
                  <input class="field-input" name="confirmEmail" placeholder="Repeat your email" required>
                </div>
              </div>
            ` : ""}
            <div>
              <label class="field-label">Password</label>
              <div class="field-wrap">
                <span class="material-symbols-outlined field-icon">lock</span>
                <input class="field-input" type="password" name="password" placeholder="Password123" required>
              </div>
            </div>
            <button class="gradient-btn" type="submit">${isRegister ? "Create Account" : "Sign In"}</button>
          </form>
          <div class="auth-toggle">
            ${isRegister ? "Already have an account?" : "Don't have an account?"}
            <button type="button" id="toggle-auth">${isRegister ? "Sign In" : "Create an account"}</button>
          </div>
        </section>
      </main>
    </div>
  `;
}

function appLayout(content) {
  const role = state.user?.role || "USER";
  return `
    <div class="layout">
      <aside class="sidebar">
        <div class="brand">
          <h1>Cognitive Sanctuary</h1>
          <p>Premium Quiz Experience</p>
        </div>
        <nav class="nav">
          ${navLink("dashboard", "dashboard", role === "ADMIN" ? "Control Room" : "Dashboard")}
          ${navLink("quiz-library", "quiz", "Quizzes")}
          ${navLink("history", "history", "History")}
          ${navLink("results", "analytics", "Results")}
          ${role === "ADMIN" ? navLink("admin", "admin_panel_settings", "Admin Tools") : ""}
        </nav>
        <div class="sidebar-profile">
          <div class="avatar">${initials(state.user?.username || "Quizze")}</div>
          <div>
            <div style="font-weight:800;">${escapeHtml(state.user?.username || "")}</div>
            <div class="muted tiny">${escapeHtml(role)}</div>
          </div>
        </div>
      </aside>
      <div class="main">
        <header class="topbar">
          <div class="search">
            <span class="material-symbols-outlined">search</span>
            <input id="search-input" placeholder="Search quizzes, categories, attempts...">
          </div>
          <div class="topbar-actions">
            <div class="muted tiny">${escapeHtml(state.user?.email || "")}</div>
            <button class="ghost-btn" style="padding:10px 14px;" id="logout-btn">Logout</button>
          </div>
        </header>
        <main class="canvas">
          ${state.error ? `<div class="error-banner">${escapeHtml(state.error)}</div>` : ""}
          ${state.message ? `<div class="notice">${escapeHtml(state.message)}</div>` : ""}
          ${content}
        </main>
      </div>
    </div>
  `;
}

function navLink(view, icon, label) {
  return `<a href="#" class="nav-link ${state.view === view ? "active" : ""}" data-nav="${view}">
    <span class="material-symbols-outlined">${icon}</span><span>${label}</span>
  </a>`;
}

function dashboardView() {
  const quizzes = state.quizzes.slice(0, 2);
  const attempted = state.attemptHistory.length;
  const avgPct = state.resultHistory.length
    ? Math.round(state.resultHistory.reduce((sum, item) => sum + (item.percentage || 0), 0) / state.resultHistory.length)
    : 0;
  return appLayout(`
    <section class="hero">
      <div>
        <h2>Welcome back, ${escapeHtml(state.user?.username || "Scholar")}.</h2>
        <p>Your cognitive focus is anchored in a calm editorial workspace. Pick up where you left off or begin a new challenge.</p>
      </div>
    </section>
    <section class="stats-grid">
      <article class="stat-card"><div class="stat-label">Quizzes Attempted</div><div class="stat-value" style="color:var(--primary);">${attempted}</div></article>
      <article class="stat-card"><div class="stat-label">Average Score</div><div class="stat-value" style="color:var(--tertiary);">${avgPct}%</div></article>
      <article class="stat-card"><div class="stat-label">Available Quizzes</div><div class="stat-value">${state.quizzes.length}</div></article>
    </section>
    <section class="dashboard-grid">
      <div class="surface-card high">
        <div class="panel-title-row">
          <h3 class="panel-title">Recommended for You</h3>
          <button class="text-btn" data-nav="quiz-library">View Library</button>
        </div>
        <div class="cards-grid">
          ${quizzes.map(quizCard).join("") || `<div class="muted">No published quizzes yet. Ask an admin to publish one.</div>`}
        </div>
      </div>
      <aside class="surface-card">
        <div class="panel-title-row">
          <h3 class="panel-title">Current Momentum</h3>
          <span class="tag alt">Level ${Math.max(1, attempted + 3)}</span>
        </div>
        <div class="progress-shell">
          <div class="progress-track"><div class="progress-fill" style="width:${Math.min(100, avgPct || 12)}%;"></div></div>
          <div class="muted">You need one more strong run to deepen your weekly rhythm.</div>
        </div>
        <div style="height:24px;"></div>
        <h4 class="panel-title">Recent Results</h4>
        <div class="list" style="margin-top:14px;">
          ${state.resultHistory.slice(0, 4).map(result => `
            <div class="list-item">
              <div style="font-weight:700;">${escapeHtml(result.quizTitle)}</div>
              <div class="muted tiny" style="margin-top:6px;">${Math.round(result.percentage || 0)}% score • ${result.correctAnswers} correct</div>
            </div>
          `).join("") || `<div class="muted">No submitted quiz results yet.</div>`}
        </div>
      </aside>
    </section>
  `);
}

function quizCard(quiz) {
  return `
    <article class="quiz-card">
      <div class="quiz-card-cover">
        <span class="tag">${escapeHtml(quiz.categoryName || quiz.difficulty || "Quiz")}</span>
      </div>
      <div class="quiz-card-body">
        <div>
          <h4>${escapeHtml(quiz.title)}</h4>
          <p class="muted">${escapeHtml(quiz.description || "A focused assessment built to sharpen your recall and reasoning.")}</p>
        </div>
        <div class="helper-row">
          <div class="tiny muted">${quiz.timeLimitInMinutes} mins • ${quiz.questionCount} questions</div>
          <button class="gradient-btn" style="padding:10px 14px;" data-action="open-quiz" data-id="${quiz.id}">Enter</button>
        </div>
      </div>
    </article>
  `;
}

function quizLibraryView() {
  return appLayout(`
    <section class="hero">
      <div>
        <h2>Quiz Library</h2>
        <p>Published quizzes are presented with generous darkspace and sharp typographic rhythm, just as the stitch concepts intended.</p>
      </div>
    </section>
    <section class="cards-grid">
      ${state.quizzes.map(quizCard).join("") || `<div class="surface-card">No published quizzes available.</div>`}
    </section>
  `);
}

function quizDetailView() {
  const quiz = state.quizDetail;
  return appLayout(`
    <section class="surface-card high">
      <div class="hero">
        <div>
          <span class="tag">${escapeHtml(quiz.categoryName || quiz.difficulty)}</span>
          <h2 style="margin-top:16px;">${escapeHtml(quiz.title)}</h2>
          <p>${escapeHtml(quiz.description || "A premium challenge designed for focused recall.")}</p>
        </div>
        <div class="surface-card" style="min-width:280px;">
          <div class="list">
            <div><div class="stat-label">Difficulty</div><div style="font-weight:800; margin-top:8px;">${escapeHtml(quiz.difficulty)}</div></div>
            <div><div class="stat-label">Duration</div><div style="font-weight:800; margin-top:8px;">${quiz.timeLimitInMinutes} Minutes</div></div>
            <div><div class="stat-label">Questions</div><div style="font-weight:800; margin-top:8px;">${quiz.questionCount}</div></div>
            <button class="gradient-btn" data-action="start-quiz" data-id="${quiz.id}">Start Quiz</button>
          </div>
        </div>
      </div>
    </section>
  `);
}

function quizAttemptView() {
  const currentIndex = state.attempt.currentIndex || 0;
  const question = state.attemptQuestions[currentIndex];
  const progress = ((currentIndex + 1) / state.attemptQuestions.length) * 100;
  return appLayout(`
    <section class="quiz-screen">
      <div class="hero">
        <div>
          <span class="tag">Question ${currentIndex + 1} of ${state.attemptQuestions.length}</span>
          <h2 style="margin-top:16px;">${escapeHtml(question.content)}</h2>
        </div>
        <div style="width:min(100%, 280px);" class="progress-shell">
          <div class="helper-row tiny muted"><span>Progress</span><span>${Math.round(progress)}%</span></div>
          <div class="progress-track"><div class="progress-fill" style="width:${progress}%"></div></div>
        </div>
      </div>
      <section class="question-grid">
        ${question.options.map((option, index) => {
          const selected = state.answers[question.id] === option.id;
          return `
            <button class="option-btn ${selected ? "selected" : ""}" data-action="select-option" data-question="${question.id}" data-option="${option.id}">
              <span class="option-badge">${String.fromCharCode(65 + index)}</span>
              <div>
                <div style="font-size:1.08rem; font-weight:700;">${escapeHtml(option.content)}</div>
              </div>
            </button>
          `;
        }).join("")}
      </section>
      <div class="helper-row" style="margin-top:28px;">
        <button class="text-btn" ${currentIndex === 0 ? "disabled" : ""} data-action="prev-question">Previous Question</button>
        ${currentIndex === state.attemptQuestions.length - 1
          ? `<button class="gradient-btn" data-action="submit-quiz">Submit Quiz</button>`
          : `<button class="gradient-btn" data-action="next-question">Next Question</button>`}
      </div>
    </section>
  `);
}

function resultsView() {
  if (!state.currentResult) {
    return appLayout(`
      <section class="surface-card">
        <div class="panel-title-row">
          <h3 class="panel-title">Result History</h3>
        </div>
        <div class="list">
          ${state.resultHistory.map(item => `
            <div class="list-item helper-row">
              <div>
                <div style="font-weight:800;">${escapeHtml(item.quizTitle)}</div>
                <div class="muted tiny">${Math.round(item.percentage || 0)}% • ${item.correctAnswers} correct • ${new Date(item.submittedAt).toLocaleString()}</div>
              </div>
              <button class="gradient-btn" style="padding:10px 14px;" data-action="open-result" data-id="${item.attemptId}">Review</button>
            </div>
          `).join("") || `<div class="muted">No submitted results yet.</div>`}
        </div>
      </section>
    `);
  }

  const result = state.currentResult;
  const scorePct = Math.round(result.percentage || 0);
  const scoreDeg = Math.round((scorePct / 100) * 360);
  return appLayout(`
    <section class="result-grid">
      <div class="surface-card">
        <h2 style="margin:0; font-size:2.6rem;">Quiz Completed</h2>
        <p class="muted">${escapeHtml(result.quizTitle)}</p>
        <div style="display:flex; gap:28px; align-items:center; margin-top:28px;">
          <div class="score-ring" style="--score-deg:${scoreDeg}deg;">
            <div class="score-ring-inner">
              <div style="font-size:2rem; font-weight:900;">${scorePct}%</div>
              <div class="tiny muted">Score</div>
            </div>
          </div>
          <div class="list" style="flex:1;">
            <div><strong>${result.score}</strong> / ${result.maxScore} points</div>
            <div><strong>${result.correctAnswers}</strong> correct answers</div>
            <div><strong>${result.wrongAnswers}</strong> wrong answers</div>
          </div>
        </div>
        <div class="stats-grid" style="margin-top:28px;">
          <div class="stat-card"><div class="stat-label">Attempted</div><div class="stat-value" style="font-size:1.8rem;">${result.attemptedQuestions}</div></div>
          <div class="stat-card"><div class="stat-label">Correct</div><div class="stat-value" style="font-size:1.8rem; color:var(--primary);">${result.correctAnswers}</div></div>
          <div class="stat-card"><div class="stat-label">Wrong</div><div class="stat-value" style="font-size:1.8rem; color:var(--error);">${result.wrongAnswers}</div></div>
        </div>
      </div>
      <div class="surface-card high">
        <div class="panel-title-row">
          <h3 class="panel-title">Review Answers</h3>
          <button class="ghost-btn" style="padding:10px 14px;" data-action="close-result">Back</button>
        </div>
        <div class="list">
          ${result.answers.map(answer => `
            <div class="review-item ${answer.correct ? "" : "incorrect"}">
              <div class="helper-row">
                <span class="stat-label">Question ${answer.questionId}</span>
                <span class="badge ${answer.correct ? "live" : "draft"}">${answer.correct ? "Correct" : "Incorrect"}</span>
              </div>
              <div style="margin-top:12px; font-size:1.05rem; font-weight:700;">${escapeHtml(answer.questionContent)}</div>
              <div style="margin-top:14px;" class="${answer.correct ? "notice" : "error-banner"}">Your answer: ${escapeHtml(answer.selectedOptionContent || "No answer")}</div>
            </div>
          `).join("")}
        </div>
      </div>
    </section>
  `);
}

function historyView() {
  return appLayout(`
    <section class="surface-card">
      <div class="panel-title-row">
        <h3 class="panel-title">Attempt History</h3>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Quiz</th><th>Status</th><th>Score</th><th>Percentage</th><th>Submitted</th></tr>
          </thead>
          <tbody>
            ${state.attemptHistory.map(item => `
              <tr>
                <td>${escapeHtml(item.quizTitle)}</td>
                <td>${escapeHtml(item.status)}</td>
                <td>${item.score ?? 0} / ${item.maxScore ?? 0}</td>
                <td>${Math.round(item.percentage || 0)}%</td>
                <td>${item.submittedAt ? new Date(item.submittedAt).toLocaleString() : "-"}</td>
              </tr>
            `).join("") || `<tr><td colspan="5" class="muted">No attempts yet.</td></tr>`}
          </tbody>
        </table>
      </div>
    </section>
  `);
}

function adminView() {
  const quizzes = state.adminData.quizzes;
  return appLayout(`
    <section class="hero">
      <div>
        <h2>Admin Dashboard</h2>
        <p>System overview and quiz management in the same editorial dark-mode language as the stitch concepts.</p>
      </div>
    </section>
    <section class="metric-grid">
      <article class="stat-card"><div class="stat-label">Published</div><div class="stat-value" style="color:var(--primary);">${quizzes.filter(q => q.published).length}</div></article>
      <article class="stat-card"><div class="stat-label">Drafts</div><div class="stat-value" style="color:var(--tertiary);">${quizzes.filter(q => !q.published).length}</div></article>
      <article class="stat-card"><div class="stat-label">Results Recorded</div><div class="stat-value">${state.resultHistory.length}</div></article>
      <article class="stat-card"><div class="stat-label">User Attempts</div><div class="stat-value">${state.attemptHistory.length}</div></article>
    </section>
    <section class="dashboard-grid">
      <div class="surface-card high">
        <div class="panel-title-row">
          <h3 class="panel-title">${state.adminData.formMode === "edit" ? "Edit Quiz" : "Create New Quiz"}</h3>
        </div>
        <form id="admin-quiz-form" class="form-grid">
          <div>
            <label class="field-label">Quiz Title</label>
            <input class="field-input" style="padding-left:16px;" name="title" placeholder="Java Basics" value="${escapeHtml(state.adminData.activeQuiz?.title || "")}" required>
          </div>
          <div>
            <label class="field-label">Description</label>
            <textarea class="field-textarea" name="description" placeholder="Fundamentals of Java programming">${escapeHtml(state.adminData.activeQuiz?.description || "")}</textarea>
          </div>
          <div class="split-row">
            <div style="flex:1;">
              <label class="field-label">Category</label>
              <input class="field-input" style="padding-left:16px;" name="categoryName" placeholder="Programming" value="${escapeHtml(state.adminData.activeQuiz?.categoryName || "")}">
            </div>
            <div style="flex:1;">
              <label class="field-label">Difficulty</label>
              <select class="field-select" name="difficulty">
                ${["EASY", "MEDIUM", "HARD"].map(level => `<option value="${level}" ${state.adminData.activeQuiz?.difficulty === level ? "selected" : ""}>${level}</option>`).join("")}
              </select>
            </div>
          </div>
          <div class="split-row">
            <div style="flex:1;">
              <label class="field-label">Time Limit</label>
              <input class="field-input" style="padding-left:16px;" type="number" min="0" max="300" name="timeLimitInMinutes" value="${state.adminData.activeQuiz?.timeLimitInMinutes ?? 15}">
            </div>
            <div style="flex:1; display:grid; gap:12px; padding-top:26px;">
              ${checkbox("published", "Published", !!state.adminData.activeQuiz?.published)}
              ${checkbox("oneAttemptOnly", "One Attempt Only", !!state.adminData.activeQuiz?.oneAttemptOnly)}
            </div>
          </div>
          <div class="helper-row">
            <button class="gradient-btn" type="submit">${state.adminData.formMode === "edit" ? "Update Quiz" : "Create Quiz"}</button>
            ${state.adminData.formMode === "edit" ? `<button class="ghost-btn" type="button" id="cancel-edit">Cancel</button>` : ""}
          </div>
        </form>
      </div>
      <div class="surface-card">
        <div class="panel-title-row"><h3 class="panel-title">Manage Quizzes</h3></div>
        <div class="list">
          ${quizzes.map(quiz => `
            <div class="list-item helper-row">
              <div>
                <div style="font-weight:800;">${escapeHtml(quiz.title)}</div>
                <div class="muted tiny">${escapeHtml(quiz.categoryName || "Uncategorized")} • ${quiz.questionCount} questions • ${quiz.published ? "Published" : "Draft"}</div>
              </div>
              <div style="display:flex; gap:8px;">
                <button class="pill-btn" data-action="edit-quiz" data-id="${quiz.id}">Edit</button>
                <button class="ghost-btn" data-action="delete-quiz" data-id="${quiz.id}">Delete</button>
              </div>
            </div>
          `).join("") || `<div class="muted">No quizzes created yet.</div>`}
        </div>
      </div>
    </section>
  `);
}

function checkbox(name, label, checked) {
  return `<label class="tiny muted"><input type="checkbox" name="${name}" ${checked ? "checked" : ""}> ${label}</label>`;
}

function render() {
  if (state.view === "loading") {
    app.innerHTML = `<div class="auth-shell"><div class="glass-card">Loading interface...</div></div>`;
    return;
  }

  switch (state.view) {
    case "auth":
      app.innerHTML = authShell();
      bindAuthEvents();
      break;
    case "dashboard":
      app.innerHTML = dashboardView();
      bindAppEvents();
      break;
    case "quiz-library":
      app.innerHTML = quizLibraryView();
      bindAppEvents();
      break;
    case "quiz-detail":
      app.innerHTML = quizDetailView();
      bindAppEvents();
      break;
    case "quiz-attempt":
      app.innerHTML = quizAttemptView();
      bindAppEvents();
      break;
    case "history":
      app.innerHTML = historyView();
      bindAppEvents();
      break;
    case "results":
      app.innerHTML = resultsView();
      bindAppEvents();
      break;
    case "admin":
      app.innerHTML = adminView();
      bindAppEvents();
      break;
    default:
      app.innerHTML = authShell();
      bindAuthEvents();
  }
}

function bindAuthEvents() {
  document.getElementById("toggle-auth")?.addEventListener("click", () => {
    state.authMode = state.authMode === "login" ? "register" : "login";
    state.error = "";
    state.message = "";
    render();
  });

  document.getElementById("auth-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    state.error = "";
    state.message = "";

    try {
      if (state.authMode === "register") {
        if (formData.get("email") !== formData.get("confirmEmail")) {
          throw new Error("Email confirmation does not match");
        }

        await api("/api/auth/register", {
          method: "POST",
          body: JSON.stringify({
            firstName: formData.get("firstName"),
            lastName: formData.get("lastName"),
            email: formData.get("email"),
            username: formData.get("username"),
            password: formData.get("password"),
          }),
        });
        state.authMode = "login";
        showMessage("Account created. Sign in to enter the sanctuary.");
      } else {
        const auth = await api("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({
            usernameOrEmail: formData.get("usernameOrEmail"),
            password: formData.get("password"),
          }),
        });
        persistAuth(auth);
        await bootstrapApp();
      }
    } catch (error) {
      showError(error.message);
    }
  });
}

function bindAppEvents() {
  document.querySelectorAll("[data-nav]").forEach((element) => {
    element.addEventListener("click", async (event) => {
      event.preventDefault();
      const view = element.dataset.nav;
      state.currentResult = null;
      if (view === "dashboard") {
        await loadDashboard();
      } else if (view === "quiz-library") {
        await loadQuizzes();
        state.view = "quiz-library";
        render();
      } else if (view === "history") {
        await loadHistory();
        state.view = "history";
        render();
      } else if (view === "results") {
        await loadResults();
        state.view = "results";
        render();
      } else if (view === "admin") {
        await loadAdmin();
      }
    });
  });

  document.getElementById("logout-btn")?.addEventListener("click", () => {
    clearAuth();
    render();
  });

  document.querySelectorAll("[data-action='open-quiz']").forEach((button) => {
    button.addEventListener("click", () => openQuizDetail(Number(button.dataset.id)));
  });

  document.querySelector("[data-action='start-quiz']")?.addEventListener("click", async (event) => {
    await startQuiz(Number(event.currentTarget.dataset.id));
  });

  document.querySelectorAll("[data-action='select-option']").forEach((button) => {
    button.addEventListener("click", () => {
      state.answers[button.dataset.question] = Number(button.dataset.option);
      render();
    });
  });

  document.querySelector("[data-action='next-question']")?.addEventListener("click", () => {
    const currentQuestion = state.attemptQuestions[state.attempt.currentIndex];
    if (!state.answers[currentQuestion.id]) {
      showError("Select an option before moving forward.");
      return;
    }
    state.error = "";
    state.attempt.currentIndex += 1;
    render();
  });

  document.querySelector("[data-action='prev-question']")?.addEventListener("click", () => {
    state.attempt.currentIndex = Math.max(0, state.attempt.currentIndex - 1);
    render();
  });

  document.querySelector("[data-action='submit-quiz']")?.addEventListener("click", submitAttempt);

  document.querySelectorAll("[data-action='open-result']").forEach((button) => {
    button.addEventListener("click", async () => {
      state.currentResult = await api(`/api/quizzes/attempts/${button.dataset.id}/result`);
      state.view = "results";
      render();
    });
  });

  document.querySelector("[data-action='close-result']")?.addEventListener("click", async () => {
    state.currentResult = null;
    await loadResults();
    state.view = "results";
    render();
  });

  document.getElementById("admin-quiz-form")?.addEventListener("submit", submitAdminQuiz);
  document.getElementById("cancel-edit")?.addEventListener("click", () => {
    state.adminData = { ...state.adminData, formMode: "create", quizId: null, activeQuiz: null };
    render();
  });

  document.querySelectorAll("[data-action='edit-quiz']").forEach((button) => {
    button.addEventListener("click", () => {
      const quiz = state.adminData.quizzes.find((item) => item.id === Number(button.dataset.id));
      state.adminData = { ...state.adminData, formMode: "edit", quizId: quiz.id, activeQuiz: quiz };
      render();
    });
  });

  document.querySelectorAll("[data-action='delete-quiz']").forEach((button) => {
    button.addEventListener("click", async () => {
      if (!confirm("Delete this quiz?")) return;
      try {
        await api(`/api/admin/quizzes/${button.dataset.id}`, { method: "DELETE" });
        showMessage("Quiz deleted successfully.");
        await loadAdmin();
      } catch (error) {
        showError(error.message);
      }
    });
  });
}

async function bootstrapApp() {
  try {
    await Promise.all([loadQuizzes(), loadHistory(), loadResults()]);
    if (state.user?.role === "ADMIN") {
      await loadAdminData();
      state.view = "admin";
    } else {
      state.view = "dashboard";
    }
    state.error = "";
    state.message = "";
    render();
  } catch (error) {
    clearAuth();
    showError(error.message);
  }
}

async function loadDashboard() {
  await Promise.all([loadQuizzes(), loadHistory(), loadResults()]);
  state.view = "dashboard";
  render();
}

async function loadQuizzes() {
  state.quizzes = await api("/api/quizzes");
}

async function loadHistory() {
  state.attemptHistory = await api("/api/users/me/attempts");
}

async function loadResults() {
  state.resultHistory = await api("/api/users/me/results");
}

async function loadAdminData() {
  const drafts = await api("/api/admin/quizzes");
  state.adminData.quizzes = drafts;
}

async function loadAdmin() {
  await Promise.all([loadAdminData(), loadHistory(), loadResults()]);
  state.view = "admin";
  render();
}

async function openQuizDetail(id) {
  try {
    state.quizDetail = await api(`/api/quizzes/${id}`);
    state.view = "quiz-detail";
    state.error = "";
    render();
  } catch (error) {
    showError(error.message);
  }
}

async function startQuiz(id) {
  try {
    const attempt = await api(`/api/quizzes/${id}/start`, { method: "POST" });
    const questions = await api(`/api/quizzes/attempts/${attempt.attemptId}/questions`);
    state.attempt = { ...attempt, currentIndex: 0 };
    state.attemptQuestions = questions;
    state.answers = {};
    state.view = "quiz-attempt";
    state.error = "";
    render();
  } catch (error) {
    showError(error.message);
  }
}

async function submitAttempt() {
  const unanswered = state.attemptQuestions.find((question) => !state.answers[question.id]);
  if (unanswered) {
    showError("Answer every question before submitting.");
    return;
  }

  try {
    const payload = {
      answers: state.attemptQuestions.map((question) => ({
        questionId: question.id,
        selectedOptionId: state.answers[question.id],
      })),
    };

    await api(`/api/quizzes/attempts/${state.attempt.attemptId}/submit`, {
      method: "POST",
      body: JSON.stringify(payload),
    });

    state.currentResult = await api(`/api/quizzes/attempts/${state.attempt.attemptId}/result`);
    await Promise.all([loadHistory(), loadResults(), loadQuizzes()]);
    state.view = "results";
    state.attempt = null;
    state.attemptQuestions = [];
    state.answers = {};
    render();
  } catch (error) {
    showError(error.message);
  }
}

async function submitAdminQuiz(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const body = {
    title: formData.get("title"),
    description: formData.get("description"),
    categoryName: formData.get("categoryName"),
    difficulty: formData.get("difficulty"),
    timeLimitInMinutes: Number(formData.get("timeLimitInMinutes")),
    published: formData.get("published") === "on",
    negativeMarkingEnabled: false,
    oneAttemptOnly: formData.get("oneAttemptOnly") === "on",
  };

  try {
    if (state.adminData.formMode === "edit" && state.adminData.quizId) {
      await api(`/api/admin/quizzes/${state.adminData.quizId}`, {
        method: "PUT",
        body: JSON.stringify(body),
      });
      showMessage("Quiz updated successfully.");
    } else {
      await api("/api/admin/quizzes", {
        method: "POST",
        body: JSON.stringify(body),
      });
      showMessage("Quiz created successfully.");
    }
    state.adminData = { ...state.adminData, formMode: "create", quizId: null, activeQuiz: null };
    await loadAdmin();
  } catch (error) {
    showError(error.message);
  }
}

(async function init() {
  render();
  if (state.token && state.user) {
    await bootstrapApp();
  } else {
    state.view = "auth";
    render();
  }
})();
