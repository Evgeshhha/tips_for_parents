import ProductCard from './ProductCard.jsx';

export default function FeedingCalendar({ monthData }) {
  const days = monthData?.exampleDay ?? [];

  const daysLabel =
    days.length === 1
      ? 'карточка'
      : days.length < 5
        ? 'карточки'
        : 'карточек';

  return (
    <section className="feeding-calendar" id="calendar">
      <div className="calendar-topline">
        <div>
          <h2>План месяца</h2>
        </div>
      </div>

      <div className="legend">
        <span>
          <i className="legend-dot new-dot" />
          {' '}Новый продукт
        </span>
        <span>
          <i className="legend-dot addition-dot" />
          {' '}Добавка
        </span>
        <span>
          <i className="legend-dot continuation-dot" />
          {' '}Уже введённый продукт
        </span>
      </div>

      {days.length ? (
        <div className="day-card-list">
          {days.map((day, index) => (
            <ProductCard
              key={`${monthData.month}-${day.title}-${index}`}
              day={day}
              index={index}
            />
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <span>🌱</span>
          <h3>На этот месяц нет примеров дней</h3>
          <p>В данных нет поля exampleDay с продуктами для отображения.</p>
        </div>
      )}
    </section>
  );
}