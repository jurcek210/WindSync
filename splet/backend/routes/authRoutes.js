import express from "express";
<<<<<<< HEAD
import { Register, Login } from "../controllers/authController.js"; // ✅ named import
import { userVerify } from "../middleware/auth.js";
=======
import { Register, Login, Logout, getAllUsers } from "../controllers/authController.js"; 
import { userVerify, getMe } from "../middleware/auth.js";
>>>>>>> dev

const router = express.Router();

router.post("/register", Register);
router.post("/login", Login);
router.get("/verify", userVerify)
<<<<<<< HEAD
=======
router.get("/me", getMe);
router.get("/users", getAllUsers);
router.post("/logout", Logout)
>>>>>>> dev

export default router;