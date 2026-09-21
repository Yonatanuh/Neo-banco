import React from "react";
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

const COLORS = [
  "#0d9488",
  "#2563eb",
  "#9333ea",
  "#e11d48",
  "#ea580c",
  "#ca8a04",
];

const GastosChart = ({ gastosPorCategoria }) => {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie
          data={gastosPorCategoria}
          cx="50%"
          cy="50%"
          innerRadius={60}
          outerRadius={80}
          fill="#8884d8"
          paddingAngle={5}
          dataKey="value"
        >
          {gastosPorCategoria.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{
            backgroundColor: "#0f172a",
            borderColor: "#334155",
            borderRadius: "0.5rem",
          }}
          itemStyle={{ color: "#e2e8f0" }}
        />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
};

export default GastosChart;
