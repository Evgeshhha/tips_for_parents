import PersonalProductCard from './PersonalProductCard.jsx';
import './PersonalFeedingCalendar.css';

export default function PersonalFeedingCalendar({
  monthData,
  productStatuses = [],
}) {
  const days = monthData?.exampleDay ?? [];

  return (
    <section className="personal-calendar" id="personal-calendar">

      <div className="personal-legend">
        <span>
          <i className="personal-dot not-started" />
          Не введено
        </span>

        <span>
          <i className="personal-dot introducing" />
          Вводится
        </span>

        <span>
          <i className="personal-dot introduced" />
          В рационе
        </span>
      </div>

      {days.length > 0 ? (
        <div className="personal-day-list">
          {days.map((day, index) => (
            <PersonalProductCard
              key={`${monthData.month}-${day.title}-${index}`}
              day={day}
              index={index}
              productStatuses={productStatuses}
            />
          ))}
        </div>
      ) : (
        <div className="personal-empty">
          <span>🌱</span>
          <h3>Пока нет этапов для этого месяца</h3>
          <p>
            Персональный план ещё не содержит примеров дней
            для выбранного возраста.
          </p>
        </div>
      )}
    </section>
  );
}