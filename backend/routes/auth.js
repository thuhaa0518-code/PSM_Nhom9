
const express = require('express');
const bcrypt  = require('bcryptjs');
const jwt     = require('jsonwebtoken');
const db      = require('../db');

const router = express.Router();

// POST /api/auth/register
router.post('/register', async (req, res) => {
  try {
    const { fullName, email, password, studentId } = req.body;
    if (!fullName || !email || !password) {
      return res.status(400).json({ message: 'Thiếu thông tin bắt buộc' });
    }

    const [existing] = await db.query('SELECT id FROM users WHERE email = ?', [email]);
    if (existing.length > 0) {
      return res.status(400).json({ message: 'Email đã được đăng ký' });
    }

    const hashedPw = await bcrypt.hash(password, 10);
    const [result] = await db.query(
      'INSERT INTO users (fullName, email, password, studentId) VALUES (?, ?, ?, ?)',
      [fullName, email, hashedPw, studentId || null]
    );

    res.json({ message: 'Đăng ký thành công', userId: result.insertId });
  } catch (err) {
    console.error('[REGISTER ERROR]', err.message, err.code);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
  try {
    const { input, password } = req.body;
    if (!input || !password) {
      return res.status(400).json({ message: 'Thiếu thông tin đăng nhập' });
    }

    const isEmail = input.includes('@');
    const [rows] = await db.query(
      isEmail
        ? 'SELECT * FROM users WHERE email = ?'
        : 'SELECT * FROM users WHERE studentId = ?',
      [input]
    );

    if (rows.length === 0) {
      return res.status(401).json({ message: 'Sai thông tin đăng nhập' });
    }

    const user = rows[0];
    const match = await bcrypt.compare(password, user.password);
    if (!match) {
      return res.status(401).json({ message: 'Sai thông tin đăng nhập' });
    }

    const token = jwt.sign({ userId: user.id }, process.env.JWT_SECRET, { expiresIn: '30d' });
    res.json({
      token,
      user: {
        id: user.id,
        fullName: user.fullName,
        email: user.email,
        studentId: user.studentId
      }
    });
  } catch (err) {
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

module.exports = router;
