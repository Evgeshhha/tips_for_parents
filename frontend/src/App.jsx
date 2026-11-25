
import { useEffect, useState } from "react";

import PersonalFeedingCalendar from "./components/PersonalFeedingCalendar.jsx";
import Header from "./components/Header.jsx";
import MonthSelector from "./components/MonthSelector.jsx";
import FeedingCalendar from "./components/FeedingCalendar.jsx";
import AdviceSection from "./components/AdviceSection.jsx";
import ChildProfile from "./components/ChildProfile.jsx";
import CreateChildProfile from "./components/CreateChildProfile.jsx";

import { API_URL } from "./config.js";
import "./App.css";

const CHILD_ID_KEY = "childId";

export default function App() {
  // Обычный календарь
  const [months, setMonths] = useState([]);
  const [selectedMonth, setSelectedMonth] = useState(4);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Навигация
  const [currentPage, setCurrentPage] = useState("calendar");

  // Профиль ребёнка
  const [childProfile, setChildProfile] = useState(null);
  const [profileStatus, setProfileStatus] = useState("checking");
  const [profileError, setProfileError] = useState("");
  const [profileRetry, setProfileRetry] = useState(0);

  // Создание профиля
  const [creatingProfile, setCreatingProfile] = useState(false);
  const [createError, setCreateError] = useState("");

  // Персональный календарь
  const [personalPlan, setPersonalPlan] = useState(null);
  const [personalSelectedMonth, setPersonalSelectedMonth] = useState(4);
  const [personalLoading, setPersonalLoading] = useState(false);
  const [personalError, setPersonalError] = useState("");
  const [personalRetry, setPersonalRetry] = useState(0);

  // Статусы продуктов ребёнка
  const [productStatuses, setProductStatuses] = useState([]);

  // --------------------------------------------------
  // Загрузка обычного календаря
  // --------------------------------------------------

  useEffect(() => {
    const controller = new AbortController();

    async function loadMonths() {
      try {
        setLoading(true);
        setError("");

        const response = await fetch(`${API_URL}/api/months`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`Сервер вернул ошибку ${response.status}`);
        }

        const data = await response.json();

        if (!Array.isArray(data) || data.length === 0) {
          throw new Error("Сервер не вернул список месяцев");
        }

        const sortedMonths = [...data].sort(
          (a, b) => a.month - b.month
        );

        setMonths(sortedMonths);

        setSelectedMonth((current) =>
          sortedMonths.some((item) => item.month === current)
            ? current
            : sortedMonths[0].month
        );
      } catch (err) {
        if (err.name !== "AbortError") {
          console.error("Ошибка загрузки месяцев:", err);

          setError(
            "Ой-ой, что-то пошло не так. Пожалуйста, попробуйте обновить страницу чуть позже."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadMonths();

    return () => controller.abort();
  }, []);

  // --------------------------------------------------
  // Проверка наличия сохранённого профиля
  // --------------------------------------------------

  useEffect(() => {
    const controller = new AbortController();

    async function loadChildProfile() {
      const childId = localStorage.getItem(CHILD_ID_KEY);

      if (!childId) {
        setChildProfile(null);
        setProfileStatus("missing");
        return;
      }

      try {
        setProfileStatus("checking");
        setProfileError("");

        const response = await fetch(
          `${API_URL}/api/children/${childId}`,
          {
            signal: controller.signal,
          }
        );

        if (response.status === 404) {
          localStorage.removeItem(CHILD_ID_KEY);
          setChildProfile(null);
          setProfileStatus("missing");
          return;
        }

        if (!response.ok) {
          throw new Error(`Сервер вернул ошибку ${response.status}`);
        }

        const data = await response.json();

        setChildProfile(data);
        setProfileStatus("ready");
      } catch (err) {
        if (err.name !== "AbortError") {
          console.error("Ошибка загрузки профиля:", err);

          setProfileError(
            "Не удалось загрузить профиль. Проверьте подключение к серверу."
          );

          setProfileStatus("error");
        }
      }
    }

    loadChildProfile();

    return () => controller.abort();
  }, [profileRetry]);

  // --------------------------------------------------
  // Переход после проверки профиля
  // --------------------------------------------------

  useEffect(() => {
    if (currentPage !== "personal-entry") {
      return;
    }

    // Пока профиль проверяется, остаёмся на экране загрузки.
    if (profileStatus === "checking") {
      return;
    }

    // Профиль найден — открываем персональный план.
    if (profileStatus === "ready" && childProfile?.id) {
      setCurrentPage("personal");
      return;
    }

    // Профиля нет — открываем создание профиля.
    if (profileStatus === "missing") {
      setCurrentPage("profile");
      return;
    }

    // Если произошла ошибка, открываем экран с возможностью повтора.
    if (profileStatus === "error") {
      setCurrentPage("profile");
    }
  }, [currentPage, profileStatus, childProfile]);

  // --------------------------------------------------
  // Создание профиля через API
  // --------------------------------------------------

  async function handleCreateProfile(formData) {
    try {
      setCreatingProfile(true);
      setCreateError("");

      const response = await fetch(`${API_URL}/api/children`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        throw new Error(
          data?.message ||
          data?.detail ||
          `Не удалось создать профиль. Код ошибки: ${response.status}`
        );
      }

      if (!data?.id) {
        throw new Error("Сервер не вернул идентификатор ребёнка");
      }

      localStorage.setItem(CHILD_ID_KEY, data.id);

      setChildProfile(data);
      setProfileStatus("ready");
      setProfileError("");
      setCreateError("");

      // После создания открываем профиль.
      setCurrentPage("profile");
    } catch (err) {
      console.error("Ошибка создания профиля:", err);

      setCreateError(
        err.message || "Не удалось создать профиль. Попробуйте ещё раз."
      );
    } finally {
      setCreatingProfile(false);
    }
  }

  // --------------------------------------------------
  // Загрузка персонального плана и статусов продуктов
  // --------------------------------------------------

  useEffect(() => {
    if (currentPage !== "personal") {
      return;
    }

    const childId = childProfile?.id;

    if (!childId) {
      setPersonalError("Сначала необходимо создать профиль ребёнка.");
      return;
    }

    const controller = new AbortController();

    async function loadPersonalPlan() {
      try {
        setPersonalLoading(true);
        setPersonalError("");

        const [planResponse, productsResponse] = await Promise.all([
          fetch(`${API_URL}/api/children/${childId}/plan`, {
            signal: controller.signal,
          }),

          fetch(`${API_URL}/api/children/${childId}/products`, {
            signal: controller.signal,
          }),
        ]);

        const planData = await planResponse.json().catch(() => null);
        const productsData = await productsResponse.json().catch(() => null);

        if (!planResponse.ok) {
          throw new Error(
            planData?.message ||
            planData?.detail ||
            `Не удалось загрузить персональный план. Код ошибки: ${planResponse.status}`
          );
        }

        if (!productsResponse.ok) {
          throw new Error(
            productsData?.message ||
            productsData?.detail ||
            `Не удалось загрузить статусы продуктов. Код ошибки: ${productsResponse.status}`
          );
        }

        if (
          !Array.isArray(planData?.months) ||
          planData.months.length === 0
        ) {
          throw new Error(
            "Сервер не вернул месяцы персонального плана."
          );
        }

        if (!Array.isArray(productsData)) {
          throw new Error(
            "Сервер вернул некорректный список статусов продуктов."
          );
        }

        setProductStatuses(productsData);

        const sortedMonths = [...planData.months].sort(
          (a, b) => a.month - b.month
        );

        setPersonalPlan({
          ...planData,
          months: sortedMonths,
        });

        // Месяцы 4 и 5 объединяются в одну группу.
        const firstMonth = sortedMonths[0].month;

        setPersonalSelectedMonth(
          firstMonth === 5 ? 4 : firstMonth
        );
      } catch (err) {
        if (err.name !== "AbortError") {
          console.error(
            "Ошибка загрузки персонального плана:",
            err
          );

          setPersonalError(
            err.message || "Не удалось загрузить персональный план."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setPersonalLoading(false);
        }
      }
    }

    loadPersonalPlan();

    return () => controller.abort();
  }, [currentPage, childProfile?.id, personalRetry]);

  // --------------------------------------------------
  // Обычный календарь: выбор месяца
  // --------------------------------------------------

  const monthData = months.find(
    (item) => item.month === selectedMonth
  );

function changeMonth(direction) {
  // Формируем список отображаемых месяцев.
  // 4 и 5 месяц объединяются в одну группу.
  const visibleMonths = months.filter(
    (item) => item.month !== 5
  );

  const currentIndex = visibleMonths.findIndex(
    (item) => item.month === selectedMonth
  );

  const nextIndex = currentIndex + direction;

  if (nextIndex >= 0 && nextIndex < visibleMonths.length) {
    setSelectedMonth(visibleMonths[nextIndex].month);
  }
}

  // --------------------------------------------------
  // Персональный календарь: выбор месяца
  // --------------------------------------------------

  const personalMonths = personalPlan?.months ?? [];

  const personalMonthGroups = [
    ...new Set(
      personalMonths.map((item) =>
        item.month === 5 ? 4 : item.month
      )
    ),
  ].sort((a, b) => a - b);

  const personalMonthData =
    personalMonths.find(
      (item) => item.month === personalSelectedMonth
    ) ||
    (personalSelectedMonth === 4
      ? personalMonths.find((item) => item.month === 5)
      : undefined);

  function changePersonalMonth(direction) {
    const currentIndex = personalMonthGroups.indexOf(
      personalSelectedMonth
    );

    const nextMonth = personalMonthGroups[currentIndex + direction];

    if (nextMonth !== undefined) {
      setPersonalSelectedMonth(nextMonth);
    }
  }

  // --------------------------------------------------
  // Подготовка данных персонального календаря
  // --------------------------------------------------

  const personalCalendarData = personalMonthData
    ? {
        ...personalMonthData,
        exampleDay: (personalMonthData.days ?? []).map((item) => ({
          ...item.day,
          newProductCode: item.newProductCode,
          seq: item.seq,
        })),
      }
    : null;

  // --------------------------------------------------
  // Навигация
  // --------------------------------------------------

  function handleNavigate(page) {
    if (page === "personal-entry") {
      // Запускаем переход с проверкой профиля.
      setCurrentPage("personal-entry");
      return;
    }

    if (page === "profile") {
      setCurrentPage("profile");
      return;
    }

    setCurrentPage(page);
  }

  // --------------------------------------------------
  // Отображение страниц
  // --------------------------------------------------

  return (
    <div className="app-shell">
      <Header
        currentPage={currentPage}
        onNavigate={handleNavigate}
      />

      <div className="page-background">

        {/* Проверка профиля перед открытием персонального плана */}
        {currentPage === "personal-entry" && (
          <div className="loading-state" role="status">
            Проверяем наличие профиля малыша...
          </div>
        )}

        {/* Страница профиля */}
        {currentPage === "profile" && (
          profileStatus === "checking" ? (
            <div className="loading-state" role="status">
              Загружаем профиль...
            </div>
          ) : profileStatus === "error" ? (
            <div className="page-container">
              <div className="error-state" role="alert">
                <p>{profileError}</p>

                <button
                  onClick={() =>
                    setProfileRetry((value) => value + 1)
                  }
                >
                  Повторить загрузку
                </button>
              </div>
            </div>
          ) : profileStatus === "missing" ? (
            <>
              {createError && (
                <div className="profile-api-error" role="alert">
                  {createError}
                </div>
              )}

              <CreateChildProfile
                onSubmit={handleCreateProfile}
                loading={creatingProfile}
              />
            </>
          ) : (
            <ChildProfile
              onCreatePlan={() => setCurrentPage("personal")}
              child={childProfile}
              onChildUpdated={(updatedChild) => {
                setChildProfile(updatedChild);
                setPersonalRetry((value) => value + 1);
              }}
            />
          )
        )}

        {/* Персональный календарь */}
        {currentPage === "personal" && (
          <main className="page-container">

            <section className="page-hero">
              <div className="hero-copy">
                <h1>Персональный план</h1>

                <p className="hero-subtitle">
                  Индивидуальный план с учётом особенностей малыша
                </p>
              </div>
            </section>

            {personalLoading && (
              <div className="loading-state" role="status">
                Загружаем персональный план...
              </div>
            )}

            {!personalLoading && personalError && (
              <div className="error-state" role="alert">
                <p>{personalError}</p>

                <button
                  onClick={() =>
                    setPersonalRetry((value) => value + 1)
                  }
                >
                  Повторить загрузку
                </button>
              </div>
            )}

            {!personalLoading &&
              !personalError &&
              personalMonthData && (
                <div className="content-layout">

                  <aside className="sidebar">
                    <MonthSelector
                      months={personalMonths}
                      selectedMonth={personalSelectedMonth}
                      onSelect={setPersonalSelectedMonth}
                    />

                    <div className="sidebar-note">
                      <span>🌿</span>

                      <p>
                        <strong>
                          Персональный план формируется с учётом данных
                          профиля ребёнка. При возникновении вопросов
                          обратитесь к педиатру.
                        </strong>
                      </p>
                    </div>
                  </aside>

                  <div className="main-content">

                    <section className="month-heading">
                      <div>
                        <h2>
                          {personalSelectedMonth === 4
                            ? "4–5 месяц"
                            : `${personalSelectedMonth} месяц`}
                        </h2>
                      </div>
                    </section>

                    <PersonalFeedingCalendar
                      monthData={personalCalendarData}
                      productStatuses={productStatuses}
                    />

                    <nav
                      className="month-navigation"
                      aria-label="Навигация по месяцам"
                    >
                      <button
                        disabled={
                          personalMonthGroups.indexOf(
                            personalSelectedMonth
                          ) === 0
                        }
                        onClick={() => changePersonalMonth(-1)}
                      >
                        ← Предыдущий месяц
                      </button>

                      <button
                        disabled={
                          personalMonthGroups.indexOf(
                            personalSelectedMonth
                          ) === personalMonthGroups.length - 1
                        }
                        onClick={() => changePersonalMonth(1)}
                      >
                        Следующий месяц →
                      </button>
                    </nav>

                  </div>
                </div>
              )}

            {!personalLoading &&
              !personalError &&
              !personalMonthData &&
              personalPlan && (
                <div className="empty-state">
                  <span>🌱</span>
                  <h3>Нет данных для выбранного месяца</h3>
                </div>
              )}
          </main>
        )}

        {/* Обычный календарь */}
        {currentPage === "calendar" && (
          <main className="page-container">

            <section className="page-hero">
              <div className="hero-copy">
                <h1>План прикорма</h1>

                <p className="hero-subtitle">
                  План введения продуктов по этапам с 4 до 12 месяцев
                </p>
              </div>
            </section>

            <div className="source-notice">
              <span className="notice-icon">i</span>

              План составлен на основе методических рекомендаций

              <p>
                <strong>
                  «Программы оптимизации вскармливания детей первого года
                  жизни в Российской Федерации» от 2019 года.
                </strong>
              </p>
            </div>

            {loading && (
              <div className="loading-state" role="status">
                Загружаем план...
              </div>
            )}

            {!loading && error && (
              <div className="error-state" role="alert">
                <p>{error}</p>

                <button onClick={() => window.location.reload()}>
                  Повторить загрузку
                </button>
              </div>
            )}

            {!loading && !error && monthData && (
              <div className="content-layout">

                <aside className="sidebar">
                  <MonthSelector
                    months={months}
                    selectedMonth={selectedMonth}
                    onSelect={setSelectedMonth}
                  />

                  <div className="sidebar-note">
                    <span>🌿</span>

                    <p>
                      <strong>
                        Каждый ребёнок уникален. Если у Вас возникли
                        вопросы — обратитесь к педиатру.
                      </strong>
                    </p>
                  </div>
                </aside>

                <div className="main-content">

                  <section className="month-heading">
                    <div>
                      <h2>
                        {monthData.month === 4 || monthData.month === 5
                          ? "4–5 месяц"
                          : `${monthData.month} месяц`}
                      </h2>
                    </div>
                  </section>

                  <FeedingCalendar monthData={monthData} />

                  <AdviceSection monthData={monthData} />

                  <nav
                    className="month-navigation"
                    aria-label="Навигация по месяцам"
                  >
                    <button
                    disabled={
                      months.filter((item) => item.month !== 5)[0]?.month === selectedMonth
                    }
                      onClick={() => changeMonth(-1)}
                    >
                      ← Предыдущий месяц
                    </button>

                    <button
                    disabled={
                      months.filter((item) => item.month !== 5).at(-1)?.month === selectedMonth
                    }
                      onClick={() => changeMonth(1)}
                    >
                      Следующий месяц →
                    </button>
                  </nav>

                </div>
              </div>
            )}

            <footer className="page-footer">
              <strong>
                Источник — «Программа оптимизации вскармливания детей
                первого года жизни в Российской Федерации» от 2019 года
              </strong>
            </footer>

          </main>
        )}

      </div>
    </div>
  );
}
