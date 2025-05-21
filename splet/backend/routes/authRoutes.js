import express from "express";
import { Register, Login, Logout } from "../controllers/authController.js"; // ✅ named import
import { userVerify, getMe } from "../middleware/auth.js";

const router = express.Router();

router.post("/register", Register);
router.post("/login", Login);
router.get("/verify", userVerify)
router.get("/me", getMe);
router.post("/logout", Logout)

export default router;