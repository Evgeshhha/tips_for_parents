import { useState } from 'react';
import './CreateChildProfile.css';

const initialForm = {
  nickname: '',
  birthDate: '',
  birthWeightKg: '',
  feedingType: 'BREAST',
  milkMl: '',
};

export default function CreateChildProfile({ onSubmit, loading = false }) {
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));

    setErrors((prev) => ({
      ...prev,
      [name]: '',
    }));
  }

  function validate() {
    const newErrors = {};

    if (form.nickname.trim().length > 100) {
      newErrors.nickname = 'Имя не должно превышать 100 символов';
    }

    if (!form.birthDate) {
      newErrors.birthDate = 'Укажите дату рождения';
    } else {
      const selectedDate = new Date(`${form.birthDate}T00:00:00`);
      const today = new Date();
      today.setHours(23, 59, 59, 999);

      if (Number.isNaN(selectedDate.getTime()) || selectedDate > today) {
        newErrors.birthDate = 'Дата рождения не может быть в будущем';
      }
    }

    const weight = Number(form.birthWeightKg);

    if (!form.birthWeightKg) {
      newErrors.birthWeightKg = 'Укажите вес при рождении';
    } else if (!Number.isFinite(weight) || weight < 1 || weight > 7) {
      newErrors.birthWeightKg = 'Вес должен быть от 1 до 7 кг';
    }

    if (form.milkMl !== '') {
      const milk = Number(form.milkMl);

      if (!Number.isInteger(milk) || milk < 0 || milk > 2000) {
        newErrors.milkMl = 'Укажите целое число от 0 до 2000 мл';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  function handleSubmit(event) {
    event.preventDefault();

    if (!validate()) return;

    const data = {
      nickname: form.nickname.trim() || null,
      birthDate: form.birthDate,
      birthWeightKg: Number(form.birthWeightKg),
      feedingType: form.feedingType,
      milkMl: form.milkMl === '' ? null : Number(form.milkMl),
    };

    onSubmit?.(data);
  }

  return (
    <main className="create-profile-page">
      <div className="create-profile-card">

        <div className="create-profile-heading">

          <h1>Давайте познакомимся!</h1>

          <p>
            Расскажите немного о малыше, чтобы мы могли
            подготовить персональный профиль и календарь прикорма.
          </p>
        </div>

        <form onSubmit={handleSubmit} noValidate>

          <div className="create-profile-fields">

            <div className="create-field">
              <label htmlFor="nickname">
                Как будем обращаться к малышу?
              </label>

              <input
                id="nickname"
                name="nickname"
                type="text"
                placeholder="Например, Малыш"
                value={form.nickname}
                onChange={handleChange}
                maxLength={100}
              />

              {errors.nickname && (
                <span className="create-field-error">
                  {errors.nickname}
                </span>
              )}
            </div>

            <div className="create-field">
              <label htmlFor="birthDate">
                Дата рождения <span>*</span>
              </label>

              <input
                id="birthDate"
                name="birthDate"
                type="date"
                value={form.birthDate}
                onChange={handleChange}
                max={new Date().toLocaleDateString('en-CA')}
              />

              {errors.birthDate && (
                <span className="create-field-error">
                  {errors.birthDate}
                </span>
              )}
            </div>

            <div className="create-field">
              <label htmlFor="birthWeightKg">
                Вес при рождении (кг) <span>*</span>
              </label>

              <input
                id="birthWeightKg"
                name="birthWeightKg"
                type="number"
                placeholder="Например, 3.3"
                value={form.birthWeightKg}
                onChange={handleChange}
                min="1"
                max="7"
                step="0.1"
              />

              {errors.birthWeightKg && (
                <span className="create-field-error">
                  {errors.birthWeightKg}
                </span>
              )}
            </div>

            <div className="create-field">
              <label>
                Тип вскармливания <span>*</span>
              </label>

              <div className="feeding-options">

                <label
                  className={`feeding-option ${
                    form.feedingType === 'BREAST' ? 'selected' : ''
                  }`}
                >
                  <input
                    type="radio"
                    name="feedingType"
                    value="BREAST"
                    checked={form.feedingType === 'BREAST'}
                    onChange={handleChange}
                  />

                  <span className="feeding-option-icon">🤱</span>

                  <span className="feeding-option-text">
                    <strong>Грудное</strong>
                    <small>Грудное молоко</small>
                  </span>

                  <span className="feeding-option-check">✓</span>
                </label>

                <label
                  className={`feeding-option ${
                    form.feedingType === 'FORMULA' ? 'selected' : ''
                  }`}
                >
                  <input
                    type="radio"
                    name="feedingType"
                    value="FORMULA"
                    checked={form.feedingType === 'FORMULA'}
                    onChange={handleChange}
                  />

                  <span className="feeding-option-icon">🍼</span>

                  <span className="feeding-option-text">
                    <strong>Искусственное</strong>
                    <small>Детская смесь</small>
                  </span>

                  <span className="feeding-option-check">✓</span>
                </label>

              </div>
            </div>

            <div className="create-field">
              <label htmlFor="milkMl">
                Объём молока за сутки (мл)
              </label>

              <input
                id="milkMl"
                name="milkMl"
                type="number"
                placeholder="Например, 700"
                value={form.milkMl}
                onChange={handleChange}
                min="0"
                max="2000"
                step="1"
              />

              {errors.milkMl && (
                <span className="create-field-error">
                  {errors.milkMl}
                </span>
              )}
            </div>

          </div>

          <p className="create-profile-required">
            <span>*</span> Обязательные поля
          </p>

          <button
            type="submit"
            className="create-profile-submit"
            disabled={loading}
          >
            {loading ? 'Создаём профиль...' : 'Создать профиль'}
            {!loading && <span>→</span>}
          </button>

        </form>

        <p className="create-profile-footer">
          Все данные сохраняются в профиле малыша и используются
          для формирования персонального календаря.
        </p>

      </div>
    </main>
  );
}
