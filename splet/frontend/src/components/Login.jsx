import { useState } from "react"
import { useNavigate } from "react-router-dom"
import "../styles/Auth.css"

const Login = ({ setUser, setLoggedIn }) => {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [message, setMessage] = useState("")
  const navigate = useNavigate()

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch("http://localhost:3001/api/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({ email, password }),
      });

      const data = await res.json();
      setMessage(data.message);

      if (data.success) {
        setUser(data.user);
        setLoggedIn(true);
        navigate("/");
      }


    } catch (err) {
      setMessage("Napaka pri prijavi");
    }
  };
  return (
    <div className="auth-container">
      <h2>Prijava</h2>
      <form onSubmit={handleLogin} className="auth-form">
        <input
          type="email"
          placeholder="E-pošta"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Geslo"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <button type="submit">Prijava</button>
      </form>
      {message && <p className="auth-message">{message}</p>}
    </div>
  );
};

export default Login;