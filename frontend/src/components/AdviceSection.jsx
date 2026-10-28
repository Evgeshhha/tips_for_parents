import { useState, useRef, useEffect } from 'react';
import './AdviceSection.css';

const priorityPhrases = [
  {
    match: /5.?7.?дней|5–7|5-7/,
    title: 'Увеличивайте постепенно',
    tone: 'peach',
  },
  {
    match: /первой половине дня/,
    title: 'Предлагайте в первой половине дня',
    tone: 'yellow',
  },
  {
    match: /монокомпонентных/,
    title: 'Начинайте с одного продукта',
    tone: 'green',
  },
  {
    match: /острых инфекционных|прививок/,
    title: 'Выбирайте спокойный период',
    tone: 'blue',
  },
  {
    match: /грудного вскармливания/,
    title: 'Продолжайте основное вскармливание',
    tone: 'blue',
  },
  {
    match: /предлагать воду/,
    title: 'Предлагайте воду между кормлениями',
    tone: 'blue',
  },
  {
    match: /9–10|мелкоизмельченные/,
    title: 'Постепенно меняйте текстуру',
    tone: 'peach',
  },
  {
    match: /с\s*ложечки/i,
    title: 'Начинайте с ложечки',
    tone: 'peach',
  } ,
];

function selectAdvice(rules = []) {
  const tones = ['peach', 'yellow', 'green', 'blue'];

  return rules.map((rule, index) => {
    const priority = priorityPhrases.find((item) =>
      item.match.test(rule.text)
    );

    return {
      factId: rule.factId,
      text: rule.text,
      title: priority?.title ?? `Рекомендация ${index + 1}`,
      tone: priority?.tone ?? tones[index % tones.length],
    };
  });
}


export default function AdviceSection({ monthData }) {
  const advice = selectAdvice(monthData?.rules ?? []);
  const restrictions = monthData?.restrictions ?? [];
  const [activeIndex, setActiveIndex] = useState(0);
  const stageRef = useRef(null);

  const goTo = (index) => {
    setActiveIndex(Math.max(0, Math.min(index, advice.length - 1)));
  };

      useEffect(() => {
        const stage = stageRef.current;

        if (!stage) return;

        const handleWheel = (event) => {
          // Не даём странице прокручиваться над сценой карточек.
          event.preventDefault();

          if (advice.length <= 1) return;

          const delta =
            Math.abs(event.deltaY) >= Math.abs(event.deltaX)
              ? event.deltaY
              : event.deltaX;

          if (delta === 0) return;

          setActiveIndex((current) => {
            const direction = delta > 0 ? 1 : -1;

            return Math.max(
              0,
              Math.min(current + direction, advice.length - 1)
            );
          });
        };

        stage.addEventListener('wheel', handleWheel, {
          passive: false,
        });

        return () => {
          stage.removeEventListener('wheel', handleWheel);
        };
      }, [advice.length]);

  return (
    <section className="advice-section" id="tips">
      <div className="advice-heading">
        <div>
          <p className="eyebrow">КОРОТКО О ГЛАВНОМ</p>
          <h2>Важные рекомендации</h2>
        </div>
      </div>

      {advice.length > 0 && (
        <div className="advice-deck">
            <div
              className="advice-deck-stage"
              ref={stageRef}
            >
            {advice.map((item, index) => {
              const offset = index - activeIndex;
              const distance = Math.abs(offset);

              return (
                <article
                  key={item.title}
                  className={`advice-card ${item.tone} ${
                    offset === 0 ? 'active' : ''
                  }`}

                style={{
                  zIndex: advice.length - distance,
                  opacity: distance > 2 ? 0 : 1,
                  pointerEvents: distance > 1 ? 'none' : 'auto',
                  transform: `
                    translate(-50%, -50%)
                    translateX(${offset * 65}px)
                    translateY(${distance * 5}px)
                    rotate(${offset * 4}deg)
                    scale(${1 - distance * 0.04})
                  `,
                }}

                  onClick={() => {
                    if (offset !== 0) goTo(index);
                  }}
                  aria-hidden={offset !== 0}
                >
                  <span className="advice-card-number">
                    {String(index + 1).padStart(2, '0')}
                  </span>

                  <h3>{item.title}</h3>
                  <p>{item.text}</p>
                </article>
              );
            })}
          </div>

          <div className="advice-deck-controls">
            <button
              type="button"
              className="advice-arrow"
              onClick={() => goTo(activeIndex - 1)}
              disabled={activeIndex === 0}
              aria-label="Предыдущая рекомендация"
            >
              ←
            </button>

            <div className="advice-progress">
              <span>
                {activeIndex + 1} из {advice.length}
              </span>

              <div className="advice-progress-track">
                <div
                  className="advice-progress-fill"
                  style={{
                    width: `${
                      ((activeIndex + 1) / advice.length) * 100
                    }%`,
                  }}
                />
              </div>
            </div>

            <button
              type="button"
              className="advice-arrow"
              onClick={() => goTo(activeIndex + 1)}
              disabled={activeIndex === advice.length - 1}
              aria-label="Следующая рекомендация"
            >
              →
            </button>
          </div>
        </div>
      )}

      <p className="medical-disclaimer">
        Эти сведения помогают ориентироваться в источнике и не заменяют
        рекомендации педиатра. При введении нового продукта учитывайте
        индивидуальную готовность ребёнка и следуйте советам специалиста.
      </p>
    </section>
  );
}
