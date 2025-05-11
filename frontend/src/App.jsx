import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./components/Login";
import Register from "./components/Register";
import Home from "./components/Home";
import Taskbar from "./components/Taskbar";
import "./styles/index.css";
import { useEffect, useState } from "react";

function App() {
  const [user, setUser] = useState(null);
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    const checkAuth = async () => {
      try {
        const res = await fetch("http://localhost:3001/api/me", {
          credentials: "include",
        });
        const data = await res.json();
        if (data.status) {
          setUser(data.user);
          setLoggedIn(true);
        }
      } catch (err) {
        setUser(null);
        setLoggedIn(false);
      }
    };

    checkAuth();
  }, []);




  return (
    <Router>
      <Taskbar
        loggedIn={loggedIn}
        setLoggedIn={setLoggedIn}
        user={user}
        setUser={setUser}
      />
      <Routes>
        <Route path="/" element={<Home />} />
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
