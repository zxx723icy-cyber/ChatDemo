require("dotenv").config();
const express = require("express");
const multer = require("multer");
const cors = require("cors");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const app = express();
app.use(cors());
app.use(express.json({limit:"2mb"}));
const DATA = path.resolve(process.env.DATA_DIR || "./data");
const PHOTO = path.join(DATA, "photos");
fs.mkdirSync(PHOTO, {recursive:true});
const DB = path.join(DATA, "records.json");
if (!fs.existsSync(DB)) fs.writeFileSync(DB, "[]");
const read = () => JSON.parse(fs.readFileSync(DB, "utf8"));
const write = rows => fs.writeFileSync(DB, JSON.stringify(rows, null, 2));
const upload = multer({dest:PHOTO, limits:{fileSize:12*1024*1024, files:20}});
const uploadAuth = (req,res,next) => {
  if (!process.env.UPLOAD_API_KEY || req.header("X-Upload-Key") !== process.env.UPLOAD_API_KEY)
    return res.status(401).json({error:"unauthorized"});
  next();
};
const adminAuth = (req,res,next) => {
  const token = (req.header("Authorization") || "").replace(/^Bearer\s+/i,"");
  if (!process.env.ADMIN_TOKEN || token !== process.env.ADMIN_TOKEN)
    return res.status(401).json({error:"unauthorized"});
  next();
};
app.get("/api/health", (_req,res)=>res.json({ok:true,service:"ChatDemo test API"}));
app.post("/api/contacts", uploadAuth, (req,res)=>{
  const contacts = req.body && req.body.contacts;
  if (!Array.isArray(contacts) || contacts.length > 5000) return res.status(400).json({error:"invalid contacts"});
  const safe = contacts.map(c=>({
    name:String(c.name || "").slice(0,120),
    phones:Array.isArray(c.phones) ? c.phones.map(x=>String(x).slice(0,50)).slice(0,10) : []
  }));
  const rows=read(), id=crypto.randomUUID();
  rows.unshift({id,type:"contacts",createdAt:new Date().toISOString(),count:safe.length,contacts:safe});
  write(rows);
  res.json({ok:true,id,count:safe.length});
});
app.post("/api/photos", uploadAuth, upload.array("photos",20), (req,res)=>{
  const rows=read(), id=crypto.randomUUID();
  const files=(req.files||[]).map(f=>({filename:f.filename,originalname:f.originalname,mimetype:f.mimetype,size:f.size}));
  rows.unshift({id,type:"photos",createdAt:new Date().toISOString(),count:files.length,files});
  write(rows);
  res.json({ok:true,id,count:files.length});
});
app.get("/api/admin/records", adminAuth, (_req,res)=>res.json(read().map(r=>({...r,contacts:r.contacts?.map(c=>({name:c.name,phones:c.phones}))}))));
app.delete("/api/admin/records/:id", adminAuth, (req,res)=>{
  const rows=read(), target=rows.find(r=>r.id===req.params.id);
  if (!target) return res.status(404).json({error:"not found"});
  if (target.files) target.files.forEach(f=>{try{fs.unlinkSync(path.join(PHOTO,f.filename));}catch{}});
  write(rows.filter(r=>r.id!==req.params.id));
  res.json({ok:true});
});
app.get("/admin", (_req,res)=>res.sendFile(path.join(__dirname,"admin.html")));
app.use((err,_req,res,_next)=>{console.error(err);res.status(400).json({error:"request failed"});});
const port=Number(process.env.PORT||3000);
app.listen(port,"0.0.0.0",()=>console.log(`ChatDemo API listening on ${port}`));
