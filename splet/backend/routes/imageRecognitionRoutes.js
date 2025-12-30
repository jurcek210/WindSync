import express from "express";
import { pingImageApi } from "../controllers/imageRecognitionConstroler.js";

const router = express.Router();

// test endpoint
router.get("/ping", pingImageApi);

export default router;
