import React from 'react';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend, CartesianGrid
} from 'recharts';
import '../styles/zanimivosti.css';

const podatki = [
  { drzava: 'Danska', procent: 55 },
  { drzava: 'Irska', procent: 39 },
  { drzava: 'Nemčija', procent: 29 },
  { drzava: 'Portugalska', procent: 28 },
  { drzava: 'Španija', procent: 23 },
  { drzava: 'Grčija', procent: 21 },
  { drzava: 'Litva', procent: 20 },
  { drzava: 'Švedska', procent: 18 },
  { drzava: 'Nizozemska', procent: 17 },
  { drzava: 'Finska', procent: 16 },
  { drzava: 'Francija', procent: 12 },
  { drzava: 'Italija', procent: 11 },
  { drzava: 'Belgija', procent: 10 },
  { drzava: 'Avstrija', procent: 8 },
  { drzava: 'Madžarska', procent: 2 },
  { drzava: 'Slovenija', procent: 0.05 }
];

const Zanimivosti = () => {
  return (
    <div className="zanimivosti-page">
      <div className="overlay">
        <h2>🌬️ Uporaba vetrne energije v EU državah</h2>
        <p>
          Primerjava deleža električne energije iz vetra med izbranimi državami EU in Slovenijo. 
          Podatki kažejo velik razkorak med državami – Danska je vodilna z več kot 50 %, Slovenija pa na repu lestvice.
        </p>

        <ResponsiveContainer width="100%" height={500}>
          <BarChart
            data={podatki}
            layout="vertical"
            margin={{ top: 20, right: 30, left: 100, bottom: 20 }}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="var(--primary-dark)" />
            <XAxis
              type="number"
              domain={[0, 60]}
              tickFormatter={(val) => `${val}%`}
              stroke={ 'var(--white)' }
              tick={{ fill: 'var(--white)' }}
            />
            <YAxis
              type="category"
              dataKey="drzava"
              tick={{ fontSize: 14, fontWeight: 'bold', fill: 'var(--white)' }}
              stroke={'var(--white)'}
            />
            <Tooltip formatter={(value) => `${value}%`} />
            <Legend wrapperStyle={{ color: 'var(--white)' }} />
            <Bar dataKey="procent" fill="var(--primary-color)" name="Delež (%)" />
          </BarChart>
        </ResponsiveContainer>

        <div className="info-box">
          <h3>📊 Ste vedeli?</h3>
          <ul>
            <li>💡 Povprečen delež vetrne energije v EU je okoli <strong>16%</strong>.</li>
            <li>🔌 Slovenija pridobi le <strong>0.05%</strong> elektrike iz vetra – eden najnižjih deležev v Evropi.</li>
            <li>🌍 Danska proizvede več kot polovico elektrike z vetrnimi elektrarnami!</li>
            <li>🌄 V Sloveniji je manj vetrnih elektrarn zaradi geografske lege, hribovitega terena in strožjih okoljskih ter prostorskih omejitev.</li>
          </ul>
        </div>

      </div>
    </div>
  );
};

export default Zanimivosti;
