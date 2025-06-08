import { useEffect, useState } from "react";
import axios from "axios";
import "../styles/Profile.css";

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

const turbinePriceMap = {
  tipA: 4000,
  tipB: 1500,
  tipC: 1500,
  tipY: 20000,
  tipZ: 40000,
  tipW: 12000,
};

function calculateStats(windmill) {
  const power = turbinePowerMap[windmill.windMillType] || 1;
  const price = turbinePriceMap[windmill.windMillType] || 1000;
  const costPerKWh = 0.11915;

  const created = new Date(windmill.createdAt);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  created.setHours(0, 0, 0, 0);

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
  const todayEarnings = todayEnergy * costPerKWh;
  const earnings = totalEnergy * costPerKWh;
  const remaining = Math.max(price - earnings, 0);
  const avgDaily = totalEnergy / (days || 1);
  const monthsLeft = remaining / (avgDaily * costPerKWh * 30);

  return {
    todayEnergy: todayEnergy.toFixed(2),
    todayEarnings: todayEarnings.toFixed(2),
    totalEnergy: totalEnergy.toFixed(1),
    totalEarnings: earnings.toFixed(2),
    monthsLeft: monthsLeft.toFixed(1),
  };
}

const Profile = () => {
  const [windmills, setWindmills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState(null);
  const [sortBy, setSortBy] = useState("date");
  const [windmillStats, setWindmillStats] = useState({});

  useEffect(() => {
    const fetchData = async () => {
      const token = localStorage.getItem("token");

      try {
        const userRes = await axios.get("http://localhost:3001/api/me", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        setUser(userRes.data.user);

        const wmRes = await axios.get(
          "http://localhost:3001/api/windmills/my",
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        setWindmills(wmRes.data);
        const statsObj = {};
        await Promise.all(
          wmRes.data.map(async (wm) => {
            const stats = await calculateStats(wm);
            statsObj[wm._id] = stats;
          })
        );
        setWindmillStats(statsObj);
      } catch (err) {
        console.error("Napaka pri nalaganju profila:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const toggleStatus = async (id, newStatus) => {
    const token = localStorage.getItem("token");
    try {
      await axios.put(
        `http://localhost:3001/api/windmills/${id}/toggle-status`,
        { status: newStatus },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      setWindmills((prev) =>
        prev.map((w) => (w._id === id ? { ...w, status: newStatus } : w))
      );
    } catch (err) {
      console.error("Napaka pri posodabljanju statusa:", err);
      alert("Napaka pri spremembi statusa veternice.");
    }
  };

  const deleteWindmill = async (id) => {
    const confirmed = window.confirm("Ali res želiš izbrisati veternico?");
    if (!confirmed) return;

    const token = localStorage.getItem("token");
    try {
      await axios.delete(`http://localhost:3001/api/windmills/${id}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      setWindmills((prev) => prev.filter((w) => w._id !== id));
    } catch (err) {
      console.error("Napaka pri brisanju veternice:", err);
      alert("Napaka pri brisanju veternice.");
    }
  };

  const totalStats = {
    totalEarnings: 0,
    totalEnergy: 0,
    totalInvestment: 0,
    totalDays: 0,
    count: 0,
  };

  windmills.forEach((wm) => {
    const stats = windmillStats[wm._id];
    if (!stats) return;

    const price = turbinePriceMap[wm.windMillType] || 1000;

    const created = new Date(wm.createdAt);
    const today = new Date();
    created.setHours(0, 0, 0, 0);
    today.setHours(0, 0, 0, 0);
    const days = Math.max(
      Math.floor((today - created) / (1000 * 60 * 60 * 24)),
      1
    );

    totalStats.totalEarnings += parseFloat(stats.totalEarnings);
    totalStats.totalEnergy += parseFloat(stats.totalEnergy);
    totalStats.totalInvestment += price;
    totalStats.totalDays += days;
    totalStats.count += 1;
  });

  const remainingInvestment = Math.max(
    totalStats.totalInvestment - totalStats.totalEarnings,
    0
  );
  const oldestDate = Math.min(...windmills.map((wm) => new Date(wm.createdAt)));
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const start = new Date(oldestDate);
  start.setHours(0, 0, 0, 0);
  const durationDays = Math.max(
    Math.floor((today - start) / (1000 * 60 * 60 * 24)),
    1
  );

  const avgDailyEarnings = totalStats.totalEarnings / durationDays;

  const monthsToPayback =
    avgDailyEarnings > 0
      ? (remainingInvestment / (avgDailyEarnings * 30)).toFixed(1)
      : "∞";

  const sortedWindmills = [...windmills].sort((a, b) => {
    const statA = calculateStats(a);
    const statB = calculateStats(b);

    switch (sortBy) {
      case "todayEnergy":
        return statB.todayEnergy - statA.todayEnergy;
      case "todayEnergy-asc":
        return statA.todayEnergy - statB.todayEnergy;
      case "todayEarnings":
        return statB.todayEarnings - statA.todayEarnings;
      case "todayEarnings-asc":
        return statA.todayEarnings - statB.todayEarnings;
      case "totalEarnings":
        return statB.totalEarnings - statA.totalEarnings;
      case "totalEarnings-asc":
        return statA.totalEarnings - statB.totalEarnings;
      case "date-asc":
        return new Date(a.createdAt) - new Date(b.createdAt);
      case "date":
      default:
        return new Date(b.createdAt) - new Date(a.createdAt);
    }
  });

  if (loading) return <p>Loading...</p>;

  return (
    <div className="profile-container">
      <div className="profile-header">
        <div className="profile-header">
          <div className="profile-info centered">
            <div className="profile-row-inline">
              <div className="profile-texts">
                <h2 className="profile-username">{user?.username}</h2>
                <p className="profile-email">{user?.email}</p>
              </div>{" "}
            </div>

            <div className="total-stats-box">
              <h3>Skupna statistika</h3>
              <div className="stat-grid">
                <div className="stat-block">
                  <span className="stat-label">Število veternic</span>
                  <span className="stat-value">{totalStats.count}</span>
                </div>
                <div className="stat-block">
                  <span className="stat-label">Skupna energija</span>
                  <span className="stat-value">
                    {totalStats.totalEnergy.toFixed(1)} kWh
                  </span>
                </div>
                <div className="stat-block">
                  <span className="stat-label">Skupni zaslužek</span>
                  <span className="stat-value">
                    {totalStats.totalEarnings.toFixed(2)} €
                  </span>
                </div>
                <div className="stat-block">
                  <span className="stat-label">Povračilo investicije</span>
                  <span className="stat-value">{monthsToPayback} mesecev</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="windmill-section">
        {windmills.length === 0 ? (
          <p>Nimaš še dodanih veternic.</p>
        ) : (
          <div className="windmill-grid">
            <div
              style={{
                display: "flex",
                justifyContent: "flex-end",
                marginBottom: "1rem",
              }}
            >
              <label
                htmlFor="sortBy"
                style={{ marginRight: "0.5rem", fontWeight: "bold" }}
              >
                Sortiraj po:
              </label>
              <select
                id="sortBy"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                style={{
                  padding: "6px 12px",
                  borderRadius: "6px",
                  border: "1px solid #ccc",
                  fontSize: "1rem",
                }}
              >
                <option value="date">Najnovejše postavljene</option>
                <option value="date-asc">Najstarejše postavljene</option>
                <option value="todayEnergy">Največja energija danes</option>
                <option value="todayEnergy-asc">
                  Najmanjša energija danes
                </option>
                <option value="todayEarnings">Največji zaslužek danes</option>
                <option value="todayEarnings-asc">
                  Najmanjši zaslužek danes
                </option>
                <option value="totalEarnings">Največji skupni zaslužek</option>
                <option value="totalEarnings-asc">
                  Najmanjši skupni zaslužek
                </option>
              </select>
            </div>
            {sortedWindmills.map((wm) => {
              const stats = windmillStats[wm._id];
              if (!stats) return null;
              const price = turbinePriceMap[wm.windMillType] || 1000;
              const imgSrc = images[wm.windMillType];

              return (
                <div className="windmill-card-container" key={wm._id}>
                  <div
                    className={`windmill-card ${
                      wm.status ? "active" : "inactive"
                    }`}
                  >
                    <div className="windmill-left">
                      <div className="windmill-card-header">
                        <h3 className="windmill-name">{wm.name}</h3>
                        <span
                          className={`status-label ${wm.status ? "on" : "off"}`}
                        >
                          {wm.status ? "Aktivna" : "Neaktivna"}
                        </span>
                      </div>

                      <div className="windmill-info-grid">
                        <div>
                          <span className="info-label">Datum postavitve </span>
                          <span className="info-value">
                            {new Date(wm.createdAt).toLocaleDateString()}
                          </span>
                        </div>
                        <div>
                          <span className="info-label">Investicija </span>
                          <span className="info-value">{price} €</span>
                        </div>
                      </div>

                      <hr className="divider" />

                      <div className="windmill-stats-grid">
                        <div>
                          <span className="info-label">Današnja energija </span>
                          <span className="info-value">
                            {stats.todayEnergy} kWh
                          </span>
                        </div>
                        <div>
                          <span className="info-label">Današnji zaslužek </span>
                          <span className="info-value">
                            {stats.todayEarnings} €
                          </span>
                        </div>
                        <div>
                          <span className="info-label">Skupna energija </span>
                          <span className="info-value">
                            {stats.totalEnergy} kWh
                          </span>
                        </div>
                        <div>
                          <span className="info-label">Skupni zaslužek </span>
                          <span className="info-value">
                            {stats.totalEarnings} €
                          </span>
                        </div>
                        <div>
                          <span className="info-label">Povračilo </span>
                          <span className="info-value">
                            {stats.monthsLeft} mesecev
                          </span>
                        </div>
                      </div>

                      <div className="windmill-actions">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleStatus(wm._id, !wm.status);
                          }}
                          className="action-button toggle"
                        >
                          {wm.status ? "Deaktiviraj" : "Aktiviraj"}
                        </button>

                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteWindmill(wm._id);
                          }}
                          className="action-button delete"
                        >
                          Izbriši
                        </button>
                      </div>
                    </div>

                    {imgSrc && (
                      <div className="windmill-image-container">
                        <img
                          src={imgSrc}
                          alt={`Veternica ${wm.windMillType}`}
                          className="windmill-image"
                        />
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
