import { Request, Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import { emitPopularDestinationsUpdate } from '../services/socket';

export async function getPopularDestinationsPublic(_req: Request, res: Response) {
  try {
    const query = `
      SELECT id, title, subtitle, category, badge, image_url, lat, lng, address as full_address, is_active, sort_order, created_at
      FROM popular_destinations
      WHERE is_active = 1
      ORDER BY sort_order ASC, created_at DESC
    `;
    const result = await db.query(query);
    return res.json({
      success: true,
      data: result.rows.map(row => ({
        ...row,
        is_active: Boolean(row.is_active)
      }))
    });
  } catch (error: any) {
    console.error('Error fetching public popular destinations:', error);
    return res.status(500).json({ success: false, message: 'Failed to fetch popular destinations', error: error.message });
  }
}

export async function getPopularDestinationsAdmin(_req: AuthenticatedRequest, res: Response) {
  try {
    const query = `
      SELECT id, title, subtitle, category, badge, image_url, lat, lng, address as full_address, is_active, sort_order, created_at, updated_at
      FROM popular_destinations
      ORDER BY sort_order ASC, created_at DESC
    `;
    const result = await db.query(query);
    return res.json({
      success: true,
      data: result.rows.map(row => ({
        ...row,
        is_active: Boolean(row.is_active)
      }))
    });
  } catch (error: any) {
    console.error('Error fetching admin popular destinations:', error);
    return res.status(500).json({ success: false, message: 'Failed to fetch destinations for admin', error: error.message });
  }
}

export async function createPopularDestination(req: AuthenticatedRequest, res: Response) {
  try {
    const { title, subtitle, category, badge, image_url, lat, lng, address, full_address, sort_order } = req.body;

    if (!title || !category || lat == null || lng == null) {
      return res.status(400).json({ success: false, message: 'Title, category, latitude and longitude are required' });
    }

    const id = 'dest_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);
    const addr = full_address || address || title;
    const sub = subtitle || category;
    const b = badge || 'Popular';
    const img = image_url || 'https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80';
    const order = Number(sort_order) || 0;

    await db.query(
      `INSERT INTO popular_destinations (id, title, subtitle, category, badge, image_url, lat, lng, address, is_active, sort_order)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 1, $10)`,
      [id, title, sub, category, b, img, Number(lat), Number(lng), addr, order]
    );

    const created = {
      id,
      title,
      subtitle: sub,
      category,
      badge: b,
      image_url: img,
      lat: Number(lat),
      lng: Number(lng),
      full_address: addr,
      is_active: true,
      sort_order: order
    };

    // Broadcast real-time WebSocket update to Rider & Captain apps
    emitPopularDestinationsUpdate({ action: 'create', destination: created });

    return res.status(201).json({
      success: true,
      data: created,
      message: 'Popular destination created successfully'
    });
  } catch (error: any) {
    console.error('Error creating popular destination:', error);
    return res.status(500).json({ success: false, message: 'Failed to create destination', error: error.message });
  }
}

export async function updatePopularDestination(req: AuthenticatedRequest, res: Response) {
  try {
    const { id } = req.params;
    const { title, subtitle, category, badge, image_url, lat, lng, address, full_address, is_active, sort_order } = req.body;

    const existing = await db.query('SELECT * FROM popular_destinations WHERE id = $1', [id]);
    if (existing.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Destination not found' });
    }

    const current = existing.rows[0];
    const newTitle = title !== undefined ? title : current.title;
    const newSubtitle = subtitle !== undefined ? subtitle : current.subtitle;
    const newCategory = category !== undefined ? category : current.category;
    const newBadge = badge !== undefined ? badge : current.badge;
    const newImg = image_url !== undefined ? image_url : current.image_url;
    const newLat = lat !== undefined ? Number(lat) : Number(current.lat);
    const newLng = lng !== undefined ? Number(lng) : Number(current.lng);
    const newAddress = (full_address !== undefined ? full_address : address) !== undefined ? (full_address || address) : current.address;
    const newActive = is_active !== undefined ? (is_active ? 1 : 0) : current.is_active;
    const newOrder = sort_order !== undefined ? Number(sort_order) : current.sort_order;

    await db.query(
      `UPDATE popular_destinations
       SET title = $1, subtitle = $2, category = $3, badge = $4, image_url = $5, lat = $6, lng = $7, address = $8, is_active = $9, sort_order = $10, updated_at = CURRENT_TIMESTAMP
       WHERE id = $11`,
      [newTitle, newSubtitle, newCategory, newBadge, newImg, newLat, newLng, newAddress, newActive, newOrder, id]
    );

    const updated = {
      id,
      title: newTitle,
      subtitle: newSubtitle,
      category: newCategory,
      badge: newBadge,
      image_url: newImg,
      lat: newLat,
      lng: newLng,
      full_address: newAddress,
      is_active: Boolean(newActive),
      sort_order: newOrder
    };

    // Broadcast real-time WebSocket update to Rider & Captain apps
    emitPopularDestinationsUpdate({ action: 'update', destination: updated });

    return res.json({
      success: true,
      data: updated,
      message: 'Popular destination updated successfully'
    });
  } catch (error: any) {
    console.error('Error updating popular destination:', error);
    return res.status(500).json({ success: false, message: 'Failed to update destination', error: error.message });
  }
}

export async function deletePopularDestination(req: AuthenticatedRequest, res: Response) {
  try {
    const { id } = req.params;
    await db.query('DELETE FROM popular_destinations WHERE id = $1', [id]);

    // Broadcast real-time WebSocket update to Rider & Captain apps
    emitPopularDestinationsUpdate({ action: 'delete', id });

    return res.json({
      success: true,
      message: 'Popular destination deleted successfully'
    });
  } catch (error: any) {
    console.error('Error deleting popular destination:', error);
    return res.status(500).json({ success: false, message: 'Failed to delete destination', error: error.message });
  }
}
