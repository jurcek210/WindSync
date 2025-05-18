import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useEffect, useState } from "react";
import axios from "axios";
import L from "leaflet";

const Map = ({ loggedIn }) => {

  const [windmills, setWindmills] = useState([]);
  const [clickedLatLng, setClickedLatLng] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [name, setName] = useState("");
  const [windSpeed, setWindSpeed] = useState("");


  const windmillIcon = new L.Icon({
    iconUrl: "/photos/windmill.png",
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });

  const MapClickHandler = ({ onClick }) => {
    useMapEvents({
      click(e) {
        onClick(e.latlng);
      }
    });
    return null;
  };


  useEffect(() => {
    const fetchWindmills = async () => {
      try {
        const { data } = await axios.get("/api/windmills");
        console.log("Vsebina podatkov:", data);
        console.log(loggedIn)
        setWindmills(data);
      } catch (err) {
        console.error("Napaka pri pridobivanju veternic:", err);
      }
    };
    fetchWindmills();
  }, []);

  return (
    <div
      style={{
        display: "flex",
        justifyContent: "center",
        gap: "24px",
        width: "100%",
        maxWidth: "66%",
        margin: "40px",
      }}
    >
      <div
        style={{
          width: "1200px",
          height: "800px",
          borderRadius: "16px",
          overflow: "hidden",
          boxShadow: "0 8px 24px rgba(0,0,0,0.08)",
          border: "1px solid var(--border)",
        }}
      >


        <MapContainer
          center={[46.1512, 14.9955]}
          zoom={9}
          style={{ width: "100%", height: "100%" }}

          dragging={true}
        >
          <MapClickHandler onClick={(latlng) => {
            if (loggedIn) {
              setClickedLatLng(latlng);
            } else {
              alert("Prijavi se, da lahko dodaš veternico.");
            }
          }} />


          <TileLayer
            attribution='&copy; <a href="https://carto.com/">CARTO</a>'
            url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
          />
          {windmills.map((wm) => (
            <Marker
              key={wm._id}
              position={[
                wm.location.coordinates[1],
                wm.location.coordinates[0],
              ]}
              icon={windmillIcon}
            >
              <Popup>
                <strong>{wm.name}</strong>
                <br />
                Hitrost vetra: {wm.windSpeed ?? "ni podatka"} m/s
                <br />
                Status: {wm.status ? " Aktivna" : " Neaktivna"}
              </Popup>
            </Marker>
          ))}
          {clickedLatLng && (
            <Marker position={clickedLatLng}>
              <Popup>
                <button
                  onClick={() => setShowModal(true)}
                  style={{
                    backgroundColor: "#4caf50",
                    color: "white",
                    border: "none",
                    padding: "4px 10px",
                    borderRadius: "4px",
                    cursor: "pointer"
                  }}
                >
                  + Dodaj veternico
                </button>
              </Popup>
            </Marker>
          )}
        </MapContainer>
      </div>

      <div
        style={{
          flex: 1,
          height: "800px",
        }}
      ></div>
      {showModal && (
        <div style={{
          position: "fixed",
          top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: "rgba(0,0,0,0.4)",
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          zIndex: 999
        }}>
          <div style={{
            backgroundColor: "white",
            padding: "20px",
            borderRadius: "8px",
            width: "300px"
          }}>
            <h2>Dodaj veternico</h2>
            <input
              placeholder="Ime"
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{ width: "100%", marginBottom: "8px" }}
            />
            <input
              type="number"
              placeholder="Hitrost vetra"
              value={windSpeed}
              onChange={(e) => setWindSpeed(e.target.value)}
              style={{ width: "100%", marginBottom: "8px" }}
            />
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
              <button onClick={() => setShowModal(false)}>Prekliči</button>
              <button
                onClick={async () => {
                  try {
                    await axios.post("/api/windmills", {
                      name,
                      windSpeed: parseFloat(windSpeed),
                      location: {
                        type: "Point",
                        coordinates: [clickedLatLng.lng, clickedLatLng.lat]
                      }
                    }, {
                      headers: {
                        Authorization: `Bearer ${localStorage.getItem("token")}`
                      }
                    });

                    setShowModal(false);
                    setClickedLatLng(null);
                    setName("");
                    setWindSpeed("");
                    const { data } = await axios.get("/api/windmills");
                    setWindmills(data);
                  } catch (err) {
                    console.error("Napaka pri shranjevanju veternice", err);
                  }
                }}
                style={{ backgroundColor: "#4caf50", color: "white", padding: "4px 8px" }}
              >
                Shrani
              </button>
            </div>
          </div>
        </div>
      )}

    </div>


  );
};

export default Map;
