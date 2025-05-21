import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./components/Login";
import Register from "./components/Register";
import Home from "./components/Home";
import Taskbar from "./components/Taskbar";
import "./styles/index.css";
import { useEffect, useState } from "react";
import axios from "axios";

function App() {
  const [user, setUser] = useState(null);
  const [loggedIn, setLoggedIn] = useState(false);
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    const checkAuth = async () => {
      const token = localStorage.getItem("token");
      if (!token) {
        setUser(null);
        setLoggedIn(false);
         setAuthChecked(true);
        return;
      }

      try {
        const res = await axios.get("http://localhost:3001/api/me", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        const data = res.data;
        if (data.status) {
          setUser(data.user);
          setLoggedIn(true);
        }
      } catch (err) {
        console.error("Auth error:", err);
        setUser(null);
        setLoggedIn(false);
      } finally {
        setAuthChecked(true);
      }
    };

    checkAuth();
  }, []);

   if (!authChecked) return null;




  return (
    <Router>
      <Taskbar
        loggedIn={loggedIn}
        setLoggedIn={setLoggedIn}
        user={user}
        setUser={setUser}
      />
      <Routes>
        <Route path="/" element={
          <Home user={user} loggedIn={loggedIn} />
        } />

        <Route
          path="/login"
          element={<Login setUser={setUser} setLoggedIn={setLoggedIn} />}
        />

        <Route path="/register" element={<Register />} />
      </Routes>
    </Router>
  );
}

export default App;
