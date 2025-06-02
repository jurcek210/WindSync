import { useEffect, useState } from "react";
import axios from "axios";
import '../styles/WindMills.css';
import {
  LineChart, Line, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer
} from "recharts";
import "../styles/WindMills.css"; // Stil za futuristični videz

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
    <div className="graph-container">
      <h3 className="graph-title">Mesečna proizvodnja energije</h3>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data}>
          <CartesianGrid stroke="rgba(255, 255, 255, 0.1)" />
          <XAxis dataKey="month" stroke="#aaa" />
          <YAxis unit=" kWh" stroke="#aaa" />
          <Tooltip
            contentStyle={{
              backgroundColor: "rgba(0, 0, 0, 0.7)",
              border: "none",
              borderRadius: "8px",
              color: "#00fff7",
            }}
            labelStyle={{ color: "#fff" }}
          />
          <Line
            type="monotone"
            dataKey="energy"
            stroke="rgba(0, 255, 247, 0.5)"  // svetlejša in bolj prosojna turkizna
            strokeWidth={2}                // manjša debelina linije
            dot={{ r: 3, stroke: "rgba(0, 255, 247, 0.5)", strokeWidth: 1, fill: "#001f3f" }}
            activeDot={{ r: 5, fill: "rgba(0, 255, 247, 0.7)" }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export default WindEnergyGraph;
