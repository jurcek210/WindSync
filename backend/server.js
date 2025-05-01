import dotenv from 'dotenv'
import express from 'express'
import cors from 'cors'
import connectDB from './config/db.js'

dotenv.config();
connectDB();

const app = express()
app.use(cors())
app.use(express.json())

app.get('/', (req, res) => {
    res.send("API WORKS")
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`)
})