import React from "react";
import { useNavigate } from "react-router-dom";  
import "../styles/Taskbar.css";

const Taskbar = ({ loggedIn, setLoggedIn, user, setUser }) => {
  const navigate = useNavigate();

  return (
    <header className="taskbar">
      <div
        className="taskbar-logo"
        style={{ cursor: "pointer" }}
        onClick={() => navigate("/")}
      >
        <img src="/photos/logo.png" alt="WindSync logo" className="logo-icon" />
        <h1>WindSync</h1>
      </div>
      <nav className="taskbar-right">
        <a href="/">HOME</a>
        {!loggedIn ? (
          <>
            <a href="/login">LOGIN</a>
            <a href="/register">REGISTER</a>
          </>
        ) : (
          <>
            <a href="/profile">MOJ PROFIL</a>
            <button
              onClick={async () => {
                await fetch("http://localhost:3001/api/logout", { method: "POST" });
                localStorage.removeItem("token");
                setUser(null);
                setLoggedIn(false);
              }}
            >
              LOGOUT
            </button>
          </>
        )}
        <a href="/zanimivosti">ZANIMIVOSTI</a>
      </nav>
    </header>
  );
};

export default Taskbar;
