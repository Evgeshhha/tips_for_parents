import month4 from "../assets/icons/kabachok.png";
import month5 from "../assets/icons/kasha.png";
import month6 from "../assets/icons/maco.png";

const icons = {
  4: month4,
  5: month5,
  6: month6
};

export default function MonthSelector({ months, selectedMonth, onSelect }) {
  return (
    <div className="month-selector">
      <h3>Возраст ребёнка</h3>

      <div className="month-list">
        {months.map((item) => (
          <button
            key={item.month}
            className={`month-option ${selectedMonth === item.month ? 'selected' : ''}`}
            onClick={() => onSelect(item.month)}
          >
            <img
              className="month-icon"
              src={icons[item.month]}
              alt=""
            />

            <span className="month-option-copy">
              <strong>{item.month} месяц</strong>
            </span>

            <span className="month-chevron">›</span>
          </button>
        ))}
      </div>
    </div>
  );
}