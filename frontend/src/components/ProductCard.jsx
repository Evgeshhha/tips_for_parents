const nutrientLabels = [
  ['energy', 'ккал', 'Энергия'],
  ['protein', 'г', 'Белки'],
  ['fat', 'г', 'Жиры'],
  ['carbs', 'г', 'Углеводы'],
];

function displayRange(value) {
  if (!value || (value.lo === 0 && value.hi === 0)) return '0';

  const fmt = (n) =>
    Number.isInteger(n) ? String(n) : String(Number(n.toFixed(1))).replace('.', ',');

  return value.lo === value.hi ? fmt(value.lo) : `${fmt(value.lo)}–${fmt(value.hi)}`;
}

function nutrientValue(nutrients, key) {
  return nutrients?.[key] ? displayRange(nutrients[key]) : '—';
}

function dishType(dish, day) {
  if (day?.newProduct && dish.name === day.newProduct) {
    return 'new';
  }

  return 'continuation';
}

function DishRow({ dish, day }) {
  const type = dishType(dish, day);

  const className =
    type === 'new'
      ? 'dish-row dish-new'
      : type === 'addition'
        ? 'dish-row dish-addition'
        : 'dish-row dish-continuation';

  return (
    <div className={className}>
      <div className="dish-main">
        <div className="dish-name-line">
          <strong>{dish.name}</strong>
          <span className={`type-pill ${type}`}>
            {type === 'new'
              ? 'Новый продукт'
              : type === 'addition'
                ? 'Добавка'
                : 'В рационе'}
          </span>
        </div>

        <div className="dish-amount">
          Количество: <b>{dish.amount} {dish.unit === 'г, мл' ? 'г/мл' : dish.unit ?? ''}</b>
          {dish.tableRange && (
            <span className="range-note"> · ориентир: {dish.tableRange}</span>
          )}
        </div>

        <div className="nutrients">
          {nutrientLabels.map(([key, unit, label]) => (
            <div key={key}>
              <span>{label}</span>
              <strong>
                {nutrientValue(dish.nutrients, key)} <small>{unit}</small>
              </strong>
            </div>
          ))}
        </div>

        {dish.amountPolicy && type === 'new' && (
          <p className="amount-policy">{dish.amountPolicy}</p>
        )}

        {dish.hints?.map((hint, i) => (
          <p className="product-hint" key={`${hint.factId ?? i}`}>
            💡 {hint.text}
          </p>
        ))}

        {dish.addons?.map((addon, i) => (
          <div className="addon-line" key={`${addon.factId ?? i}`}>
            <span>＋ Добавка: {addon.name}</span>
            <strong>
              {addon.amount} {addon.unit === 'г, мл' ? 'г/мл' : addon.unit}
            </strong>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function ProductCard({ day, index }) {
  const isRestDay = !day.newProduct;

  const title =
    day.title ||
    (day.newProduct ? `Новый продукт: ${day.newProduct}` : 'День без новых продуктов');

  const mainDish =
    day.dishes?.find((dish) => dish.name === day.newProduct) ?? day.dishes?.[0];

  return (
    <article className={`day-card ${isRestDay ? 'rest-day-card' : ''}`}>
      <header className="day-card-header">
        <div>
          <span className="day-number">{String(index + 1).padStart(2, '0')}</span>
          <div>
            <h3>{title.replace(/^Новый продукт:\s*/i, 'Новый продукт — ')}</h3>
          </div>
        </div>
      </header>

      {day.note && <div className="day-note">ⓘ {day.note}</div>}

      <div className="dish-list">
        {day.dishes?.map((dish, i) => (
          <DishRow
            key={`${dish.factId ?? dish.name}-${i}`}
            dish={dish}
            day={day}
          />
        ))}
      </div>

      {day.totals && (
        <div className="day-totals">
          <strong>Итого по указанным блюдам</strong>
          <span>{displayRange(day.totals.energy)} ккал</span>
          <span>Б {displayRange(day.totals.protein)} г</span>
          <span>Ж {displayRange(day.totals.fat)} г</span>
          <span>У {displayRange(day.totals.carbs)} г</span>
        </div>
      )}
    </article>
  );
}