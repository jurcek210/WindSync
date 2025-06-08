import { useState } from "react"
import {useNavigate} from "react-router-dom"

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
        <div>
        <h2>Registracija</h2>
        <form onSubmit={handleRegister}>
        <input
            type="text"
            placeholder="uporabnisko ime"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
          <input
            type="email"
            placeholder="E-pošta"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          /><br />
          <input
            type="password"
            placeholder="Geslo"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          /><br />
          <button type="submit">Registracija</button>
        </form>
        {message && <p>{message}</p>}
      </div>
    )
}
export default Register;