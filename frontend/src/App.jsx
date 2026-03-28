import { useEffect, useMemo, useRef, useState } from "react";
import {
  Link,
  Navigate,
  NavLink,
  Outlet,
  Route,
  Routes,
  useNavigate,
  useParams,
} from "react-router-dom";

const STORAGE_TOKEN = "quizze_token";
const STORAGE_USER = "quizze_user";

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_USER) || "null");
  } catch {
    return null;
  }
}

function initials(name) {
  return (name || "QS")
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

async function apiRequest(path, options = {}, token) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(path, { ...options, headers });
  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(payload?.message || "Request failed");
  }

  return payload.data;
}

function buildQueryString(params) {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      searchParams.set(key, value);
    }
  });

  const query = searchParams.toString();
  return query ? `?${query}` : "";
}

function formatRemainingTime(targetTime) {
  if (!targetTime) {
    return null;
  }

  const diff = new Date(targetTime).getTime() - Date.now();
  if (diff <= 0) {
    return "00:00";
  }

  const totalSeconds = Math.floor(diff / 1000);
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function Button({ children, className = "", ...props }) {
  return (
    <button className={`btn ${className}`.trim()} {...props}>
      {children}
    </button>
  );
}

function Card({ className = "", children }) {
  return <section className={`card ${className}`.trim()}>{children}</section>;
}

function Field({ icon, label, name, placeholder, type = "text", as = "input", defaultValue, value, ...props }) {
  return (
    <div className="field">
      <label className="field-label">{label}</label>
      <div className={`field-shell${icon ? " with-icon" : ""}`}>
        {icon ? <span className="material-symbols-outlined field-icon">{icon}</span> : null}
        {as === "textarea" ? (
          <textarea
            className="field-input field-textarea"
            defaultValue={defaultValue}
            name={name}
            placeholder={placeholder}
            value={value}
            {...props}
          />
        ) : (
          <input
            className="field-input"
            defaultValue={defaultValue}
            name={name}
            placeholder={placeholder}
            required={props.required ?? true}
            type={type}
            value={value}
            {...props}
          />
        )}
      </div>
    </div>
  );
}

function CustomSelect({ label, name, value, onChangeValue, options, placeholder = "Select option" }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);

  useEffect(() => {
    function handlePointerDown(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, []);

  const selectedOption = options.find((option) => option.value === value);

  return (
    <div className="field" ref={rootRef}>
      {label ? <label className="field-label">{label}</label> : null}
      <input name={name} type="hidden" value={value || ""} />
      <button
        aria-expanded={open}
        className={`custom-select-trigger${open ? " open" : ""}`}
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        <span className={selectedOption ? "custom-select-value" : "custom-select-placeholder"}>
          {selectedOption?.label || placeholder}
        </span>
        <span className="material-symbols-outlined custom-select-icon">expand_more</span>
      </button>

      {open ? (
        <div className="custom-select-menu" role="listbox">
          {options.map((option) => (
            <button
              className={`custom-select-option${option.value === value ? " selected" : ""}`}
              key={`${name}-${option.value}`}
              onClick={() => {
                onChangeValue(option.value);
                setOpen(false);
              }}
              type="button"
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem(STORAGE_TOKEN) || "");
  const [user, setUser] = useState(() => readStoredUser());
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const auth = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token && user),
      login(authResponse) {
        const nextUser = {
          id: authResponse.userId,
          username: authResponse.username,
          email: authResponse.email,
          role: authResponse.role,
        };

        localStorage.setItem(STORAGE_TOKEN, authResponse.accessToken);
        localStorage.setItem(STORAGE_USER, JSON.stringify(nextUser));
        setToken(authResponse.accessToken);
        setUser(nextUser);
      },
      logout() {
        localStorage.removeItem(STORAGE_TOKEN);
        localStorage.removeItem(STORAGE_USER);
        setToken("");
        setUser(null);
      },
    }),
    [token, user],
  );

  const sharedProps = { auth, message, setMessage, error, setError };

  return (
    <Routes>
      <Route
        path="/auth"
        element={
          auth.isAuthenticated ? (
            <Navigate to={auth.user?.role === "ADMIN" ? "/admin" : "/dashboard"} replace />
          ) : (
            <AuthPage {...sharedProps} />
          )
        }
      />
      <Route element={<ProtectedLayout {...sharedProps} />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage {...sharedProps} />} />
        <Route path="/quizzes" element={<QuizLibraryPage {...sharedProps} />} />
        <Route path="/quizzes/:quizId" element={<QuizDetailPage {...sharedProps} />} />
        <Route path="/attempts/:attemptId" element={<AttemptPage {...sharedProps} />} />
        <Route path="/history" element={<HistoryPage {...sharedProps} />} />
        <Route path="/results" element={<ResultsPage {...sharedProps} />} />
        <Route path="/results/:attemptId" element={<ResultDetailPage {...sharedProps} />} />
        <Route
          path="/admin"
          element={auth.user?.role === "ADMIN" ? <AdminPage {...sharedProps} /> : <Navigate to="/dashboard" replace />}
        />
      </Route>
      <Route path="*" element={<Navigate to={auth.isAuthenticated ? "/dashboard" : "/auth"} replace />} />
    </Routes>
  );
}

function ProtectedLayout({ auth, message, setMessage, error, setError }) {
  const navigate = useNavigate();
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  if (!auth.isAuthenticated) {
    return <Navigate to="/auth" replace />;
  }

  const navItems = [
    {
      to: auth.user?.role === "ADMIN" ? "/admin" : "/dashboard",
      icon: "dashboard",
      label: auth.user?.role === "ADMIN" ? "Overview" : "Dashboard",
    },
    { to: "/quizzes", icon: "quiz", label: "Quizzes" },
    { to: "/history", icon: "history", label: "History" },
    { to: "/results", icon: "analytics", label: "Results" },
  ];

  if (auth.user?.role === "ADMIN") {
    navItems.push({ to: "/admin", icon: "settings", label: "Admin" });
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <div>
            <h1>Quizze</h1>
            <p>Assessment workspace</p>
          </div>
        </div>

        <nav className="nav">
          {navItems.map((item) => (
            <NavLink key={`${item.to}-${item.label}`} className={({ isActive }) => `nav-link${isActive ? " active" : ""}`} to={item.to}>
              <span className="material-symbols-outlined">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-profile">
          <div>
            <div className="profile-name">{auth.user?.username}</div>
          </div>
          <Button
            className="ghost-btn sidebar-logout"
            onClick={() => setShowLogoutConfirm(true)}
            type="button"
          >
            Logout
          </Button>
        </div>
      </aside>

      <div className="main">
        <main className="canvas">
          {error ? <div className="error-banner">{error}</div> : null}
          {message ? <div className="notice">{message}</div> : null}
          <Outlet />
        </main>
      </div>

      {showLogoutConfirm ? (
        <div className="modal-backdrop" onClick={() => setShowLogoutConfirm(false)} role="presentation">
          <div className="modal-card" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true" aria-labelledby="logout-title">
            <h3 className="panel-title" id="logout-title">Log out?</h3>
            <p className="muted modal-copy">You’ll need to sign in again to continue your quiz session and dashboard access.</p>
            <div className="action-group modal-actions">
              <Button className="ghost-btn" onClick={() => setShowLogoutConfirm(false)} type="button">
                Cancel
              </Button>
              <Button
                className="primary-btn"
                onClick={() => {
                  auth.logout();
                  setError("");
                  setMessage("");
                  setShowLogoutConfirm(false);
                  navigate("/auth", { replace: true });
                }}
                type="button"
              >
                Logout
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function AuthPage({ auth, setMessage, setError }) {
  const navigate = useNavigate();
  const [mode, setMode] = useState("login");
  const [loading, setLoading] = useState(false);
  const isRegister = mode === "register";

  useEffect(() => {
    setError("");
    setMessage("");
  }, [mode, setError, setMessage]);

  async function handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    setLoading(true);
    setError("");
    setMessage("");

    try {
      if (isRegister) {
        if (formData.get("email") !== formData.get("confirmEmail")) {
          throw new Error("Email confirmation does not match");
        }

        await apiRequest("/api/auth/register", {
          method: "POST",
          body: JSON.stringify({
            firstName: formData.get("firstName"),
            lastName: formData.get("lastName"),
            email: formData.get("email"),
            username: formData.get("username"),
            password: formData.get("password"),
          }),
        });

        setMode("login");
        setMessage("Account created. Sign in to continue.");
      } else {
        const response = await apiRequest("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({
            usernameOrEmail: formData.get("usernameOrEmail"),
            password: formData.get("password"),
          }),
        });

        auth.login(response);
        navigate(response.role === "ADMIN" ? "/admin" : "/dashboard", { replace: true });
      }
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-shell">
      <main className="auth-wrap">
        <section className="auth-intro">
          <div className="logo-stack">
            <div className="logo-badge">
              <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1", color: "var(--success)", fontSize: 28 }}>
                check_circle
              </span>
            </div>
            <div>
              <h1 className="logo-title">Quizze</h1>
              <p className="logo-subtitle">Minimal quiz operations for teams and learners.</p>
            </div>
          </div>

          <div className="auth-copy">
            <div className="eyebrow">Focused workflow</div>
            <h2>Sign in, take quizzes, review outcomes.</h2>
            <p>A minimal interface for authentication, quiz attempts, reporting, and administration.</p>
          </div>
        </section>

        <Card className="auth-card">
          <h2 className="section-title">{isRegister ? "Create your account" : "Welcome back"}</h2>
          <p className="section-copy">
            {isRegister
              ? "Create a new account to start taking quizzes."
              : "Enter your credentials to access your workspace."}
          </p>

          <form className="form-grid" onSubmit={handleSubmit}>
            {isRegister ? (
              <div className="split-row">
                <Field label="First Name" name="firstName" placeholder="Niranjan" />
                <Field label="Last Name" name="lastName" placeholder="Kumar" />
              </div>
            ) : null}

            <Field
              icon={isRegister ? "mail" : "person"}
              label={isRegister ? "Email Address" : "Username or Email"}
              name={isRegister ? "email" : "usernameOrEmail"}
              placeholder={isRegister ? "name@example.com" : "niranjan"}
            />

            {isRegister ? <Field icon="alternate_email" label="Username" name="username" placeholder="niranjan" /> : null}
            {isRegister ? <Field icon="verified" label="Email Confirmation" name="confirmEmail" placeholder="Repeat your email" /> : null}
            <Field icon="lock" label="Password" name="password" placeholder="Password123" type="password" />

            <Button className="primary-btn" disabled={loading} type="submit">
              {loading ? "Please wait..." : isRegister ? "Create Account" : "Sign In"}
            </Button>
          </form>

          <div className="auth-toggle">
            {isRegister ? "Already have an account?" : "Don't have an account?"}{" "}
            <button onClick={() => setMode(isRegister ? "login" : "register")} type="button">
              {isRegister ? "Sign In" : "Create an account"}
            </button>
          </div>
        </Card>
      </main>
    </div>
  );
}

function QuizCard({ quiz }) {
  return (
    <article className="quiz-card">
      <div className="quiz-card-header">
        <span className="tag">{quiz.categoryName || quiz.difficulty || "Quiz"}</span>
        <span className="tiny muted">{quiz.timeLimitInMinutes} min</span>
      </div>
      <div className="quiz-card-body">
        <div>
          <h4>{quiz.title}</h4>
          <p className="muted">{quiz.description || "A focused assessment with automatic scoring and a clean attempt flow."}</p>
        </div>
        <div className="chip-row">
          <span className="badge">{quiz.difficulty}</span>
          {quiz.oneAttemptOnly ? <span className="badge">One attempt</span> : null}
          {quiz.negativeMarkingEnabled ? <span className="badge badge-warn">Negative marking</span> : null}
        </div>
        <div className="helper-row">
          <div className="tiny muted">{quiz.questionCount} questions</div>
          <Link className="btn primary-btn" to={`/quizzes/${quiz.id}`}>
            Open
          </Link>
        </div>
      </div>
    </article>
  );
}

function DashboardPage({ auth, setError, setMessage }) {
  const [quizzes, setQuizzes] = useState([]);
  const [quizCount, setQuizCount] = useState(0);
  const [attemptHistory, setAttemptHistory] = useState([]);
  const [resultHistory, setResultHistory] = useState([]);
  const [analytics, setAnalytics] = useState(null);

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      apiRequest("/api/quizzes?page=0&size=4&sortBy=createdAt&sortDir=desc", {}, auth.token),
      apiRequest("/api/users/me/attempts", {}, auth.token),
      apiRequest("/api/users/me/results", {}, auth.token),
      apiRequest("/api/users/me/analytics", {}, auth.token),
    ])
      .then(([quizData, attemptData, resultData, analyticsData]) => {
        if (cancelled) return;
        setQuizzes(quizData.content || []);
        setQuizCount(quizData.totalElements || 0);
        setAttemptHistory(attemptData);
        setResultHistory(resultData);
        setAnalytics(analyticsData);
        setError("");
        setMessage("");
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(loadError.message);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [auth.token, setError, setMessage]);

  const avgPct = resultHistory.length
    ? Math.round(resultHistory.reduce((sum, item) => sum + (item.percentage || 0), 0) / resultHistory.length)
    : 0;

  return (
    <>
      <section className="hero">
        <div>
          <h2>Welcome back, {auth.user?.username}.</h2>
          <p>Track quiz activity, review results, and continue where you left off in a calm focused workspace.</p>
        </div>
      </section>

      <section className="stats-grid">
        <article className="stat-card">
          <div className="stat-label">Quizzes Attempted</div>
          <div className="stat-value accent-primary">{attemptHistory.length}</div>
        </article>
        <article className="stat-card">
          <div className="stat-label">Average Score</div>
          <div className="stat-value accent-success">{avgPct}%</div>
        </article>
        <article className="stat-card">
          <div className="stat-label">Available Quizzes</div>
          <div className="stat-value">{quizCount}</div>
        </article>
      </section>

      <section className="dashboard-grid">
        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Available Quizzes</h3>
            <Link className="text-btn" to="/quizzes">
              View all
            </Link>
          </div>
          <div className="cards-grid">
            {quizzes.slice(0, 2).map((quiz) => (
              <QuizCard key={quiz.id} quiz={quiz} />
            ))}
            {!quizzes.length ? <div className="empty-state">No published quizzes yet. Ask an admin to publish one.</div> : null}
          </div>
        </Card>

        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Progress</h3>
            <span className="tag alt">Level {Math.max(1, attemptHistory.length + 3)}</span>
          </div>
          <div className="progress-shell">
            <div className="helper-row tiny muted">
              <span>Average performance</span>
              <span>{Math.min(100, avgPct || 12)}%</span>
            </div>
            <div className="progress-track">
              <div className="progress-fill" style={{ width: `${Math.min(100, avgPct || 12)}%` }} />
            </div>
            <div className="muted">A quick snapshot of how your submitted quiz scores are trending.</div>
          </div>

          <div className="section-separator" />

          <div className="panel-title-row compact">
            <h4 className="panel-title">Recent Results</h4>
          </div>
          <div className="list">
            {resultHistory.slice(0, 4).map((result) => (
              <div className="list-item" key={result.attemptId}>
                <div className="list-title">{result.quizTitle}</div>
                <div className="muted tiny">{Math.round(result.percentage || 0)}% score | {result.correctAnswers} correct</div>
              </div>
            ))}
            {!resultHistory.length ? <div className="empty-state">No submitted quiz results yet.</div> : null}
          </div>
        </Card>
      </section>

      <section className="dashboard-grid">
        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Performance Insights</h3>
            <span className="muted tiny">{analytics?.totalSubmittedAttempts ?? 0} submitted attempts</span>
          </div>
          <section className="analytics-summary-grid">
            <div className="list-item">
              <div className="list-title">Overall</div>
              <div className="analytics-list">
                <div className="helper-row"><span className="muted">Distinct Quizzes</span><strong>{analytics?.totalDistinctQuizzes ?? 0}</strong></div>
                <div className="helper-row"><span className="muted">Average Score</span><strong>{Number(analytics?.averageScore ?? 0).toFixed(1)}</strong></div>
                <div className="helper-row"><span className="muted">Average Percentage</span><strong>{Math.round(analytics?.averagePercentage ?? 0)}%</strong></div>
                <div className="helper-row"><span className="muted">Best Result</span><strong>{Math.round(analytics?.bestPercentage ?? 0)}%</strong></div>
              </div>
            </div>

            <div className="list-item">
              <div className="list-title">Strongest Category</div>
              {analytics?.strongestCategory ? (
                <div className="analytics-list">
                  <div className="helper-row"><span className="muted">Category</span><strong>{analytics.strongestCategory.categoryName}</strong></div>
                  <div className="helper-row"><span className="muted">Attempts</span><strong>{analytics.strongestCategory.attempts}</strong></div>
                  <div className="helper-row"><span className="muted">Average</span><strong>{Math.round(analytics.strongestCategory.averagePercentage || 0)}%</strong></div>
                </div>
              ) : (
                <div className="empty-state">No category data yet.</div>
              )}
            </div>

            <div className="list-item">
              <div className="list-title">Weakest Category</div>
              {analytics?.weakestCategory ? (
                <div className="analytics-list">
                  <div className="helper-row"><span className="muted">Category</span><strong>{analytics.weakestCategory.categoryName}</strong></div>
                  <div className="helper-row"><span className="muted">Attempts</span><strong>{analytics.weakestCategory.attempts}</strong></div>
                  <div className="helper-row"><span className="muted">Average</span><strong>{Math.round(analytics.weakestCategory.averagePercentage || 0)}%</strong></div>
                </div>
              ) : (
                <div className="empty-state">No category data yet.</div>
              )}
            </div>
          </section>
        </Card>

        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Recent Trend</h3>
            <span className="muted tiny">Latest submitted results</span>
          </div>
          <div className="list">
            {analytics?.recentTrend?.map((item) => (
              <div className="list-item" key={item.attemptId}>
                <div className="helper-row">
                  <div>
                    <div className="list-title">{item.quizTitle}</div>
                    <div className="muted tiny">{item.categoryName} | {new Date(item.submittedAt).toLocaleString()}</div>
                  </div>
                  <div className="overview-emphasis">{Math.round(item.percentage || 0)}%</div>
                </div>
              </div>
            ))}
            {!analytics?.recentTrend?.length ? <div className="empty-state">No submitted trend data yet.</div> : null}
          </div>
        </Card>
      </section>
    </>
  );
}

function QuizLibraryPage({ auth, setError }) {
  const [quizzes, setQuizzes] = useState([]);
  const [availableCategories, setAvailableCategories] = useState([]);
  const [filters, setFilters] = useState({
    search: "",
    category: "",
    difficulty: "",
    sortBy: "createdAt",
    sortDir: "desc",
    page: 0,
    size: 6,
  });
  const [pageMeta, setPageMeta] = useState({
    pageNumber: 0,
    totalPages: 0,
    totalElements: 0,
    hasNext: false,
    hasPrevious: false,
  });

  useEffect(() => {
    const query = buildQueryString(filters);

    apiRequest(`/api/quizzes${query}`, {}, auth.token)
      .then((data) => {
        setQuizzes(data.content || []);
        setAvailableCategories(data.availableCategories || []);
        setPageMeta({
          pageNumber: data.pageNumber,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
          hasNext: data.hasNext,
          hasPrevious: data.hasPrevious,
        });
      })
      .catch((loadError) => setError(loadError.message));
  }, [auth.token, filters, setError]);

  function updateFilter(name, value) {
    setFilters((current) => ({
      ...current,
      [name]: value,
      page: name === "page" ? value : 0,
    }));
  }

  const categoryOptions = [{ value: "", label: "All categories" }, ...availableCategories.map((category) => ({ value: category, label: category }))];
  const difficultyOptions = [
    { value: "", label: "All levels" },
    { value: "EASY", label: "EASY" },
    { value: "MEDIUM", label: "MEDIUM" },
    { value: "HARD", label: "HARD" },
  ];
  const sortOptions = [
    { value: "createdAt:desc", label: "Newest first" },
    { value: "title:asc", label: "Title A-Z" },
    { value: "difficulty:asc", label: "Difficulty" },
    { value: "timeLimitInMinutes:asc", label: "Shortest time" },
  ];

  return (
    <>
      <section className="hero">
        <div>
          <h2>Quiz Library</h2>
          <p>Browse published quizzes with category filters, search, sorting, and paginated results.</p>
        </div>
      </section>

      <Card>
        <div className="filters-grid">
          <Field
            label="Search"
            name="search"
            placeholder="Search by title or category"
            required={false}
            value={filters.search}
            onChange={(event) => updateFilter("search", event.target.value)}
          />
          <CustomSelect label="Category" name="category" onChangeValue={(nextValue) => updateFilter("category", nextValue)} options={categoryOptions} value={filters.category} />
          <CustomSelect label="Difficulty" name="difficulty" onChangeValue={(nextValue) => updateFilter("difficulty", nextValue)} options={difficultyOptions} value={filters.difficulty} />
          <CustomSelect
            label="Sort"
            name="sort"
            onChangeValue={(nextValue) => {
              const [sortBy, sortDir] = nextValue.split(":");
              setFilters((current) => ({ ...current, sortBy, sortDir, page: 0 }));
            }}
            options={sortOptions}
            value={`${filters.sortBy}:${filters.sortDir}`}
          />
        </div>
      </Card>

      <section className="cards-grid">
        {quizzes.map((quiz) => (
          <QuizCard key={quiz.id} quiz={quiz} />
        ))}
        {!quizzes.length ? <Card className="empty-state empty-state-card">No published quizzes available.</Card> : null}
      </section>

      <div className="pagination-row">
        <div className="muted tiny">
          Showing page {pageMeta.totalPages ? pageMeta.pageNumber + 1 : 0} of {pageMeta.totalPages} | {pageMeta.totalElements} quizzes
        </div>
        <div className="action-group">
          <Button className="ghost-btn" disabled={!pageMeta.hasPrevious} onClick={() => updateFilter("page", Math.max(0, filters.page - 1))} type="button">
            Previous
          </Button>
          <Button className="primary-btn" disabled={!pageMeta.hasNext} onClick={() => updateFilter("page", filters.page + 1)} type="button">
            Next
          </Button>
        </div>
      </div>
    </>
  );
}

function QuizDetailPage({ auth, setError, setMessage }) {
  const { quizId } = useParams();
  const navigate = useNavigate();
  const [quiz, setQuiz] = useState(null);
  const [leaderboard, setLeaderboard] = useState(null);
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    Promise.all([
      apiRequest(`/api/quizzes/${quizId}`, {}, auth.token),
      apiRequest(`/api/quizzes/${quizId}/leaderboard?limit=5`, {}, auth.token),
    ])
      .then(([quizData, leaderboardData]) => {
        setQuiz(quizData);
        setLeaderboard(leaderboardData);
        setError("");
      })
      .catch((loadError) => setError(loadError.message));
  }, [auth.token, quizId, setError]);

  async function startQuiz() {
    setStarting(true);
    try {
      const attempt = await apiRequest(`/api/quizzes/${quizId}/start`, { method: "POST" }, auth.token);
      setMessage("Quiz attempt started successfully.");
      navigate(`/attempts/${attempt.attemptId}`);
    } catch (startError) {
      setError(startError.message);
    } finally {
      setStarting(false);
    }
  }

  if (!quiz) {
    return <Card>Loading quiz details...</Card>;
  }

  return (
    <section className="detail-layout">
      <Card className="detail-card">
        <div className="hero">
          <div>
            <span className="tag">{quiz.categoryName || quiz.difficulty}</span>
            <h2 style={{ marginTop: 16 }}>{quiz.title}</h2>
            <p>{quiz.description || "A structured quiz with time limits, multiple questions, and automatic scoring."}</p>
            <div className="chip-row" style={{ marginTop: 16 }}>
              <span className="badge">{quiz.difficulty}</span>
              {quiz.oneAttemptOnly ? <span className="badge">One attempt only</span> : null}
              {quiz.negativeMarkingEnabled ? <span className="badge badge-warn">Negative marking enabled</span> : null}
              <span className="badge">Randomized questions</span>
            </div>
          </div>
          <Card className="detail-summary">
            <div className="list">
              <div>
                <div className="stat-label">Difficulty</div>
                <div className="detail-value">{quiz.difficulty}</div>
              </div>
              <div>
                <div className="stat-label">Duration</div>
                <div className="detail-value">{quiz.timeLimitInMinutes} Minutes</div>
              </div>
              <div>
                <div className="stat-label">Questions</div>
                <div className="detail-value">{quiz.questionCount}</div>
              </div>
              <Button className="primary-btn" disabled={starting} onClick={startQuiz} type="button">
                {starting ? "Preparing..." : "Start Quiz"}
              </Button>
            </div>
          </Card>
        </div>
      </Card>

      <Card>
        <div className="panel-title-row">
          <h3 className="panel-title">Leaderboard</h3>
          <span className="muted tiny">{leaderboard?.totalSubmittedAttempts || 0} submitted attempts</span>
        </div>
        <div className="list">
          {leaderboard?.entries?.map((entry) => (
            <div
              className={`leaderboard-item${entry.userId === auth.user?.id ? " current-user" : ""}`}
              key={`${entry.rank}-${entry.userId}-${entry.submittedAt}`}
            >
              <div className="leaderboard-rank">#{entry.rank}</div>
              <div className="leaderboard-meta">
                <div className="list-title">
                  {entry.username}
                  {entry.userId === auth.user?.id ? <span className="leaderboard-you">You</span> : null}
                </div>
                <div className="muted tiny">
                  {Math.round(entry.percentage || 0)}% | {entry.correctAnswers} correct | {new Date(entry.submittedAt).toLocaleString()}
                </div>
              </div>
              <div className="leaderboard-score">
                {entry.score} / {entry.maxScore}
              </div>
            </div>
          ))}
          {!leaderboard?.entries?.length ? <div className="empty-state">No submitted attempts yet. The leaderboard will appear after the first completed quiz.</div> : null}
        </div>
      </Card>
    </section>
  );
}

function AttemptPage({ auth, setError, setMessage }) {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const [attemptData, setAttemptData] = useState(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [timeLeft, setTimeLeft] = useState(null);

  useEffect(() => {
    apiRequest(`/api/quizzes/attempts/${attemptId}/questions`, {}, auth.token)
      .then((data) => {
        setAttemptData(data);
        setTimeLeft(formatRemainingTime(data.expiresAt));
        setError("");
      })
      .catch((loadError) => setError(loadError.message));
  }, [attemptId, auth.token, setError]);

  useEffect(() => {
    if (!attemptData?.expiresAt || submitting) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      const nextValue = formatRemainingTime(attemptData.expiresAt);
      setTimeLeft(nextValue);

      if (nextValue === "00:00") {
        window.clearInterval(intervalId);
        submitQuiz(true);
      }
    }, 1000);

    return () => window.clearInterval(intervalId);
  }, [answers, attemptData?.expiresAt, submitting]);

  if (!attemptData?.questions?.length) {
    return <Card>Loading attempt questions...</Card>;
  }

  const questions = attemptData.questions;
  const question = questions[currentIndex];
  const answeredCount = questions.filter((item) => Boolean(answers[item.id])).length;
  const progress = questions.length ? (answeredCount / questions.length) * 100 : 0;

  async function submitQuiz(autoSubmitted = false) {
    setSubmitting(true);
    try {
      const response = await apiRequest(
        `/api/quizzes/attempts/${attemptId}/submit`,
        {
          method: "POST",
          body: JSON.stringify({
            answers: questions
              .filter((item) => answers[item.id])
              .map((item) => ({
                questionId: item.id,
                selectedOptionId: answers[item.id],
              })),
          }),
        },
        auth.token,
      );

      setMessage(autoSubmitted || response.timeExpired ? "Time ended. Quiz submitted automatically." : "Quiz submitted successfully.");
      navigate(`/results/${attemptId}`);
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="quiz-screen">
      <section className="hero">
        <div>
          <span className="tag">
            Question {currentIndex + 1} of {questions.length}
          </span>
          <h3 className="attempt-quiz-title">{attemptData.quizTitle}</h3>
          <h2 style={{ marginTop: 16 }}>{question.content}</h2>
        </div>
        <div className="progress-panel">
          <div className="helper-row tiny">
            <span className="muted">Time left</span>
            <span className={timeLeft === "00:00" ? "accent-secondary" : "accent-success"}>{timeLeft || "No limit"}</span>
          </div>
          <div className="helper-row tiny muted">
            <span>Answered</span>
            <span>{answeredCount} / {questions.length}</span>
          </div>
          <div className="progress-track">
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
        </div>
      </section>

      <section className="question-grid">
        {question.options.map((option, index) => {
          const selected = answers[question.id] === option.id;
          return (
            <button
              className={`option-btn${selected ? " selected" : ""}`}
              key={option.id}
              onClick={() => setAnswers((current) => ({ ...current, [question.id]: option.id }))}
              type="button"
            >
              <span className="option-badge">{String.fromCharCode(65 + index)}</span>
              <div className="option-title">{option.content}</div>
            </button>
          );
        })}
      </section>

      <div className="helper-row action-row">
        <Button className="ghost-btn" disabled={currentIndex === 0} onClick={() => setCurrentIndex((value) => Math.max(0, value - 1))} type="button">
          Previous
        </Button>
        {currentIndex === questions.length - 1 ? (
          <Button className="primary-btn" disabled={submitting} onClick={submitQuiz} type="button">
            {submitting ? "Submitting..." : "Submit Quiz"}
          </Button>
        ) : (
          <Button
            className="primary-btn"
            onClick={() => {
              setError("");
              setCurrentIndex((value) => Math.min(questions.length - 1, value + 1));
            }}
            type="button"
          >
            Next
          </Button>
        )}
      </div>
    </section>
  );
}

function HistoryPage({ auth, setError }) {
  const [attempts, setAttempts] = useState([]);

  useEffect(() => {
    apiRequest("/api/users/me/attempts", {}, auth.token)
      .then(setAttempts)
      .catch((loadError) => setError(loadError.message));
  }, [auth.token, setError]);

  return (
    <Card>
      <div className="panel-title-row">
        <h3 className="panel-title">Attempt History</h3>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Quiz</th>
              <th>Status</th>
              <th>Score</th>
              <th>Percentage</th>
              <th>Submitted</th>
            </tr>
          </thead>
          <tbody>
            {attempts.map((item) => (
              <tr key={item.attemptId}>
                <td>{item.quizTitle}</td>
                <td>{item.status}</td>
                <td>
                  {item.score ?? 0} / {item.maxScore ?? 0}
                </td>
                <td>{Math.round(item.percentage || 0)}%</td>
                <td>{item.submittedAt ? new Date(item.submittedAt).toLocaleString() : "-"}</td>
              </tr>
            ))}
            {!attempts.length ? (
              <tr>
                <td className="muted" colSpan={5}>
                  No attempts yet.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

function ResultsPage({ auth, setError }) {
  const [results, setResults] = useState([]);

  useEffect(() => {
    apiRequest("/api/users/me/results", {}, auth.token)
      .then(setResults)
      .catch((loadError) => setError(loadError.message));
  }, [auth.token, setError]);

  return (
    <Card>
      <div className="panel-title-row">
        <h3 className="panel-title">Result History</h3>
      </div>
      <div className="list">
        {results.map((item) => (
          <div className="list-item helper-row" key={item.attemptId}>
            <div>
              <div className="list-title">{item.quizTitle}</div>
              <div className="muted tiny">
                {Math.round(item.percentage || 0)}% | {item.correctAnswers} correct | {new Date(item.submittedAt).toLocaleString()}
              </div>
            </div>
            <Link className="btn primary-btn" to={`/results/${item.attemptId}`}>
              Review
            </Link>
          </div>
        ))}
        {!results.length ? <div className="empty-state">No submitted results yet.</div> : null}
      </div>
    </Card>
  );
}

function ResultDetailPage({ auth, setError }) {
  const { attemptId } = useParams();
  const [result, setResult] = useState(null);

  useEffect(() => {
    apiRequest(`/api/quizzes/attempts/${attemptId}/result`, {}, auth.token)
      .then(setResult)
      .catch((loadError) => setError(loadError.message));
  }, [attemptId, auth.token, setError]);

  if (!result) {
    return <Card>Loading result summary...</Card>;
  }

  const scorePct = Math.round(result.percentage || 0);

  return (
    <section className="result-grid">
      <Card>
        <h2 className="result-heading">Quiz Completed</h2>
        <p className="muted">{result.quizTitle}</p>

        <div className="result-score-block">
          <div className="score-box">
            <div className="score-box-value">{scorePct}%</div>
            <div className="tiny muted">Overall score</div>
          </div>
          <div className="list result-summary-list">
            <div>
              <strong>{result.score}</strong> / {result.maxScore} points
            </div>
            <div>
              <strong>{result.correctAnswers}</strong> correct answers
            </div>
            <div>
              <strong>{result.wrongAnswers}</strong> wrong answers
            </div>
          </div>
        </div>

        <div className="stats-grid result-stats">
          <div className="stat-card">
            <div className="stat-label">Attempted</div>
            <div className="stat-value result-stat-value">{result.attemptedQuestions}</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Correct</div>
            <div className="stat-value result-stat-value accent-success">{result.correctAnswers}</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Wrong</div>
            <div className="stat-value result-stat-value accent-secondary">{result.wrongAnswers}</div>
          </div>
        </div>
      </Card>

      <Card>
        <div className="panel-title-row">
          <h3 className="panel-title">Review Answers</h3>
          <Link className="btn ghost-btn" to="/results">
            Back
          </Link>
        </div>
        <div className="list">
          {result.answers.map((answer) => (
            <div className={`review-item${answer.correct ? "" : " incorrect"}`} key={answer.questionId}>
              <div className="helper-row">
                <span className="stat-label">Question {answer.questionId}</span>
                <span className={`badge ${answer.correct ? "live" : "draft"}`}>{answer.correct ? "Correct" : "Incorrect"}</span>
              </div>
              <div className="review-question">{answer.questionContent}</div>
              <div className={answer.correct ? "notice" : "error-banner"} style={{ marginTop: 14 }}>
                Your answer: {answer.selectedOptionContent || "No answer"}
              </div>
            </div>
          ))}
        </div>
      </Card>
    </section>
  );
}

function AdminPage({ auth, setError, setMessage }) {
  const [quizzes, setQuizzes] = useState([]);
  const [overview, setOverview] = useState(null);
  const [mode, setMode] = useState("create");
  const [activeQuiz, setActiveQuiz] = useState(null);
  const [selectedDifficulty, setSelectedDifficulty] = useState("MEDIUM");
  const [selectedQuizId, setSelectedQuizId] = useState(null);
  const [quizAnalytics, setQuizAnalytics] = useState(null);
  const [questionAnalytics, setQuestionAnalytics] = useState(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  async function loadQuizzes() {
    try {
      const [quizData, overviewData] = await Promise.all([
        apiRequest("/api/admin/quizzes", {}, auth.token),
        apiRequest("/api/admin/analytics/overview", {}, auth.token),
      ]);
      setQuizzes(quizData);
      setOverview(overviewData);
      if (!selectedQuizId && quizData.length) {
        setSelectedQuizId(quizData[0].id);
      }
      setError("");
    } catch (loadError) {
      setError(loadError.message);
    }
  }

  useEffect(() => {
    loadQuizzes();
  }, [auth.token]);

  useEffect(() => {
    if (!selectedQuizId) {
      setQuizAnalytics(null);
      setQuestionAnalytics(null);
      return;
    }

    setAnalyticsLoading(true);
    Promise.all([
      apiRequest(`/api/admin/quizzes/${selectedQuizId}/analytics`, {}, auth.token),
      apiRequest(`/api/admin/quizzes/${selectedQuizId}/questions/analytics`, {}, auth.token),
    ])
      .then(([quizAnalyticsData, questionAnalyticsData]) => {
        setQuizAnalytics(quizAnalyticsData);
        setQuestionAnalytics(questionAnalyticsData);
        setError("");
      })
      .catch((loadError) => setError(loadError.message))
      .finally(() => setAnalyticsLoading(false));
  }, [auth.token, selectedQuizId, setError]);

  useEffect(() => {
    setSelectedDifficulty(activeQuiz?.difficulty || "MEDIUM");
  }, [activeQuiz, mode]);

  async function handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const body = {
      title: formData.get("title"),
      description: formData.get("description"),
      categoryName: formData.get("categoryName"),
      difficulty: selectedDifficulty,
      timeLimitInMinutes: Number(formData.get("timeLimitInMinutes")),
      published: formData.get("published") === "on",
      negativeMarkingEnabled: formData.get("negativeMarkingEnabled") === "on",
      oneAttemptOnly: formData.get("oneAttemptOnly") === "on",
    };

    setSaving(true);
    try {
      if (mode === "edit" && activeQuiz?.id) {
        await apiRequest(`/api/admin/quizzes/${activeQuiz.id}`, { method: "PUT", body: JSON.stringify(body) }, auth.token);
        setMessage("Quiz updated successfully.");
      } else {
        await apiRequest("/api/admin/quizzes", { method: "POST", body: JSON.stringify(body) }, auth.token);
        setMessage("Quiz created successfully.");
      }

      setMode("create");
      setActiveQuiz(null);
      await loadQuizzes();
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(quizId) {
    if (!window.confirm("Delete this quiz?")) {
      return;
    }

    try {
      await apiRequest(`/api/admin/quizzes/${quizId}`, { method: "DELETE" }, auth.token);
      setMessage("Quiz deleted successfully.");
      await loadQuizzes();
    } catch (deleteError) {
      setError(deleteError.message);
    }
  }

  return (
    <>
      <section className="hero">
        <div>
          <h2>Admin Dashboard</h2>
          <p>Manage quizzes, review publication state, and keep the content catalog organized.</p>
        </div>
      </section>

      <section className="metric-grid">
        <article className="stat-card">
          <div className="stat-label">Total Users</div>
          <div className="stat-value accent-primary">{overview?.totalUsers ?? 0}</div>
        </article>
        <article className="stat-card">
          <div className="stat-label">Total Quizzes</div>
          <div className="stat-value">{overview?.totalQuizzes ?? quizzes.length}</div>
        </article>
        <article className="stat-card">
          <div className="stat-label">Published</div>
          <div className="stat-value accent-success">{overview?.publishedQuizzes ?? quizzes.filter((quiz) => quiz.published).length}</div>
        </article>
        <article className="stat-card">
          <div className="stat-label">Total Attempts</div>
          <div className="stat-value accent-secondary">{overview?.totalAttempts ?? 0}</div>
        </article>
      </section>

      <section className="dashboard-grid">
        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Most Attempted Quizzes</h3>
            <span className="muted tiny">{overview?.submittedAttempts ?? 0} submitted attempts total</span>
          </div>
          <div className="list">
            {overview?.mostAttemptedQuizzes?.map((item) => (
              <div className="list-item" key={`most-${item.quizId}`}>
                <div className="helper-row">
                  <div>
                    <div className="list-title">{item.quizTitle}</div>
                    <div className="muted tiny">{item.categoryName || "Uncategorized"}</div>
                  </div>
                  <div className="overview-emphasis">{item.attempts}</div>
                </div>
              </div>
            ))}
            {!overview?.mostAttemptedQuizzes?.length ? <div className="empty-state">No attempt data yet.</div> : null}
          </div>
        </Card>

        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Top Performing Quizzes</h3>
            <span className="muted tiny">By average submitted score</span>
          </div>
          <div className="list">
            {overview?.topPerformingQuizzes?.map((item) => (
              <div className="list-item" key={`top-${item.quizId}`}>
                <div className="helper-row">
                  <div>
                    <div className="list-title">{item.quizTitle}</div>
                    <div className="muted tiny">
                      {Math.round(item.averagePercentage || 0)}% avg | {item.attempts} attempts
                    </div>
                  </div>
                  <div className="overview-emphasis">{Number(item.averageScore || 0).toFixed(1)}</div>
                </div>
              </div>
            ))}
            {!overview?.topPerformingQuizzes?.length ? <div className="empty-state">No submitted results yet.</div> : null}
          </div>
        </Card>
      </section>

      <section className="dashboard-grid admin-layout">
        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">{mode === "edit" ? "Edit Quiz" : "Create New Quiz"}</h3>
          </div>
          <form className="form-grid" onSubmit={handleSubmit}>
            <Field defaultValue={activeQuiz?.title || ""} label="Quiz Title" name="title" placeholder="Java Basics" />
            <Field
              as="textarea"
              defaultValue={activeQuiz?.description || ""}
              label="Description"
              name="description"
              placeholder="Fundamentals of Java programming"
              required={false}
            />

            <div className="split-row">
              <Field defaultValue={activeQuiz?.categoryName || ""} label="Category" name="categoryName" placeholder="Programming" required={false} />
              <CustomSelect
                label="Difficulty"
                name="difficulty"
                onChangeValue={setSelectedDifficulty}
                options={[
                  { value: "EASY", label: "EASY" },
                  { value: "MEDIUM", label: "MEDIUM" },
                  { value: "HARD", label: "HARD" },
                ]}
                value={selectedDifficulty}
              />
            </div>

            <div className="split-row admin-form-row">
              <Field
                defaultValue={activeQuiz?.timeLimitInMinutes ?? 15}
                label="Time Limit"
                max="300"
                min="0"
                name="timeLimitInMinutes"
                type="number"
              />
              <div className="check-grid">
                <label className="checkbox-label">
                  <input defaultChecked={Boolean(activeQuiz?.published)} name="published" type="checkbox" />
                  <span>Published</span>
                </label>
                <label className="checkbox-label">
                  <input defaultChecked={Boolean(activeQuiz?.negativeMarkingEnabled)} name="negativeMarkingEnabled" type="checkbox" />
                  <span>Negative Marking</span>
                </label>
                <label className="checkbox-label">
                  <input defaultChecked={Boolean(activeQuiz?.oneAttemptOnly)} name="oneAttemptOnly" type="checkbox" />
                  <span>One Attempt Only</span>
                </label>
              </div>
            </div>

            <div className="helper-row">
              <Button className="primary-btn" disabled={saving} type="submit">
                {saving ? "Saving..." : mode === "edit" ? "Update Quiz" : "Create Quiz"}
              </Button>
              {mode === "edit" ? (
                <Button
                  className="ghost-btn"
                  onClick={() => {
                    setMode("create");
                    setActiveQuiz(null);
                  }}
                  type="button"
                >
                  Cancel
                </Button>
              ) : null}
            </div>
          </form>
        </Card>

        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Manage Quizzes</h3>
          </div>
          <div className="list">
            {quizzes.map((quiz) => (
              <div className="list-item helper-row" key={quiz.id}>
                <div>
                  <div className="list-title">{quiz.title}</div>
                  <div className="muted tiny">
                    {quiz.categoryName || "Uncategorized"} | {quiz.questions?.length || 0} questions | {quiz.published ? "Published" : "Draft"}
                  </div>
                  <div className="chip-row" style={{ marginTop: 8 }}>
                    {quiz.oneAttemptOnly ? <span className="badge">One attempt</span> : null}
                    {quiz.negativeMarkingEnabled ? <span className="badge badge-warn">Negative marking</span> : null}
                  </div>
                </div>
                <div className="action-group">
                  <Button className="ghost-btn" onClick={() => setSelectedQuizId(quiz.id)} type="button">
                    Analytics
                  </Button>
                  <Button
                    className="secondary-btn"
                    onClick={() => {
                      setMode("edit");
                      setActiveQuiz(quiz);
                      setSelectedQuizId(quiz.id);
                    }}
                    type="button"
                  >
                    Edit
                  </Button>
                  <Button className="ghost-btn" onClick={() => handleDelete(quiz.id)} type="button">
                    Delete
                  </Button>
                </div>
              </div>
            ))}
            {!quizzes.length ? <div className="empty-state">No quizzes created yet.</div> : null}
          </div>
        </Card>
      </section>

      <Card>
        <div className="panel-title-row">
          <h3 className="panel-title">Quiz Analytics</h3>
          <span className="muted tiny">{quizAnalytics?.quizTitle || "Select a quiz"}</span>
        </div>

        {analyticsLoading ? (
          <div className="empty-state">Loading analytics...</div>
        ) : quizAnalytics ? (
          <>
            <section className="metric-grid analytics-grid">
              <article className="stat-card">
                <div className="stat-label">Total Attempts</div>
                <div className="stat-value">{quizAnalytics.totalAttempts}</div>
              </article>
              <article className="stat-card">
                <div className="stat-label">Submitted</div>
                <div className="stat-value accent-primary">{quizAnalytics.submittedAttempts}</div>
              </article>
              <article className="stat-card">
                <div className="stat-label">Completion Rate</div>
                <div className="stat-value accent-success">{Math.round(quizAnalytics.completionRate || 0)}%</div>
              </article>
              <article className="stat-card">
                <div className="stat-label">Average Score</div>
                <div className="stat-value accent-secondary">
                  {Number(quizAnalytics.averageScore || 0).toFixed(1)} / {Number(quizAnalytics.maxScore || 0).toFixed(1)}
                </div>
              </article>
            </section>

            <section className="analytics-summary-grid">
              <div className="list-item">
                <div className="list-title">Scoring Summary</div>
                <div className="analytics-list">
                  <div className="helper-row"><span className="muted">Average Percentage</span><strong>{Math.round(quizAnalytics.averagePercentage || 0)}%</strong></div>
                  <div className="helper-row"><span className="muted">Highest Score</span><strong>{Number(quizAnalytics.highestScore || 0).toFixed(1)}</strong></div>
                  <div className="helper-row"><span className="muted">Lowest Score</span><strong>{Number(quizAnalytics.lowestScore || 0).toFixed(1)}</strong></div>
                </div>
              </div>

              <div className="list-item">
                <div className="list-title">Attempt Breakdown</div>
                <div className="analytics-list">
                  <div className="helper-row"><span className="muted">In Progress</span><strong>{quizAnalytics.inProgressAttempts}</strong></div>
                  <div className="helper-row"><span className="muted">Expired</span><strong>{quizAnalytics.expiredAttempts}</strong></div>
                  <div className="helper-row"><span className="muted">Last Submission</span><strong>{quizAnalytics.lastSubmittedAt ? new Date(quizAnalytics.lastSubmittedAt).toLocaleString() : "-"}</strong></div>
                </div>
              </div>

              <div className="list-item">
                <div className="list-title">Answer Quality</div>
                <div className="analytics-list">
                  <div className="helper-row"><span className="muted">Avg Correct</span><strong>{Number(quizAnalytics.averageCorrectAnswers || 0).toFixed(1)}</strong></div>
                  <div className="helper-row"><span className="muted">Avg Wrong</span><strong>{Number(quizAnalytics.averageWrongAnswers || 0).toFixed(1)}</strong></div>
                </div>
              </div>
            </section>
          </>
        ) : (
          <div className="empty-state">Select a quiz to view analytics.</div>
        )}
      </Card>

      <section className="dashboard-grid">
        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Hardest Questions</h3>
            <span className="muted tiny">{questionAnalytics?.quizTitle || "Selected quiz"}</span>
          </div>
          <div className="list">
            {questionAnalytics?.hardestQuestions?.map((item) => (
              <div className="list-item" key={`hard-${item.questionId}`}>
                <div className="list-title">{item.questionContent}</div>
                <div className="muted tiny analytics-metadata">
                  {Math.round(item.correctPercentage || 0)}% correct | {item.totalAnswers} answers | {item.points} pts
                </div>
              </div>
            ))}
            {!analyticsLoading && !questionAnalytics?.hardestQuestions?.length ? (
              <div className="empty-state">No question analytics yet for this quiz.</div>
            ) : null}
          </div>
        </Card>

        <Card>
          <div className="panel-title-row">
            <h3 className="panel-title">Easiest Questions</h3>
            <span className="muted tiny">{questionAnalytics?.quizTitle || "Selected quiz"}</span>
          </div>
          <div className="list">
            {questionAnalytics?.easiestQuestions?.map((item) => (
              <div className="list-item" key={`easy-${item.questionId}`}>
                <div className="list-title">{item.questionContent}</div>
                <div className="muted tiny analytics-metadata">
                  {Math.round(item.correctPercentage || 0)}% correct | {item.totalAnswers} answers | {item.points} pts
                </div>
              </div>
            ))}
            {!analyticsLoading && !questionAnalytics?.easiestQuestions?.length ? (
              <div className="empty-state">No question analytics yet for this quiz.</div>
            ) : null}
          </div>
        </Card>
      </section>
    </>
  );
}
