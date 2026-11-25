import { useEffect, useState } from 'react';
import { API_URL } from '../config.js';
import './ChildProfile.css';

import vegetablePuree from '../assets/products/childpit.png';
import fruitPuree from '../assets/products/fruit.png';
import milk from '../assets/products/milk.png';
import meatPuree from '../assets/products/meat.png';
import fishPuree from '../assets/products/fish.png';
import egg from '../assets/products/egs.png';
import ric from '../assets/products/ric.png';
import kasha from '../assets/products/kasha.png';
import juise from '../assets/products/juise.png';
import cookies from '../assets/products/cookies.png';
import bread from '../assets/products/bread.png';
import childImage from '../assets/images/child.png';

const productIcons = {
  'овощное пюре': vegetablePuree,
  'каша': kasha,
  'мясное пюре промышленного производства/отварное мясо': meatPuree,
  'фруктовое пюре': fruitPuree,
  'желток': egg,
  'творог': ric,
  'рыбное пюре': fishPuree,
  'фруктовый сок': juise,
  'кефир и другие детские неадаптированные кисломолочные напитки': milk,
  'печенье детское': cookies,
  'хлеб пшеничный, сухари': bread,
};

const statusInfo = {
  INTRODUCED: { label: 'Введён' },
  INTRODUCING: { label: 'Вводится' },
  NOT_STARTED: { label: 'Не введён' },
  PAUSED: { label: 'На паузе' },
  EXCLUDED: { label: 'Исключён' },
};

const feedingTypeLabels = {
  BREAST: 'Грудное вскармливание',
  FORMULA: 'Искусственное вскармливание',
};

function formatDate(date) {
  if (!date) return 'Не указана';

  const [year, month, day] = date.split('-');

  if (!year || !month || !day) return date;

  return `${day}.${month}.${year}`;
}

function formatAge(months) {
  if (months === null || months === undefined) {
    return 'Возраст не указан';
  }

  const lastTwoDigits = months % 100;
  const lastDigit = months % 10;

  let word = 'месяцев';

  if (lastTwoDigits < 11 || lastTwoDigits > 14) {
    if (lastDigit === 1) word = 'месяц';
    else if (lastDigit >= 2 && lastDigit <= 4) word = 'месяца';
  }

  return `${months} ${word}`;
}

