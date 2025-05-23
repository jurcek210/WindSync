import { useEffect, useState } from "react";
import axios from "axios";
import {
  LineChart, Line, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer
} from "recharts";

function WindEnergyGraph({ lat, lng, powerKW }) {
  const [data, setData] = useState([]);

  useEffect(() => {
    if (!lat || !lng || !powerKW) return;

    const fetchData = async () => {
      const today = new Date();
      const oneYearAgo = new Date();
      oneYearAgo.setFullYear(today.getFullYear() - 1);

      const formatDate = (d) => d.toISOString().split("T")[0];
      const start = formatDate(oneYearAgo);
      const end = formatDate(today);

      const url = `https://archive-api.open-meteo.com/v1/archive?latitude=${lat}&longitude=${lng}&start_date=${start}&end_date=${end}&daily=windspeed_10m_mean&timezone=auto`;

      try {
        const res = await axios.get(url);
        const speeds = res.data?.daily?.windspeed_10m_mean || [];
        const dates = res.data?.daily?.time || [];

        const monthlyData = {};

        for (let i = 0; i < dates.length; i++) {
          const month = dates[i].slice(0, 7);
          if (!monthlyData[month]) monthlyData[month] = [];
          monthlyData[month].push(speeds[i]);
        }

        const monthlyEnergy = Object.entries(monthlyData).map(([month, values]) => {
          const avgSpeed = values.reduce((a, b) => a + b, 0) / values.length;

          let efficiencyFactor = 0;
          if (avgSpeed >= 3 && avgSpeed <= 12) {
            efficiencyFactor = (avgSpeed - 3) / (12 - 3);
          } else if (avgSpeed > 12) {
            efficiencyFactor = 1;
          }

          const energy = powerKW * 24 * values.length * efficiencyFactor;

          return { month, energy: parseFloat(energy.toFixed(1)) };
        });

        setData(monthlyEnergy);
      } catch (err) {
        console.error("Napaka pri pridobivanju podatkov za graf:", err);
      }
    };

    fetchData();
  }, [lat, lng, powerKW]);

  return (
    <div>
      <h3>Graf mesečne proizvodnje energije</h3>
      <div style={{ width: "50%", height: 250 }}>
        <ResponsiveContainer>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="month" />
            <YAxis unit=" kWh" />
            <Tooltip />
            <Line type="monotone" dataKey="energy" stroke="#007acc" />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

export default WindEnergyGraph;
