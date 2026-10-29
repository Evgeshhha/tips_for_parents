import { useEffect, useState } from 'react';

import Header from './components/Header.jsx';
import MonthSelector from './components/MonthSelector.jsx';
import FeedingCalendar from './components/FeedingCalendar.jsx';
import AdviceSection from './components/AdviceSection.jsx';

import { API_URL } from './config.js';
import './App.css';

export default function App() {
  const [months, setMonths] = useState([]);
  const [selectedMonth, setSelectedMonth] = useState(4);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    async function loadMonths() {
      try {
        setLoading(true);
        setError('');

        const response = await fetch(`${API_URL}/api/months`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(
            `Сервер вернул ошибку ${response.status}`
          );
        }

        const data = await response.json();

        if (!Array.isArray(data) || data.length === 0) {
          throw new Error('Сервер не вернул список месяцев');
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
        if (err.name !== 'AbortError') {
          console.error('Ошибка загрузки месяцев:', err);
          setError(
            'Ой-ой, что-то пошло не так. Пожалуйста, попробуйте обновить страницу чуть позже'
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

  const monthData = months.find(
    (item) => item.month === selectedMonth
  );


function changeMonth(direction) {
  const visibleMonths = months
    .filter((item) => item.month !== 5)
    .map((item) => item.month);

  const currentGroup = selectedMonth === 5 ? 4 : selectedMonth;
  const currentIndex = visibleMonths.indexOf(currentGroup);
  const nextMonth = visibleMonths[currentIndex + direction];

  if (nextMonth !== undefined) {
    setSelectedMonth(nextMonth);
  }
}

  return (
    <div className="app-shell">
        <Header />
     <div className="page-background">
      <main className="page-container">
        <section className="page-hero">
          <div className="hero-copy">
            <h1>Календарь прикорма</h1>
            <p className="hero-subtitle">
              План введения продуктов по этапам с 4 до 12 месяцев
            </p>
          </div>
        </section>

        <div className="source-notice">
            <span className="notice-icon">i</span>
          План составлен на основе методических рекомендаций<p><strong>"Программы оптимизации вскармливания детей первого года жизни в Российской Федерации" от 2019 года.</strong></p>
        </div>

        {loading && (
          <div className="loading-state" role="status">
            Загружаем календарь...
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

              <div className="sidebar-note"><span>🌿</span><p><strong> Каждый ребёнок уникален. Если у Вас возникли вопросы - обратитесь к педиатру.</strong></p></div>
            </aside>

            <div className="main-content">
              <section className="month-heading">
                <div>
                  <h2>
                    {monthData.month === 4 || monthData.month === 5
                      ? '4–5 месяц'
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
                    months.findIndex(
                      (item) => item.month === selectedMonth
                    ) === 0
                  }
                  onClick={() => changeMonth(-1)}
                >
                  ← Предыдущий месяц
                </button>

                <button
                  disabled={
                    months.findIndex(
                      (item) => item.month === selectedMonth
                    ) === months.length - 1
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
          <strong>Источник - «Программа оптимизации вскармливания детей первого года жизни в Российской Федерации» от 2019 года</strong>
        </footer>
      </main>
      </div>
    </div>
  );
}
