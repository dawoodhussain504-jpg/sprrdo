import { Request, Response } from 'express';
import { calculateRoadRoute } from '../services/routing';

export async function getRoute(req: Request, res: Response) {
  try {
    const { origin_lat, origin_lng, dest_lat, dest_lng } = req.body;

    if (!origin_lat || !origin_lng || !dest_lat || !dest_lng) {
      return res.status(400).json({
        success: false,
        message: 'origin_lat, origin_lng, dest_lat, and dest_lng are required',
      });
    }

    const route = await calculateRoadRoute(
      Number(origin_lat),
      Number(origin_lng),
      Number(dest_lat),
      Number(dest_lng)
    );

    return res.json({
      success: true,
      message: 'Road-snapped route calculated successfully',
      data: route,
    });
  } catch (error: any) {
    return res.status(500).json({
      success: false,
      message: 'Failed to calculate route',
      error: error.message,
    });
  }
}
