
import profileIcon from "../assets/icons/icon_profile.png";
import "./Header.css";

export default function Header({ currentPage, onNavigate }) {
  return (
    <header className="site-header">
      <a
        className="brand"
        href="#top"
        onClick={(e) => {
          e.preventDefault();
          onNavigate("calendar");
        }}
      >
        <span className="brand-mark">♡</span>

        <span>
          <strong>Шаг за шагом</strong>
          <small>Поддержка на каждом этапе</small>
        </span>
      </a>

      <nav className="top-nav">
        <a
          href="#calendar"
          className={currentPage === "calendar" ? "active" : ""}
          onClick={(e) => {
            e.preventDefault();
            onNavigate("calendar");
          }}
        >
          План прикорма
        </a>

        <a
          href="#personal"
          className={currentPage === "personal" ? "active" : ""}
          onClick={(e) => {
            e.preventDefault();
            onNavigate("personal-entry");
          }}
        >
          Персональный план
        </a>

        <a href="#sleep">
          Сон малыша
        </a>
      </nav>

      <button
        className={`header-icon ${
          currentPage === "profile" ? "active" : ""
        }`}
        type="button"
        title="Профиль малыша"
        aria-label="Профиль малыша"
        onClick={() => onNavigate("profile")}
      >
        <img src={profileIcon} alt="" />
      </button>
    </header>
  );
}
