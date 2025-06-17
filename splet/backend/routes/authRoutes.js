import express from "express";
import {
  Register,
  Login,
  Logout,
  getAllUsers,
} from "../controllers/authController.js";
import { userVerify, getMe } from "../middleware/auth.js";

const router = express.Router();

router.post("/register", Register);
router.post("/login", Login);
router.get("/verify", userVerify);
router.get("/me", getMe);
router.get("/users", getAllUsers);
router.post("/logout", Logout);

export default router;
