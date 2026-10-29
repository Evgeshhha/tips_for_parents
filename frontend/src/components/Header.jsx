import profileIcon from "../assets/icons/icon_profile.png";
import './Header.css';
export default function Header() {
  return (
    <header className="site-header">
      <a className="brand" href="#top">
        <span className="brand-mark">♡</span>
        <span>
          <strong>Шаг за шагом</strong>
          <small>Поддержка на каждом этапе</small>
        </span>
      </a>

      <nav className="top-nav">
        <a href="#home">Главная</a>
        <a className="active" href="#calendar">План прикорма</a>
        <a href="#sleep">Сон малыша</a>
      </nav>
        <button
          className="header-icon"
          type="button"
          title="Профиль"
        >
          <img src={profileIcon} alt="" />
        </button>
    </header>
  );
}