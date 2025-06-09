import { useEffect, useState } from "react";
import Wind from "./Wind";
import "../styles/WindmillSidebar.css";

const images = {
  tipY: "/photos/image1.jpg",
  tipZ: "/photos/image2.jpg",
  tipW: "/photos/image3.jpg",
  tipA: "/photos/Slika1.jpg",
  tipB: "/photos/Slika2.jpg",
  tipC: "/photos/Slika3.jpg",
};

const turbinePowerMap = {
  tipA: 1.2,
  tipB: 2.0,
  tipC: 1.22,
  tipY: 6.0,
  tipZ: 15.0,
  tipW: 3.5,
};

function calculateStats(windmill) {
  const power = turbinePowerMap[windmill.windMillType] || 1;
  const created = new Date(windmill.createdAt);
  const today = new Date();
  created.setHours(0, 0, 0, 0);
  today.setHours(0, 0, 0, 0);

  const days = Math.floor((today - created) / (1000 * 60 * 60 * 24));
  let totalEnergy = 0;

  for (let i = 0; i <= days; i++) {
    const speed = windmill.windSpeed;
    const eff = speed < 3 ? 0 : speed <= 12 ? (speed - 3) / 9 : 1;
    const daily = power * 24 * eff;
    totalEnergy += daily;
  }

  const todayEff =
    windmill.windSpeed < 3
      ? 0
      : windmill.windSpeed <= 12
      ? (windmill.windSpeed - 3) / 9
      : 1;

  const todayEnergy = power * 24 * todayEff;

  return {
    todayEnergy: todayEnergy.toFixed(2),
    totalEnergy: totalEnergy.toFixed(1),
  };
}

const WindmillDetailsSidebar = ({ windmill, onClose }) => {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    if (windmill) {
      const s = calculateStats(windmill);
      setStats(s);
    }
  }, [windmill]);

  const imageSrc = windmill?.windMillType
    ? images[windmill.windMillType]
    : null;

  return (
    <div className="windmill-sidebar">
      <button className="close-button" onClick={onClose}>
        ×
      </button>

      <h2 className="sidebar-title">{windmill.name}</h2>

      <span className={`status-tag ${windmill.status ? "on" : "off"}`}>
        {windmill.status ? "Aktivna" : "Neaktivna"}
      </span>

      {imageSrc && (
        <img src={imageSrc} alt="Veternica" className="sidebar-image" />
      )}

      <div className="data-cards">
        <div className="data-card">
          <span className="data-label">Hitrost vetra</span>
          <span className="data-value">{windmill.windSpeed} m/s</span>
        </div>
        <div className="data-card">
          <span className="data-label">Danes energija</span>
          <span className="data-value">{stats?.todayEnergy || "-"} kWh</span>
        </div>
        <div className="data-card">
          <span className="data-label">Skupna energija</span>
          <span className="data-value">{stats?.totalEnergy || "-"} kWh</span>
        </div>
      </div>

      {windmill.location?.coordinates && (
        <div className="wind-graph-container">
          <h3 style={{ marginBottom: "12px", color: "#0078d7" }}>
            Zgodovinski podatki o vetru
          </h3>
          <Wind
            lat={windmill.location.coordinates[1]}
            lng={windmill.location.coordinates[0]}
          />
        </div>
      )}
    </div>
  );
};

export default WindmillDetailsSidebar;
