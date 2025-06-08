import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./components/Login";
import Register from "./components/Register"
import Home from "./components/Home"
import "./styles/index.css"

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/home" element ={<Home/>}/>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      </Routes>
    </Router>
  );
}

export default App;