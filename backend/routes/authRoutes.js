import express from "express";
import { Register, Login } from "../controllers/authController.js"; // ✅ named import
import { userVerify } from "../middleware/auth.js";

const router = express.Router();

router.post("/register", Register);
router.post("/login", Login);
router.get("/verify", userVerify)

export default router;