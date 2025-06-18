import { useEffect, useState } from "react";
import axios from "axios";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend
} from "recharts";

function Wind({ lat, lng }) {
  const [lineData, setLineData] = useState([]);
  const [pieData, setPieData] = useState([]);

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
          const month = dates[i].slice(0, 7);
          if (!monthlyData[month]) {
            monthlyData[month] = [];
          }
          monthlyData[month].push(speeds[i]);
        }

        const monthlyAverages = [];
        const monthlySums = [];

        Object.entries(monthlyData).forEach(([month, values]) => {
          const sum = values.reduce((a, b) => a + b, 0);
          const avg = sum / values.length;
          monthlyAverages.push({ month, average: parseFloat(avg.toFixed(2)) });
          monthlySums.push({ name: month.slice(5), value: parseFloat(sum.toFixed(2)) }); // ime meseca za tortni diagram
        });

        const currentMonth = today.toISOString().slice(0, 7);

        const filteredAverages = monthlyAverages.filter(({ month }) => month !== currentMonth);
        const filteredSums = monthlySums.filter(({ name }) => name !== currentMonth.slice(5));

        setLineData(filteredAverages);
        setPieData(filteredSums);
      } catch (err) {
        console.error("Napaka pri pridobivanju podatkov za graf vetra:", err);
      }
    };

    fetchData();
  }, [lat, lng]);

  const COLORS = [
    "#0088FE", "#00C49F", "#FFBB28", "#FF8042",
    "#a4de6c", "#d0ed57", "#8dd1e1", "#83a6ed",
    "#8884d8", "#82ca9d", "#ffc658", "#d8854f"
  ];

  return (
    <div
      style={{
        padding: "12px 0",
        background: "linear-gradient(135deg, #f0f0f0, #ffffff)",
        borderRadius: "12px",
        boxShadow: "0 0 15px rgba(0, 200, 255, 0.2)",
        color: "#007bff",
      }}
    >
      <h3
        style={{
          marginBottom: 12,
          fontWeight: "600",
          color: "#007bff",
          textShadow: "0 0 6px rgba(0, 123, 255, 0.3)"
        }}
      >
        🌬️ Graf hitrosti vetra (mesečno)
      </h3>
      <div style={{ width: "100%", height: 250 }}>
        <ResponsiveContainer>
          <LineChart data={lineData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
            <CartesianGrid stroke="#00c8ff44" strokeDasharray="4 4" />
            <XAxis
              dataKey="month"
              tick={{ fill: "#007bff", fontSize: 12 }}
              tickFormatter={(tick) => tick.slice(5)}
            />
            <YAxis
              unit=" m/s"
              tick={{ fill: "#007bff", fontSize: 12 }}
              domain={['auto', 'auto']}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: "#ffffffcc",
                borderRadius: 6,
                borderColor: "#00c8ff88",
                color: "#007bff"
              }}
              labelStyle={{ color: "#007bff", fontWeight: "600" }}
              formatter={(value) => `${value} m/s`}
            />
            <Line
              type="monotone"
              dataKey="average"
              stroke="#bbdefb"
              strokeWidth={3}
              dot={{
                r: 4,
                stroke: "#00c8ff",
                strokeWidth: 2,
                fill: "#ffffff"
              }}
              activeDot={{
                r: 6,
                fill: "#00c8ff",
                stroke: "#00c8ff",
                strokeWidth: 2
              }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <h3
        style={{
          marginTop: 24,
          marginBottom: 12,
          fontWeight: "600",
          color: "#007bff",
          textShadow: "0 0 6px rgba(0, 123, 255, 0.3)"
        }}
      >
        🌪️ Skupna moč vetra po mesecih
      </h3>
      <div style={{ width: "100%", height: 300 }}>
        <ResponsiveContainer>
          <PieChart>
            <Pie
              data={pieData}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="50%"
              outerRadius={100}
              fill="#8884d8"
              label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}
            >
              {pieData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip
              formatter={(value) => `${value.toFixed(2)} m/s`}
              contentStyle={{
                backgroundColor: "#ffffffcc",
                borderRadius: 6,
                borderColor: "#00c8ff88",
                color: "#007bff"
              }}
              labelStyle={{ color: "#007bff", fontWeight: "600" }}
            />
            <Legend verticalAlign="bottom" height={36} />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

export default Wind;
