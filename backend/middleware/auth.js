import jwt from "jsonwebtoken";
import dotenv from "dotenv";
import User from "../models/User.js";

dotenv.config();

export const userVerify = async (req, res) => {
  try {
    const token = req.cookies.token;
    if (!token) {
      return res.status(401).json({ status: false, message: "No token" });
    }

    const decoded = jwt.verify(token, process.env.TOKEN_KEY);
    const user = await User.findById(decoded.id);

    if (!user) {
      return res.status(401).json({ status: false, message: "User not found" });
    }

    return res.status(200).json({ status: true, user: user.username });
  } catch (err) {
    return res.status(401).json({ status: false, message: "Token invalid" });
  }
};
export const protect = async (req, res, next) => {
    const token = req.cookies.token;
    if (!token) return res.status(401).json({ message: "Ni prijavljen" });
  
    try {
      const decoded = jwt.verify(token, process.env.TOKEN_KEY);
      req.user = await User.findById(decoded.id).select("-password");
      next(); 
    } catch (err) {
      return res.status(401).json({ message: "Token neveljaven" });
    }
  };

  export const getMe = async (req, res) => {
  try {
    const token = req.cookies.token;
    if (!token) return res.status(401).json({ status: false, message: "Ni prijavljen" });

    const decoded = jwt.verify(token, process.env.TOKEN_KEY);
    const user = await User.findById(decoded.id).select("-password");

    if (!user) return res.status(404).json({ status: false, message: "Uporabnik ne obstaja" });

    res.status(200).json({ status: true, user });
  } catch (err) {
    res.status(401).json({ status: false, message: "Token neveljaven" });
  }
};