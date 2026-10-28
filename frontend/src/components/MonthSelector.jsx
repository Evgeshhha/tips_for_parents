import './MonthSelector.css';
import month4 from '../assets/icons/kabachok.png';
import month5 from '../assets/icons/kasha.png';
import month6 from '../assets/icons/maco.png';
import month7 from '../assets/icons/eggs.png';
import month8 from '../assets/icons/fish.png';
import month9 from '../assets/icons/9mec.png';
import month10 from '../assets/icons/10mec.png';
import month11 from '../assets/icons/11mec.png';
import month12 from '../assets/icons/12mec.png';

const icons = {
  4: month4,
  5: month5,
  6: month6,
  7: month7,
  8: month8,
  9: month9,
  10: month10,
  11: month11,
  12: month12,
};

export default function MonthSelector({
  months,
  selectedMonth,
  onSelect,
}) {
  // Объединяем 4-й и 5-й месяцы в один пункт меню.
  const menuMonths = months
    .filter((item) => item.month !== 5)
    .map((item) =>
      item.month === 4
        ? { ...item, label: '4–5 месяц', selectMonth: 4 }
        : { ...item, label: `${item.month} месяц`, selectMonth: item.month }
    );

  return (
    <div className="month-selector">
      <h3>Возраст ребёнка</h3>

      <div className="month-list">
        {menuMonths.map((item) => {
          const isSelected =
            item.month === 4
              ? selectedMonth === 4 || selectedMonth === 5
              : selectedMonth === item.month;

          return (
            <button
              key={item.month}
              className={`month-option ${
                isSelected ? 'selected' : ''
              }`}
              onClick={() => onSelect(item.selectMonth)}
            >
              <img
                className="month-icon"
                src={icons[item.month] ?? month6}
                alt=""
              />

              <span className="month-option-copy">
                <strong>{item.label}</strong>
              </span>

              <span className="month-chevron">›</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
