import { useEffect, useState } from "react";
import axios from "axios";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer
} from "recharts";

function Wind({ lat, lng }) {
  const [data, setData] = useState([]);

  useEffect(() => {
    if (!lat || !lng) return;

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
          const month = dates[i].slice(0, 7); // YYYY-MM
          if (!monthlyData[month]) {
            monthlyData[month] = [];
          }
          monthlyData[month].push(speeds[i]);
        }

        const monthlyAverages = Object.entries(monthlyData).map(([month, values]) => {
          const avg = values.reduce((a, b) => a + b, 0) / values.length;
          return { month, average: parseFloat(avg.toFixed(2)) };
        });

        setData(monthlyAverages);
      } catch (err) {
        console.error("Napaka pri pridobivanju podatkov za graf vetra:", err);
      }
    };

    fetchData();
  }, [lat, lng]);

  return (
    <div>
      <h3>Graf hitrosti vetra (mesečno)</h3>
      <div style={{ width: "100%", height: 200 }}>
        <ResponsiveContainer>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="month" />
            <YAxis unit=" m/s" />
            <Tooltip />
            <Line type="monotone" dataKey="average" stroke="#4caf50" />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

export default Wind;
