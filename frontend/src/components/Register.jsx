import { useState } from "react"
import {useNavigate} from "react-router-dom"
import "../styles/Auth.css"

const Register = () => {
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [username, setUsername] = useState("")
    const [message, setMessage] = useState("")
    const navigate= useNavigate()

    const handleRegister = async (e) => {
        e.preventDefault()

        try {
        const res = await fetch("http://localhost:3001/api/register", {
            method: "POST",
            headers: {
                "Content-Type":"application/json",
            },
            credentials: "include",
            body: JSON.stringify({username, email,password})
        }) 
        const data = await res.json() 
        setMessage(data.message) 

        if (data.success) {
            navigate("/login")
        }
    }catch (err) {
            setMessage("napaka pri registraciji")
        }
    }

      return (
    <div className="auth-container">
      <h2>Registracija</h2>
      <form onSubmit={handleRegister} className="auth-form">
        <input
          type="email"
          placeholder="E-pošta"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="text"
          placeholder="Uporabniško ime"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Geslo"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <button type="submit">Registriraj se</button>
      </form>
      {message && <p className="auth-message">{message}</p>}
    </div>
  );
};
export default Register;