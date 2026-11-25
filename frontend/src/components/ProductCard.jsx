import './ProductCard.css';

import vegetablePuree from '../assets/images/ovoji.png';
import fruitPuree from '../assets/images/fuits.png';
import porridge from '../assets/images/kasha.png';
import meatPuree from '../assets/images/maso.png';
import fishPuree from '../assets/images/fish.png';
import yolk from '../assets/images/egg.png';
import babyCookies from '../assets/images/cooky.png';
import bread from '../assets/images/bread.png';
import kefir from '../assets/images/milk.png';
import cottageCheese from '../assets/images/tvorog.png';
import juice from '../assets/images/juse.png';

const dishImages = {
  'Овощное пюре': vegetablePuree,
  'Фруктовое пюре': fruitPuree,
  'Мясное пюре промышленного производства/отварное мясо': meatPuree,
  'Рыбное пюре': fishPuree,
  'Каша': porridge,
  'Желток': yolk,
  'Печенье детское': babyCookies,
  'Хлеб пшеничный, сухари': bread,
  'Кефир и другие детские неадаптированные кисломолочные напитки': kefir,
  'Творог': cottageCheese,
  'Фруктовый сок': juice,
};

const nutrientLabels = [
  ['energy', 'ккал', 'Энергия'],
  ['protein', 'г', 'Белки'],
  ['fat', 'г', 'Жиры'],
  ['carbs', 'г', 'Углеводы'],
];

export function displayRange(value) {
  if (value == null) return '—';

  const format = (number) =>
    Number.isInteger(number)
      ? String(number)
      : String(Number(number.toFixed(1))).replace('.', ',');

  if (typeof value === 'number') {
    return format(value);
  }

  if (value.lo == null || value.hi == null) {
    return '—';
  }

  if (value.lo === 0 && value.hi === 0) {
    return '0';
  }

  return value.lo === value.hi
    ? format(value.lo)
    : `${format(value.lo)}–${format(value.hi)}`;
}

function Nutrients({ data }) {
  return (
    <div className="nutrients">
      {nutrientLabels.map(([key, unit, label]) => (
        <div key={key}>
          <span>{label}</span>
          <strong>
            {displayRange(data?.[key])}
            <small> {unit}</small>
          </strong>
        </div>
      ))}
    </div>
  );
}

function DishRow({ dish, day }) {
  const isNew = day.newProduct === dish.name;

  return (
    <div className={`dish-row ${isNew ? 'dish-new' : 'dish-continuation'}`}>
      {dishImages[dish.name] && (
        <div className="dish-image-wrapper">
          <img
            className="dish-image"
            src={dishImages[dish.name]}
            alt={dish.name}
            loading="lazy"
          />
        </div>
      )}

      <div className="dish-main">
        <div className="dish-name-line">
          <strong>{dish.name}</strong>

          <span className={`type-pill ${isNew ? 'new' : 'continuation'}`}>
            {isNew ? 'Новый продукт' : 'Уже введён'}
          </span>
        </div>

        <div className="dish-amount">
          Количество:{' '}
          <b>
            {dish.amount}{' '}
            {dish.unit === 'г, мл' ? 'г/мл' : dish.unit ?? ''}
          </b>

          {dish.tableRange && (
            <span className="range-note">
              {' '}· ориентир: {dish.tableRange}
            </span>
          )}
        </div>

        <Nutrients data={dish.nutrients} />

        {dish.amountPolicy && isNew && (
          <p className="amount-policy">{dish.amountPolicy}</p>
        )}

        {dish.hints?.map((hint, index) => (
          <p className="product-hint" key={hint.factId ?? index}>
            💡 {hint.text}
          </p>
        ))}

        {dish.addons?.map((addon, index) => (
          <div className="addon-line" key={addon.factId ?? index}>
            <span>＋ Добавка: {addon.name}</span>

            <strong>
              {addon.amount}{' '}
              {addon.unit === 'г, мл' ? 'г/мл' : addon.unit}
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
    (day.newProduct
      ? `Новый продукт: ${day.newProduct}`
      : 'День без новых продуктов');

  return (
    <article className={`day-card ${isRestDay ? 'rest-day-card' : ''}`}>
      <header className="day-card-header">
        <div>
          <span className="day-number">
            {String(index + 1).padStart(2, '0')}
          </span>

          <div>
            <h3>
              {title.replace(/^Новый продукт:\s*/i, 'Новый продукт — ')}
            </h3>
          </div>
        </div>
      </header>

      {day.note && (
        <div className="day-note">ⓘ {day.note}</div>
      )}

      <div className="dish-list">
        {day.dishes?.map((dish, dishIndex) => (
          <DishRow
            key={`${dish.factId ?? dish.name}-${dishIndex}`}
            dish={dish}
            day={day}
          />
        ))}
      </div>

      {day.totals && (
        <div className="day-totals">
          <strong>Итого по продуктам прикорма</strong>

          <span>{displayRange(day.totals.energy)} ккал</span>
          <span>Б: {displayRange(day.totals.protein)} г</span>
          <span>Ж: {displayRange(day.totals.fat)} г</span>
          <span>У: {displayRange(day.totals.carbs)} г</span>
        </div>
      )}
    </article>
  );
}