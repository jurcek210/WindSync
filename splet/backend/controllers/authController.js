import User from "../models/User.js";
import { createSecretToken } from "../util/secretToken.js";
import bcrypt from "bcrypt";

export const Register = async (req, res) => {
  try {
    const { email, password, username } = req.body;
    const createdAt = new Date();
    const existingUser = await User.findOne({ email });
    if (existingUser) {
      return res.status(409).json({ message: "User already exists" });
    }

    const user = await User.create({
      email,
      password,
      username,
      createdAt,
    });

    const token = createSecretToken(user._id);
    res.cookie("token", token, {
      httpOnly: true,
      sameSite: "Strict",
      secure: process.env.NODE_ENV === "production",
    });

    res.status(201).json({
      token,
      message: "User registered successfully",
      success: true,
      user: { username: user.username, email: user.email },
      
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: "Server error" });
  }
};
export const getAllUsers = async (req, res) => {
  try {
    const users = await User.find({}, { username: 1, email: 1, _id: 0 }); 
    res.status(200).json(users);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: "Server error" });
  }
};


export const Login = async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password)
      return res.status(400).json({ message: "All fields required" });
    
    const user = await User.findOne({ email });
    if (user) {
  console.log("Vnešeno geslo:", password);
  console.log("Shranjeno geslo:", user.password);
  const auth = await bcrypt.compare(password, user.password);
  console.log("Rezultat primerjave:", auth);
}

    if (!user)
      return res.status(401).json({ message: "Wrong email or password" });

    const auth = await bcrypt.compare(password, user.password);
    if (!auth)
      return res.status(401).json({ message: "Wrong email or password" });

    const token = createSecretToken(user._id);
    res.cookie("token", token, {
      httpOnly: true,
      sameSite: "Strict",
      secure: process.env.NODE_ENV === "production",
    });
    console.log("PRIJAVA za:", email);
console.log("Najden uporabnik:", user);


    res.status(200).json({
      token,
      message: "User logged in successfully",
      success: true,
      user: { username: user.username, email: user.email },
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: "Server error" });
  }
};

export const Logout = (req, res) => {
  res.clearCookie("token", {
    httpOnly: true,
    sameSite: "Strict",
    secure: process.env.NODE_ENV === "production",
  });
  res.status(200).json({ message: "User signed out successfully" });
};
