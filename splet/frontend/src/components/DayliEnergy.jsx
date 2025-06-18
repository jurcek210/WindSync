import { useState } from "react";

function DayliEnergy({ windmill }) {
  const [energy, setEnergy] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [weatherDesc, setWeatherDesc] = useState("");
  const [temperature, setTemperature] = useState(null);
  const [windSpeed, setWindSpeed] = useState(null);
  const [windDirection, setWindDirection] = useState(null);

  const handleClick = async () => {
    if (energy || error) {
      setEnergy(null);
      setError(null);
      setWeatherDesc("");
      setTemperature(null);
      setWindSpeed(null);
      setWindDirection(null);
      return;
    }

    setLoading(true);
    setError(null);

    const { coordinates } = windmill.location;
    const [lng, lat] = coordinates;

    const API_KEY = "7f24a8b33318df879eda6cd00f4f5716";
    const url = `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lng}&appid=${API_KEY}&units=metric`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error("Napaka pri pridobivanju podatkov");
      }
      const data = await response.json();

      const windSpeedData = data.wind?.speed ?? 0;
      const windDirData = data.wind?.deg ?? null;
      const tempData = data.main?.temp ?? null;
      const desc = data.weather && data.weather.length > 0 ? data.weather[0].description : "";

      const energyToday = (windSpeedData * 5).toFixed(2);

      setEnergy(energyToday);
      setWeatherDesc(desc);
      setTemperature(tempData);
      setWindSpeed(windSpeedData);
      setWindDirection(windDirData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const getWeatherIcon = () => {
    const desc = weatherDesc.toLowerCase();
    if (desc.includes("sun") || desc.includes("clear")) {
      return "☀️";
    } else if (desc.includes("cloud")) {
      return "☁️";
    } else if (desc.includes("rain") || desc.includes("drizzle")) {
      return "🌧️";
    } else if (desc.includes("mist") || desc.includes("fog") || desc.includes("haze")) {
      return "🌫️";
    } else {
      return "🌈";
    }
  };

  // Funkcija za določanje puščice glede na kot
  const getWindDirectionArrow = (deg) => {
    if (deg === null) return "";
    if (deg >= 337.5 || deg < 22.5) return "⬆️";
    if (deg >= 22.5 && deg < 67.5) return "↗️";
    if (deg >= 67.5 && deg < 112.5) return "➡️";
    if (deg >= 112.5 && deg < 157.5) return "↘️";
    if (deg >= 157.5 && deg < 202.5) return "⬇️";
    if (deg >= 202.5 && deg < 247.5) return "↙️";
    if (deg >= 247.5 && deg < 292.5) return "⬅️";
    if (deg >= 292.5 && deg < 337.5) return "↖️";
    return "";
  };

  return (
    <div style={{ marginTop: "8px" }}>
      <button
        onClick={handleClick}
        disabled={loading}
        style={{
          padding: "8px 16px",
          background: "#4caf50",
          color: "#fff",
          border: "none",
          borderRadius: "4px",
          cursor: loading ? "not-allowed" : "pointer",
          fontWeight: "600",
          fontSize: "14px",
          transition: "all 0.3s ease",
        }}
      >
        {loading
          ? "Pridobivanje podatkov..."
          : energy || error
          ? "Skrij energijo in vreme"
          : "Prikaži energijo za danes"}
      </button>

      {(energy || error) && (
        <div
          style={{
            marginTop: "12px",
            fontWeight: "bold",
            backgroundColor: "#f0f0f0",
            borderRadius: "8px",
            padding: "8px 12px",
            color: "#333",
          }}
        >
          {energy && (
            <>
              <p>
                Energija danes: ⚡ {energy} kWh
              </p>
              <p>
                Vreme: {getWeatherIcon()}{" "}
                {weatherDesc.charAt(0).toUpperCase() + weatherDesc.slice(1)}
              </p>
              <p>Temperatura: {temperature} °C</p>
              <p>
                Hitrost vetra: {windSpeed} m/s{" "}
                {windDirection !== null && (
                  <span>{getWindDirectionArrow(windDirection)}</span>
                )}
              </p>
            </>
          )}
          {error && <p style={{ color: "red" }}>Napaka: {error}</p>}
        </div>
      )}
    </div>
  );
}

export default DayliEnergy;