function getCurrentDate() {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

export default function ChildProfile({
  child,
  onCreatePlan,
  onChildUpdated,
}) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [savingProducts, setSavingProducts] = useState({});

  // Редактирование отдельных параметров питания
  const [editingField, setEditingField] = useState(null);
  const [editValue, setEditValue] = useState('');
  const [savingField, setSavingField] = useState(false);
  const [profileSaveError, setProfileSaveError] = useState('');
  const [profileSaveMessage, setProfileSaveMessage] = useState('');

  // Добавление измерения веса
  const [weightDate, setWeightDate] = useState(getCurrentDate());
  const [weightValue, setWeightValue] = useState('');
  const [savingWeight, setSavingWeight] = useState(false);
  const [weightMessage, setWeightMessage] = useState('');
  const [weightError, setWeightError] = useState('');

  // Загружаем каталог и статусы продуктов
  useEffect(() => {
    if (!child?.id) {
      setLoading(false);
      return;
    }

    const controller = new AbortController();

    async function loadProducts() {
      try {
        setLoading(true);
        setError('');

        const [catalogResponse, statusesResponse] = await Promise.all([
          fetch(`${API_URL}/api/products`, {
            signal: controller.signal,
          }),
          fetch(`${API_URL}/api/children/${child.id}/products`, {
            signal: controller.signal,
          }),
        ]);

        if (!catalogResponse.ok || !statusesResponse.ok) {
          throw new Error('Не удалось загрузить информацию о продуктах.');
        }

        const catalog = await catalogResponse.json();
        const savedStatuses = await statusesResponse.json();

        console.log('Каталог продуктов:', catalog);

        console.table(
          catalog.map((product) => ({
            code: product.code,
            label: product.label,
          }))
        );

        const mergedProducts = catalog.map((product) => {
          const savedProduct = savedStatuses.find(
            (item) => item.productCode === product.code
          );

          return {
            code: product.code,
            label: product.label,
            status: savedProduct?.status || 'NOT_STARTED',
            startedOn: savedProduct?.startedOn || null,
          };
        });

        setProducts(mergedProducts);
      } catch (err) {
        if (err.name !== 'AbortError') {
          console.error('Ошибка загрузки продуктов:', err);
          setError(err.message || 'Произошла ошибка при загрузке продуктов.');
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadProducts();

    return () => controller.abort();
  }, [child?.id]);

  // Изменение статуса продукта
  async function handleStatusChange(product, newStatus) {
    setSavingProducts((prev) => ({
      ...prev,
      [product.code]: true,
    }));

    setError('');

    const requestBody = {
      productCode: product.code,
      status: newStatus,
      startedOn: product.startedOn || getCurrentDate(),
    };

    try {
      const response = await fetch(
        `${API_URL}/api/children/${child.id}/products`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(requestBody),
        }
      );

      if (!response.ok) {
        throw new Error('Не удалось сохранить статус продукта.');
      }

      const responseData = await response.json().catch(() => null);

      const savedProduct = responseData?.productCode
        ? responseData
        : requestBody;

      setProducts((prev) =>
        prev.map((item) =>
          item.code === product.code
            ? {
                ...item,
                status: savedProduct.status,
                startedOn: savedProduct.startedOn,
              }
            : item
        )
      );
    } catch (err) {
      console.error('Ошибка изменения статуса:', err);
      setError(err.message || 'Не удалось сохранить изменения.');
    } finally {
      setSavingProducts((prev) => ({
        ...prev,
        [product.code]: false,
      }));
    }
  }

  // Открытие редактирования отдельного поля
  function handleEditField(field) {
    setProfileSaveError('');
    setProfileSaveMessage('');

    if (field === 'feedingType') {
      setEditValue(child.feedingType || 'BREAST');
    } else if (field === 'milkMl') {
      setEditValue(child.milkMl ?? '');
    }

    setEditingField(field);
  }

  // Отмена редактирования
  function handleCancelEdit() {
    setEditingField(null);
    setEditValue('');
    setProfileSaveError('');
  }

  // Сохранение одного параметра питания
  async function handleSaveField() {
    setProfileSaveError('');
    setProfileSaveMessage('');

    let value = editValue;

    if (editingField === 'milkMl') {
      value = Number(editValue);

      if (
        editValue === '' ||
        !Number.isInteger(value) ||
        value < 0 ||
        value > 2000
      ) {
        setProfileSaveError('Введите целое число от 0 до 2000.');
        return;
      }
    }

    if (editingField === 'feedingType') {
      if (!['BREAST', 'FORMULA'].includes(value)) {
        setProfileSaveError('Выберите тип вскармливания.');
        return;
      }
    }

    if (!editingField) return;

    try {
      setSavingField(true);

      // Отправляем только изменяемое поле
      const requestBody = {
        [editingField]: value,
      };

      const response = await fetch(
        `${API_URL}/api/children/${child.id}`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(requestBody),
        }
      );

      const responseData = await response.json().catch(() => null);

      if (!response.ok) {
        throw new Error(
          responseData?.message ||
          responseData?.detail ||
          `Не удалось сохранить изменения. Код ошибки: ${response.status}`
        );
      }

      // Обновляем профиль в App.jsx и инициируем обновление плана
      onChildUpdated?.(responseData);

      setEditingField(null);
      setEditValue('');
      setProfileSaveMessage('Изменения успешно сохранены.');
    } catch (err) {
      console.error('Ошибка обновления параметра:', err);

      setProfileSaveError(
        err.message || 'Не удалось сохранить изменения.'
      );
    } finally {
      setSavingField(false);
    }
  }

  // Добавление измерения веса
  async function handleAddWeight(event) {
    event.preventDefault();

    setWeightMessage('');
    setWeightError('');

    const weightKg = Number(weightValue);

    if (
      weightValue === '' ||
      !Number.isFinite(weightKg) ||
      weightKg < 2 ||
      weightKg > 20
    ) {
      setWeightError('Введите число от 2 до 20 кг.');
      return;
    }

    if (!weightDate || weightDate > getCurrentDate()) {
      setWeightError('Дата измерения не может быть будущей.');
      return;
    }

    try {
      setSavingWeight(true);

      const response = await fetch(
        `${API_URL}/api/children/${child.id}/weights`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            measuredOn: weightDate,
            weightKg,
          }),
        }
      );

      if (!response.ok) {
        const responseData = await response.json().catch(() => null);

        throw new Error(
          responseData?.message ||
          responseData?.detail ||
          `Не удалось сохранить измерение. Код ошибки: ${response.status}`
        );
      }

      setWeightMessage(
        'Измерение успешно сохранено. Персональный план обновлён.'
      );
      setWeightValue('');

      // Backend уже пересчитал план, обновляем его данные в App.jsx
      onChildUpdated?.(child);
    } catch (err) {
      console.error('Ошибка сохранения веса:', err);

      setWeightError(
        err.message || 'Не удалось сохранить измерение.'
      );
    } finally {
      setSavingWeight(false);
    }
  }

  if (!child) {
    return (
      <main className="child-profile-page">
        <p>Не удалось загрузить информацию о ребёнке.</p>
      </main>
    );
  }

  const childName = child.nickname?.trim() || 'Малыш';

  const birthWeight =
    child.birthWeightKg != null
      ? `${String(child.birthWeightKg).replace('.', ',')} кг`
      : 'Не указан';

  const milkVolume =
    child.milkMl != null ? `${child.milkMl} мл` : 'Не указан';

  const feedingType =
    feedingTypeLabels[child.feedingType] || 'Не указан';

  return (
    <main className="child-profile-page">
      <div className="child-profile-heading">
        <div>
          <h1>Профиль малыша</h1>
          <p>
            Здесь Вы можете просматривать и редактировать информацию
            о ребёнке, а также управлять его планом прикорма.
          </p>
        </div>
      </div>

      {/* Основная информация */}
      <section className="child-info-card">
        <div className="child-photo-wrapper">
          <div className="child-photo-placeholder">
            <img src={childImage} alt="Ребёнок" />
          </div>
        </div>

        <div className="child-main-info">
          <div className="child-name-row">
            <h2>{childName}</h2>
            <span className="child-age">
              {formatAge(child.ageMonths)}
            </span>
          </div>

          <p className="child-birth-date">
            Дата рождения: <strong>{formatDate(child.birthDate)}</strong>
          </p>

          <div className="child-weight-grid">
            {/* Вес при рождении */}
            <div className="child-info-item">
              <span className="info-icon">⚖️</span>
              <div>
                <span>Вес при рождении</span>
                <strong>{birthWeight}</strong>
              </div>
            </div>

            {/* Тип вскармливания */}
            <div className="child-info-item">
              <span className="info-icon">🍼</span>

              <div className="editable-info-content">
                <span>Тип вскармливания</span>

                {editingField === 'feedingType' ? (
                  <div className="inline-edit-control">
                    <select
                      value={editValue}
                      onChange={(event) => setEditValue(event.target.value)}
                      disabled={savingField}
                    >
                      <option value="BREAST">
                        Грудное вскармливание
                      </option>
                      <option value="FORMULA">
                        Искусственное вскармливание
                      </option>
                    </select>

                    <div className="inline-edit-actions">
                      <button
                        type="button"
                        className="inline-save-button"
                        onClick={handleSaveField}
                        disabled={savingField}
                        aria-label="Сохранить тип вскармливания"
                        title="Сохранить"
                      >
                        {savingField ? '...' : '✓'}
                      </button>

                      <button
                        type="button"
                        className="inline-cancel-button"
                        onClick={handleCancelEdit}
                        disabled={savingField}
                        aria-label="Отменить редактирование"
                        title="Отмена"
                      >
                        ×
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="editable-info-value">
                    <strong>{feedingType}</strong>

                    <button
                      type="button"
                      className="field-edit-button"
                      onClick={() => handleEditField('feedingType')}
                      aria-label="Изменить тип вскармливания"
                      title="Изменить"
                    >
                      ✎
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Объём молока */}
            <div className="feeding-type">
              <span className="feeding-type-icon">🥛</span>

              <div className="editable-info-content">
                <span>Объём молока за сутки</span>

                {editingField === 'milkMl' ? (
                  <div className="inline-edit-control">
                    <input
                      type="number"
                      min="0"
                      max="2000"
                      step="1"
                      value={editValue}
                      onChange={(event) => setEditValue(event.target.value)}
                      disabled={savingField}
                      placeholder="Объём в мл"
                    />

                    <div className="inline-edit-actions">
                      <button
                        type="button"
                        className="inline-save-button"
                        onClick={handleSaveField}
                        disabled={savingField}
                        aria-label="Сохранить объём молока"
                        title="Сохранить"
                      >
                        {savingField ? '...' : '✓'}
                      </button>

                      <button
                        type="button"
                        className="inline-cancel-button"
                        onClick={handleCancelEdit}
                        disabled={savingField}
                        aria-label="Отменить редактирование"
                        title="Отмена"
                      >
                        ×
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="editable-info-value">
                    <strong>{milkVolume}</strong>

                    <button
                      type="button"
                      className="field-edit-button"
                      onClick={() => handleEditField('milkMl')}
                      aria-label="Изменить объём молока"
                      title="Изменить"
                    >
                      ✎
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>

          {profileSaveError && (
            <p className="profile-form-error" role="alert">
              {profileSaveError}
            </p>
          )}

          {profileSaveMessage && (
            <p className="profile-form-success" role="status">
              {profileSaveMessage}
            </p>
          )}
        </div>
      </section>

      {/* Добавление измерения веса */}
      <section className="weight-measurement-card">
        <div className="weight-measurement-heading">
          <span className="weight-measurement-icon">⚖️</span>

          <div>
            <h2>Измерения веса</h2>
            <p>
              Добавьте новое измерение, чтобы обновить данные
              персонального плана.
            </p>
          </div>
        </div>

        <form
          className="weight-measurement-form"
          onSubmit={handleAddWeight}
        >
          <label>
            Дата измерения
            <input
              type="date"
              value={weightDate}
              max={getCurrentDate()}
              onChange={(event) => setWeightDate(event.target.value)}
              required
            />
          </label>

          <label>
            Вес (кг)
            <input
              type="number"
              min="2"
              max="20"
              step="0.01"
              placeholder="Например, 7.25"
              value={weightValue}
              onChange={(event) => setWeightValue(event.target.value)}
              required
            />
          </label>

          <button
            type="submit"
            className="weight-save-button"
            disabled={savingWeight}
          >
            {savingWeight ? 'Сохранение...' : 'Добавить измерение'}
          </button>
        </form>

        {weightError && (
          <p className="profile-form-error" role="alert">
            {weightError}
          </p>
        )}

        {weightMessage && (
          <p className="profile-form-success" role="status">
            {weightMessage}
          </p>
        )}
      </section>

      {/* Продукты и статусы */}
      <section className="child-products-section">
        <div className="products-heading">
          <div>
            <h2>Продукты и статус введения</h2>
            <p>Отмечайте, какие продукты уже знакомы малышу</p>
          </div>
        </div>

        {loading && (
          <p className="products-message">
            Загружаем список продуктов...
          </p>
        )}

        {error && (
          <p className="products-error">
            {error}
          </p>
        )}

        {!loading && !error && (
          <div className="child-products-list">
            {products.map((product) => (
              <div className="child-product-row" key={product.code}>
                  <div className="child-product-icon">
                    <img
                      src={productIcons[product.code] || vegetablePuree}
                      alt=""
                      aria-hidden="true"
                    />
                  </div>

                <strong>{product.label}</strong>

                <select
                  className={`product-status-select ${product.status.toLowerCase()}`}
                  value={product.status}
                  disabled={savingProducts[product.code]}
                  onChange={(event) =>
                    handleStatusChange(product, event.target.value)
                  }
                  aria-label={`Статус продукта: ${product.label}`}
                >
                  {Object.entries(statusInfo).map(([value, info]) => (
                    <option key={value} value={value}>
                      {info.label}
                    </option>
                  ))}
                </select>

                {savingProducts[product.code] && (
                  <span className="saving-indicator">
                    Сохранение...
                  </span>
                )}
              </div>
            ))}
          </div>
        )}

        {!loading && !error && products.length === 0 && (
          <p className="products-message">
            Каталог продуктов пока пуст.
          </p>
        )}
      </section>

      {/* Создание персонального календаря */}
      <section className="personal-plan-banner">
        <div className="plan-banner-decoration">🐰</div>

        <div className="plan-banner-text">
          <h2>Персональный план прикорма</h2>
          <p>
            На основе данных о ребёнке мы подберём индивидуальный
            план с учётом возраста и особенностей питания.
          </p>
        </div>

        <button
          className="create-personal-plan-button"
          type="button"
          onClick={() => onCreatePlan?.()}
        >
          Создать (обновить) персональный план
          <span>→</span>
        </button>
      </section>
    </main>
  );
}
