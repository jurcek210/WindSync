import dotenv from 'dotenv'
import express from 'express'
import cors from 'cors'
import connectDB from './config/db.js'
import cookieParser from "cookie-parser"
import authRoute from "./routes/authRoutes.js"
import windmillRoute from "./routes/windmillRoutes.js";


dotenv.config();
connectDB();

const app = express()
app.use(express.json())

app.get('/', (req, res) => {
    res.send("API WORKS")
});

app.use(
    cors({
      origin: ["http://localhost:5173"],
      methods: ["GET", "POST", "PUT", "DELETE"],
      credentials: true,
    })
  );
  app.use(cookieParser());
  
  app.use("/api", authRoute);

  app.use("/api/windmills", windmillRoute);

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`)
})


