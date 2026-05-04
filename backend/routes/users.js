const express = require('express');
const db      = require('../db');
const auth    = require('../middleware/auth');

const router = express.Router();
router.use(auth);

// GET /api/users/me
router.get('/me', async (req, res) => {
  try {
    const [rows] = await db.query(
      'SELECT id, fullName, email, studentId, phone, birthDate FROM users WHERE id = ?',
      [req.user.userId]
    );
    if (rows.length === 0) return res.status(404).json({ message: 'Không tìm thấy user' });
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

// PUT /api/users/me
router.put('/me', async (req, res) => {
  try {
    const { fullName, phone, birthDate } = req.body;
    await db.query(
      'UPDATE users SET fullName=?, phone=?, birthDate=? WHERE id=?',
      [fullName, phone || null, birthDate || null, req.user.userId]
    );
    res.json({ message: 'Cập nhật thành công' });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

module.exports = router;
