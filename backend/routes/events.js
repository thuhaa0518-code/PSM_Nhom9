const express = require('express');
const db      = require('../db');
const auth    = require('../middleware/auth');

const router = express.Router();
router.use(auth); // tất cả routes đều cần token

// GET /api/events?startTime=&endTime=
router.get('/', async (req, res) => {
  try {
    const { startTime, endTime } = req.query;
    let sql = 'SELECT * FROM events WHERE userId = ?';
    const params = [req.user.userId];

    if (startTime && endTime) {
      sql += ' AND startTime >= ? AND startTime <= ?';
      params.push(startTime, endTime);
    }
    sql += ' ORDER BY startTime ASC';

    const [rows] = await db.query(sql, params);
    // Đảm bảo startTime/endTime là number (mysql2 có thể trả về string cho BIGINT)
    const result = rows.map(r => ({
      ...r,
      startTime: Number(r.startTime),
      endTime:   Number(r.endTime)
    }));
    res.json(result);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

// POST /api/events
router.post('/', async (req, res) => {
  try {
    const { title, location, note, startTime, endTime, color,
            priority, isRepeat, repeatDays, reminderMinutes } = req.body;

    const [result] = await db.query(
      `INSERT INTO events
       (userId, title, location, note, startTime, endTime, color,
        priority, isRepeat, repeatDays, reminderMinutes)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [req.user.userId, title, location || '', note || '',
       startTime, endTime, color || '#A855F7',
       priority || 1, isRepeat ? 1 : 0, repeatDays || '',
       reminderMinutes || 0]
    );

    const [rows] = await db.query('SELECT * FROM events WHERE id = ?', [result.insertId]);
    const row = rows[0];
    res.json({ ...row, startTime: Number(row.startTime), endTime: Number(row.endTime) });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

// PUT /api/events/:id
router.put('/:id', async (req, res) => {
  try {
    const { title, location, note, startTime, endTime, color,
            priority, isRepeat, repeatDays, isCompleted, reminderMinutes } = req.body;

    await db.query(
      `UPDATE events SET
       title=?, location=?, note=?, startTime=?, endTime=?, color=?,
       priority=?, isRepeat=?, repeatDays=?, isCompleted=?, reminderMinutes=?
       WHERE id=? AND userId=?`,
      [title, location || '', note || '', startTime, endTime, color,
       priority, isRepeat ? 1 : 0, repeatDays || '',
       isCompleted ? 1 : 0, reminderMinutes || 0,
       req.params.id, req.user.userId]
    );

    const [rows] = await db.query('SELECT * FROM events WHERE id = ?', [req.params.id]);
    const row = rows[0];
    res.json({ ...row, startTime: Number(row.startTime), endTime: Number(row.endTime) });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

// DELETE /api/events/:id
router.delete('/:id', async (req, res) => {
  try {
    await db.query('DELETE FROM events WHERE id=? AND userId=?',
      [req.params.id, req.user.userId]);
    res.json({ message: 'Đã xóa sự kiện' });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

module.exports = router;
