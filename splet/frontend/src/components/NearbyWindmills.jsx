import { useState } from "react";
import axios from "axios";
import "../styles/NearbyWindmills.css";

const NearbyWindmills = () => {
  const [windmills, setWindmills] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showLoadingBox, setShowLoadingBox] = useState(false);
  const [userCoords, setUserCoords] = useState(null);

  const fetchNearby = () => {
    setLoading(true);
    setShowLoadingBox(true);

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lon = pos.coords.longitude;

        setUserCoords({ lat, lon }); // Shrani koordinate

        try {
          const res = await axios.get(
            `/api/windmills/nearby?lat=${lat}&lon=${lon}&maxDistance=25000`
          );
          setWindmills(res.data);
        } catch (err) {
          console.error(err);
        } finally {
          setLoading(false);
        }
      },
      (err) => {
        console.error("Location error:", err);
        setLoading(false);
      }
    );
  };

  return (
    <div className="p-4 relative">
      <button
        onClick={fetchNearby}
        className={`image-button ${loading ? "loading" : ""}`}
      >
        <img
          src="../../public/photos/radar.png"
          alt="Search nearby windmills"
          className="w-16 h-16"
        />
      </button>

      {showLoadingBox && (
        <div className="loading-box">
          <p>🔍 <strong>Iskanje v teku...</strong></p>
          {userCoords && (
            <p>
              📍 Lokacija: {userCoords.lat.toFixed(3)}, {userCoords.lon.toFixed(3)}
            </p>
          )}
          <p>💨 Najdenih vetrnic: {windmills.length}</p>

          <ul className="mt-4">
            {windmills.map((wm) => (
              <li key={wm._id} className="border-b py-2">
                <strong>{wm.name}</strong> – Wind: {wm.windSpeed ?? "?"} m/s
              </li>
            ))}
          </ul>

          <button
            onClick={() => setShowLoadingBox(false)}
            className="close-btn"
          >
            Zapri
          </button>
        </div>
      )}
    </div>
  );
};

export default NearbyWindmills;
