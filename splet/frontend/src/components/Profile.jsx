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
              <div key={wm._id} className="windmill-card">
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
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
