import { Router } from 'express';
import { pool } from '../db/pool.js';
import { requireAuth } from '../middlewares/auth.middleware.js';

export const configRouter = Router();

configRouter.get('/business', async (req, res) => {
  try {
    const result = await pool.query("SELECT config FROM page_config WHERE section = 'business'");
    if (result.rows.length > 0) {
      res.json(result.rows[0].config);
    } else {
      res.status(404).json({ error: 'not_found' });
    }
  } catch (error) {
    res.status(500).json({ error: 'db_error' });
  }
});

configRouter.get('/page', async (req, res) => {
  try {
    const result = await pool.query('SELECT section, config FROM page_config');
    const pageConfig = result.rows.reduce((acc: Record<string, any>, row: any) => {
      acc[row.section] = row.config;
      return acc;
    }, {});
    res.json(pageConfig);
  } catch (error) {
    res.status(500).json({ error: 'db_error' });
  }
});

configRouter.get('/featured-products', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM featured_products WHERE visible = true');
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: 'db_error' });
  }
});
configRouter.post('/page', async (req, res) => {  try {
    const { section, config } = req.body;
    if (!section || !config) {
      res.status(400).json({ error: 'bad_request' });
      return;
    }

    await pool.query(
      `INSERT INTO page_config (section, config) 
       VALUES ($1, $2) 
       ON CONFLICT (section) DO UPDATE SET config = EXCLUDED.config`,
      [section, config]
    );

    res.status(200).json({ success: true });
  } catch (error) {
    res.status(500).json({ error: 'internal_error' });
  }
});
configRouter.get('/category-metadata', async (req, res) => {
  try {
    const result = await pool.query('SELECT category_id, description FROM category_metadata');
    res.json(result.rows);
  } catch (error) {
    console.error('🔥 Error SQL en /category-metadata:', error);
    res.status(500).json({ error: 'db_error', detail: error instanceof Error ? error.message : 'Unknown error' });
  }
});

configRouter.post('/category-metadata', async (req, res) => {
  try {
    const { categoryId, description } = req.body;
    if (!categoryId) {
      res.status(400).json({ error: 'bad_request' });
      return;
    }

    await pool.query(
      `INSERT INTO category_metadata (category_id, description) 
       VALUES ($1, $2) 
       ON CONFLICT (category_id) DO UPDATE SET description = EXCLUDED.description`,
      [categoryId, description || '']
    );

    res.status(200).json({ success: true });
  } catch (error) {
    res.status(500).json({ error: 'internal_error' });
  }
});