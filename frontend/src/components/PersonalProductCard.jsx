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

const statusInfo = {
  NOT_STARTED: {
    label: 'Не введено',
    className: 'not-started',
  },
  INTRODUCING: {
    label: 'Вводится',
    className: 'introducing',
  },
  INTRODUCED: {
    label: 'В рационе',
    className: 'introduced',
  },
  PAUSED: {
    label: 'Пауза',
    className: 'paused',
  },
  EXCLUDED: {
    label: 'Исключено',
    className: 'excluded',
  },
};

function normalizeProductCode(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ');
}

function dishType(dish, day, productStatuses) {
  // Стандартный календарь
  if (productStatuses === undefined) {
    if (
      day?.newProduct &&
      normalizeProductCode(dish.name) ===
        normalizeProductCode(day.newProduct)
    ) {
      return 'new';
    }

    return 'continuation';
  }

  // Проверяем статус продукта в рационе ребёнка
  const product = productStatuses.find(
    (item) =>
      normalizeProductCode(item.productCode) ===
      normalizeProductCode(dish.name)
  );

  // Если продукт уже есть в /products, используем его статус
  if (product?.status) {
    return product.status;
  }

  // Если продукт является новым на текущем этапе,
  // отмечаем его как вводимый
  const isNewProduct =
    normalizeProductCode(dish.name) ===
      normalizeProductCode(day?.newProduct) ||
    normalizeProductCode(dish.name) ===
      normalizeProductCode(day?.newProductCode);

  if (isNewProduct) {
    return 'INTRODUCING';
  }

  return 'NOT_STARTED';
}

function getProductStatus(dish, productStatuses) {
  // Защита: если API вернул не массив, используем пустой массив
  const statuses = Array.isArray(productStatuses)
    ? productStatuses
    : [];

  const dishName = normalizeProductCode(dish.name);

  const product = statuses.find(
    (item) =>
      normalizeProductCode(item.productCode) === dishName
  );

  if (product?.status && statusInfo[product.status]) {
    return {
      ...statusInfo[product.status],
      startedOn: product.startedOn,
    };
  }

  // Если продукта нет в /products, считаем его ещё не введённым
  return statusInfo.NOT_STARTED;
}

function formatDate(value) {
  if (!value) return null;

  const parts = value.split('-');

  if (parts.length !== 3) return value;

  return `${parts[2]}.${parts[1]}.${parts[0]}`;
}

function displayRange(value) {
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

function NutrientGrid({ data, isPercent = false }) {
  return (
    <div className="personal-nutrients">
      {nutrientLabels.map(([key, unit, label]) => (
        <div className="personal-nutrient" key={key}>
          <span>{label}</span>

          <strong>
            {displayRange(data?.[key])}
            {!isPercent && <small> {unit}</small>}
            {isPercent && data?.[key] != null && '%'}
          </strong>
        </div>
      ))}
    </div>
  );
}

function PersonalDishRow({ dish, day, productStatuses }) {
  const status = getProductStatus(dish, productStatuses);

  return (
    <div className={`personal-dish ${status.className}`}>
      {dishImages[dish.name] && (
        <div className="personal-dish-image">
          <img
            src={dishImages[dish.name]}
            alt={dish.name}
            loading="lazy"
          />
        </div>
      )}

      <div className="personal-dish-content">
        <div className="personal-dish-heading">
          <strong>{dish.name}</strong>

          <span className={`personal-status ${status.className}`}>
            {status.label}
          </span>
        </div>

        {status.startedOn && (
          <p className="personal-started-date">
            Начало введения: {formatDate(status.startedOn)}
          </p>
        )}

        <div className="personal-amount">
          Количество:{' '}
          <strong>
            {dish.amount}{' '}
            {dish.unit === 'г, мл' ? 'г/мл' : dish.unit ?? ''}
          </strong>

          {dish.tableRange && (
            <span> · ориентир: {dish.tableRange}</span>
          )}
        </div>

        <NutrientGrid data={dish.nutrients} />

        {dish.vsTable && (
          <p className="personal-table-note">
            {dish.vsTable}
          </p>
        )}

        {dish.amountPolicy && status.className === 'introducing' && (
          <p className="personal-amount-policy">
            {dish.amountPolicy}
          </p>
        )}

        {dish.hints?.map((hint, index) => (
          <p
            className="personal-hint"
            key={hint.factId ?? index}
          >
            💡 {hint.text}
          </p>
        ))}

        {dish.addons?.map((addon, index) => (
          <div
            className="personal-addon"
            key={addon.factId ?? index}
          >
            <span>＋ {addon.name}</span>

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

function PersonalSummary({ title, data, isPercent = false }) {
  if (!data) return null;

  return (
    <div className="personal-summary">
      <h4>{title}</h4>
      <NutrientGrid data={data} isPercent={isPercent} />
    </div>
  );
}

export default function PersonalProductCard({
  day,
  index,
  productStatuses = [],
}) {
  const title =
    day.title ||
    (day.newProduct
      ? `Новый продукт: ${day.newProduct}`
      : 'День без новых продуктов');

  const safeStatuses = Array.isArray(productStatuses)
    ? productStatuses
    : [];

  return (
    <article className="personal-day-card">
      <header className="personal-day-header">
        <div className="personal-day-title">
          <span className="personal-day-number">
            {String(index + 1).padStart(2, '0')}
          </span>

          <div>
            <span className="personal-day-caption">
              ЭТАП ПЕРСОНАЛЬНОГО ПЛАНА
            </span>

            <h3>
              {title.replace(/^Новый продукт:\s*/i, 'Новый продукт — ')}
            </h3>
          </div>
        </div>
      </header>

      {day.note && (
        <div className="personal-day-note">
          <span>ⓘ</span>
          {day.note}
        </div>
      )}

      <div className="personal-dishes">
        {day.dishes?.map((dish, dishIndex) => (
          <PersonalDishRow
            key={`${dish.factId ?? dish.name}-${dishIndex}`}
            dish={dish}
            day={day}
            productStatuses={safeStatuses}
          />
        ))}
      </div>

      {day.totals && (
        <PersonalSummary
          title="Пищевая ценность продуктов прикорма"
          data={day.totals}
        />
      )}

      <PersonalSummary
        title="Общая пищевая ценность за день с учётом молока"
        data={day.dayTotalsWithMilk}
      />

      <PersonalSummary
        title="Доля суточной потребности"
        data={day.percentOfDailyNeed}
        isPercent
      />

      <PersonalSummary
        title="Доля целевого количества прикорма"
        data={day.percentOfPrikormTarget}
        isPercent
      />
    </article>
  );
}