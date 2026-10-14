import { useMemo, useState } from 'react';
import Header from './components/Header.jsx';
import MonthSelector from './components/MonthSelector.jsx';
import FeedingCalendar from './components/FeedingCalendar.jsx';
import AdviceSection from './components/AdviceSection.jsx';
import feedingData from './data/feedingData.json';
import './App.css';

export default function App() {
  const months = useMemo(() => [...feedingData].sort((a, b) => a.month - b.month), []);
  const [selectedMonth, setSelectedMonth] = useState(4);
  const monthData = months.find((item) => item.month === selectedMonth) ?? months[0];

  return (
    <div className="app-shell">
      <Header />
      <main className="page-container">
        <section className="page-hero">
          <div className="hero-copy">
            <h1>Календарь прикорма</h1>
            <p className="hero-subtitle">План введения продуктов по этапам с 4 до 12 месяцев</p>
          </div>
        </section>

        <div className="source-notice">
          <span className="notice-icon">i</span>
          <div>План составлен на основе методических рекомендаций<p><strong>"Программы оптимизации вскармливания детей первого года жизни в Российской Федерации" от 2019 года.</strong></p></div>
        </div>

        <div className="content-layout">
          <aside className="sidebar">
            <MonthSelector months={months} selectedMonth={selectedMonth} onSelect={setSelectedMonth} />
            <div className="sidebar-note"><span>🌿</span><p>Каждый ребёнок уникален. Если у Вас возникли вопросы - обратитесь к педиатру.</p></div>
          </aside>

          <div className="main-content">
            <section className="month-heading">
              <div><p className="eyebrow"></p><h2>{monthData.month} месяц</h2><p>{monthData.newInPeriod?.length ? `Новые продукты: ${monthData.newInPeriod.join(', ')}` : 'Продолжение знакомого рациона'}</p></div>
            </section>

            <FeedingCalendar monthData={monthData} />
            <AdviceSection monthData={monthData} />

            <nav className="month-navigation" aria-label="Навигация по месяцам">
              <button disabled={selectedMonth <= months[0].month} onClick={() => setSelectedMonth((m) => m - 1)}>← Предыдущий месяц</button>
              <button disabled={selectedMonth >= months[months.length - 1].month} onClick={() => setSelectedMonth((m) => m + 1)}>Следующий месяц →</button>
            </nav>
          </div>
        </div>
        <footer className="page-footer">Источник - «Программа оптимизации вскармливания детей первого года жизни в Российской Федерации» от 2019 года.</footer>
      </main>
    </div>
  );
}
