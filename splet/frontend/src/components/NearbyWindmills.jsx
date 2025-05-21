import { useState } from "react";
import axios from "axios";

const NearbyWindmills = () => {
  const [windmills, setWindmills] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchNearby = () => {
    setLoading(true);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lon = pos.coords.longitude;

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
    <div className="p-4">
      <button onClick={fetchNearby} className="bg-blue-600 text-white px-4 py-2 rounded">
        Get Nearby Windmills
      </button>

      {loading && <p>Loading...</p>}

      <ul className="mt-4">
        {windmills.map((wm) => (
          <li key={wm._id} className="border-b py-2">
            <strong>{wm.name}</strong> – Wind: {wm.windSpeed ?? "?"} m/s
          </li>
        ))}
      </ul>
    </div>
  );
};

export default NearbyWindmills;
