import React from 'react';
import '../styles/Taskbar.css';

const Taskbar = ({ loggedIn, setLoggedIn, user, setUser }) => {
  return (
    <header className="taskbar">
      <div className="taskbar-logo">
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
            <button
              onClick={async () => {
                await fetch("http://localhost:3001/api/logout", {
                  method: "POST",
                  credentials: "include",
                });
                setUser(null);
                setLoggedIn(false);
              }}
            >
              LOGOUT
            </button>
          </>
        )}
      </nav>
    </header>
  );
};

export default Taskbar;
