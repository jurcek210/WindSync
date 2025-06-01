import { useEffect, useState } from "react";
import axios from "axios";
import "../styles/Profile.css";

const Profile = () => {
  const [windmills, setWindmills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState(null);

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

        const wmRes = await axios.get("http://localhost:3001/api/windmills/my", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        setWindmills(wmRes.data);
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
        prev.map((w) =>
          w._id === id ? { ...w, status: newStatus } : w
        )
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

  if (loading) return <p>Loading...</p>;

  return (
    <div className="profile-container">
      <div className="profile-header">
        <div className="profile-info">
          <h2 className="profile-title">Profil</h2>
          <div className="profile-row">
            <span className="profile-label">Username:</span>
            <span className="profile-value">{user?.username}</span>
          </div>
          <div className="profile-row">
            <span className="profile-label">Email:</span>
            <span className="profile-value">{user?.email}</span>
          </div>
        </div>
      </div>

      <div className="windmill-section">
        <h3>Moje veternice</h3>
        {windmills.length === 0 ? (
          <p>Nimaš še dodanih veternic.</p>
        ) : (
            <div className="windmill-grid">
            {windmills.map((wm) => (
              <div key={wm._id} className={`windmill-card ${wm.status ? "active" : "inactive"}`}>
                <h4>{wm.name}</h4>
                <p>
                  <strong>Lokacija:</strong> {wm.location.coordinates[1].toFixed(4)},{" "}
                  {wm.location.coordinates[0].toFixed(4)}
                </p>
                <p>
                  <strong>Hitrost vetra:</strong>{" "}
                  {wm.windSpeed != null ? `${wm.windSpeed.toFixed(2)} m/s` : "ni podatka"}
                </p>
                <p>
                  <strong>Status:</strong> {wm.status ? "Aktivna" : "Neaktivna"}
                </p>
                <div className="windmill-actions">
                <button
                  onClick={() => toggleStatus(wm._id, !wm.status)}
                  style={{
                    marginTop: "8px",
                    padding: "6px 12px",
                    backgroundColor: wm.status ? "#ef4444" : "#4caf50",
                    color: "white",
                    border: "none",
                    borderRadius: "6px",
                    cursor: "pointer",
                  }}
                >
                  {wm.status ? "Deaktiviraj" : "Aktiviraj"}
                </button>
                <button
                  onClick={() => deleteWindmill(wm._id)}
                  style={{
                    marginTop: "8px",
                    padding: "6px 12px",
                    backgroundColor: "#d32f2f",
                    color: "white",
                    border: "none",
                    borderRadius: "6px",
                    cursor: "pointer",
                  }}
                >
                  Izbriši
                </button>
                </div>
              </div>
              
            ))}
              </div>
        
        )}
      </div>
    </div>
  );
};

export default Profile;
