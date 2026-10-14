import logka from "../assets/icons/logka.png";
import mnogo from "../assets/icons/mnogo.png";
import pokoi from "../assets/icons/pokoi.png";
import time from "../assets/icons/time.png";
import water from "../assets/icons/water.png";
import knife from "../assets/icons/knife.png";
import milk from "../assets/icons/milk.png";

const priorityPhrases = [
  {
    match: /5.?7.?дней|5–7|5-7/,
    title: 'Увеличивайте постепенно',
    icon: mnogo,
    tone: 'peach',
  },
  {
    match: /первой половине дня/,
    title: 'Предлагайте в первой половине дня',
    icon: time,
    tone: 'yellow',
  },
  {
    match: /монокомпонентных/,
    title: 'Начинайте с одного продукта',
    icon: logka,
    tone: 'green',
  },
  {
    match: /острых инфекционных|прививок/,
    title: 'Выбирайте спокойный период',
    icon: pokoi,
    tone: 'blue',
  },
  {
    match: /грудного вскармливания/,
    title: 'Продолжайте основное вскармливание',
    icon: milk,
    tone: 'blue',
  },
  {
    match: /предлагать воду/,
    title: 'Предлагайте воду между кормлениями',
    icon: water,
    tone: 'blue',
  },
  {
    match: /9–10|мелкоизмельченные/,
    title: 'Постепенно меняйте текстуру',
    icon: knife,
    tone: 'peach',
  },
];

function selectAdvice(rules = []) {
  const result = [];

  for (const priority of priorityPhrases) {
    const match = rules.find((rule) => priority.match.test(rule.text));

    if (match && !result.some((item) => item.text === match.text)) {
      result.push({ ...priority, text: match.text });
    }
  }

  return result;
}

export default function AdviceSection({ monthData }) {
  const advice = selectAdvice(monthData?.rules ?? []);
  const restrictions = monthData?.restrictions ?? [];

  return (
    <section className="advice-section" id="tips">
      <div className="advice-heading">
        <div>
          <p className="eyebrow">КОРОТКО О ГЛАВНОМ</p>
          <h2>Важные рекомендации</h2>
        </div>
      </div>

      <div className="advice-grid">
        {advice.map((item) => (
          <article key={item.title} className={`advice-card ${item.tone}`}>
            <img
              className="advice-icon"
              src={item.icon}
              alt=""
            />

            <h3>{item.title}</h3>
            <p>{item.text}</p>
          </article>
        ))}
      </div>

      {restrictions.length > 0 && (
        <div className="restriction-box">
          <strong>Обратите внимание</strong>
          {restrictions.slice(0, 2).map((item, i) => (
            <p key={item.factId ?? i}>{item.text}</p>
          ))}
        </div>
      )}

      <p className="medical-disclaimer">
        Эти сведения помогают ориентироваться в источнике и не заменяют
        рекомендации педиатра. При введении нового продукта учитывайте
        индивидуальную готовность ребёнка и рекомендации специалиста.
      </p>
    </section>
  );
}