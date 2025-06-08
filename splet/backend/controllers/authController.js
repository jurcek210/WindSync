import User from "../models/User.js"
import {createSecretToken} from "../util/secretToken.js"
import bcrypt from "bcryptjs"

export const Register = async (req, res, next) => {
    try {
      const { email, password, username, createdAt } = req.body;
      const existingUser = await User.findOne({ email });
      if (existingUser) {
        return res.json({ message: "User already exists" });
      }
      const user = await User.create({ email, password, username, createdAt });
      const token = createSecretToken(user._id);
      res.cookie("token", token, {
        withCredentials: true,
        httpOnly: false,
      });
      res
        .status(201)
        .json({ message: "User signed in successfully", success: true, user });
      next();
    } catch (error) {
      console.error(error);
    }
  };

  export const Login = async(req, res, next) => {
    try {
      const {email, password} = req.body;
      if (!email || !password) {
        return res.json({message:"all fields required"})
      }
      const user = await User.findOne({email});
      if (!user) {
        return res.json({message: "wrong email!"})
      }
      const auth = await bcrypt.compare(password, user.password)
      if (!auth) {
        return res.json({message: "wrong password!"})
      }
      const token = createSecretToken(user._id);
      res.cookie("token", token, {
        withCredentials: true,
        httpOnly: false
      })
      res.status(201).json({message: "user logged in successfully", success: true})
      next()
    }catch (error) {
      console.error(error)
    }

  }